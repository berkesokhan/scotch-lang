package scotch.compiler.symbol;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.data.tuple.Tuple2;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.util.StringUtil;

public abstract class Symbol implements Comparable<Symbol> {

    private static final Pattern                tuplePattern           = compile("\\(,*\\)");
    private static final String                 moduleSubPattern       = "[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*";
    private static final String                 memberSubPattern       = "(\\[\\]|\\((?:[^\\)]+)\\)|(?:[^\\.]+)|\\(,*\\))";
    private static final Pattern                qualifiedPattern       = compile("^(\\$?" + moduleSubPattern + ")\\." + memberSubPattern + "$");
    private static final Pattern                containsSymbolsPattern = compile("\\W");
    private static final Set<String>            javaWords              = ImmutableSet.of(
        "abstract", "assert",
        "boolean", "break", "byte",
        "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double",
        "else", "enum", "extends",
        "false", "final", "finally", "float", "for",
        "goto",
        "if", "implements", "import", "instanceof", "int", "interface",
        "long",
        "native", "new", "null",
        "package", "private", "protected", "public",
        "return",
        "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "true", "try",
        "void", "volatile",
        "while"
    );
    private static final Map<Symbol, String>    javaTypeMap            = ImmutableMap.<Symbol, String>builder()
        .put(qualified("scotch.data.int", "Int"), p(Integer.class))
        .put(qualified("scotch.data.string", "String"), p(String.class))
        .put(qualified("scotch.data.char.Char", "Char"), p(Character.class))
        .put(qualified("scotch.data.bool", "Bool"), p(Boolean.class))
        .put(qualified("scotch.data.double", "Double"), p(Double.class))
        .build();
    private static final Map<Character, String> javaSymbolMap          = ImmutableMap.<Character, String>builder()
        .put('~', "$twiddle")
        .put('|', "$or")
        .put('!', "$bang")
        .put('$', "$bux")
        .put('%', "$chunk")
        .put('^', "$point")
        .put('&', "$ditto")
        .put('*', "$splat")
        .put('-', "$down")
        .put('=', "$same")
        .put('+', "$up")
        .put('/', "$split")
        .put('?', "$wat")
        .put('<', "$left")
        .put('>', "$right")
        .put('.', "$dot")
        .put(':', "$doot")
        .put('#', "$chirp")
        .build();

    public static Symbol fromString(String name) {
        return splitQualified(name).into(
            (optionalModuleName, memberName) -> optionalModuleName
                .map(moduleName -> qualified(moduleName, memberName))
                .orElseGet(() -> unqualified(memberName))
        );
    }

    public static String getPackageName(String moduleName) {
        return getPackageFor(moduleName, ".");
    }

    public static String getPackagePath(String moduleName) {
        return getPackageFor(moduleName, "/");
    }

    public static String normalizeQualified(String moduleName, String memberName) {
        if (!"[]".equals(memberName) && !memberName.contains("(") && containsSymbolsPattern.matcher(memberName).find()) {
            return moduleName + ".(" + memberName + ")";
        } else {
            return moduleName + '.' + memberName;
        }
    }

    public static Symbol qualified(String moduleName, String memberName) {
        return new QualifiedSymbol(moduleName, memberName);
    }

    public static Tuple2<Optional<String>, String> splitQualified(String name) {
        Matcher matcher = qualifiedPattern.matcher(name);
        if (matcher.matches()) {
            if (tuplePattern.matcher(matcher.group(2)).matches()) {
                return tuple2(Optional.of(matcher.group(1)), matcher.group(2));
            } else {
                return tuple2(Optional.of(matcher.group(1)), matcher.group(2).replaceAll("[\\(\\)]", ""));
            }
        } else {
            return tuple2(Optional.empty(), name);
        }
    }

    public static Symbol unqualified(String memberName) {
        return new UnqualifiedSymbol(memberName);
    }

    private static String getPackageFor(String moduleName, String delimiter) {
        return Arrays.stream(moduleName.split("\\."))
            .map(section -> javaWords.contains(section) ? section + "_" : section)
            .collect(joining(delimiter));
    }

    private Symbol() {
        // intentionally empty
    }

    public abstract <T> T accept(SymbolVisitor<T> visitor);

    @Override
    public abstract int compareTo(Symbol other);

    @Override
    public abstract boolean equals(Object o);

    public abstract String getCanonicalName();

    public abstract String getClassName();

    public abstract String getMemberName();

    public String getMethodName() {
        return toJavaName_(getMemberName());
    }

    @Override
    public abstract int hashCode();

    public abstract Symbol qualifyWith(String moduleName);

    public String quote() {
        return StringUtil.quote(getCanonicalName());
    }

    @Override
    public String toString() {
        return getCanonicalName();
    }

    public abstract Symbol unqualify();

    protected String toJavaName_(String memberName) {
        return memberName.chars()
            .mapToObj(i -> javaSymbolMap.getOrDefault(i, String.valueOf(Character.toChars(i))))
            .collect(joining());
    }

    public interface SymbolVisitor<T> {

        T visit(QualifiedSymbol symbol);

        T visit(UnqualifiedSymbol symbol);
    }

    public static class QualifiedSymbol extends Symbol {

        private final String moduleName;
        private final String memberName;

        public QualifiedSymbol(String moduleName, String memberName) {
            this.moduleName = moduleName;
            this.memberName = memberName;
        }

        @Override
        public <T> T accept(SymbolVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int compareTo(Symbol other) {
            return other.accept(new SymbolVisitor<Integer>() {
                @Override
                public Integer visit(QualifiedSymbol symbol) {
                    int result = moduleName.compareTo(symbol.moduleName);
                    if (result != 0) {
                        return result;
                    }
                    return memberName.compareTo(symbol.memberName);
                }

                @Override
                public Integer visit(UnqualifiedSymbol symbol) {
                    throw new IllegalArgumentException();
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof QualifiedSymbol) {
                QualifiedSymbol other = (QualifiedSymbol) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(memberName, other.memberName);
            } else {
                return false;
            }
        }

        @Override
        public String getCanonicalName() {
            return normalizeQualified(moduleName, memberName);
        }

        @Override
        public String getClassName() {
            return Optional.ofNullable(javaTypeMap.get(this))
                .orElseGet(() -> getPackagePath() + "/" + toJavaName_(memberName));
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        public MethodSignature getMethodSignature(Type type) {
            return MethodSignature.staticFromSymbol(
                this,
                ImmutableList.of(),
                ClassSignature.fromClass(type instanceof FunctionType ? Applicable.class : Callable.class)
            );
        }

        public String getModuleName() {
            return moduleName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, memberName);
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberName);
        }

        @Override
        public Symbol unqualify() {
            return unqualified(memberName);
        }

        private String getPackagePath() {
            return getPackagePath(moduleName);
        }
    }

    public static class UnqualifiedSymbol extends Symbol {

        private final String memberName;

        public UnqualifiedSymbol(String memberName) {
            this.memberName = memberName;
        }

        @Override
        public <T> T accept(SymbolVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public int compareTo(Symbol other) {
            return other.accept(new SymbolVisitor<Integer>() {
                @Override
                public Integer visit(QualifiedSymbol symbol) {
                    throw new IllegalArgumentException();
                }

                @Override
                public Integer visit(UnqualifiedSymbol symbol) {
                    return memberName.compareTo(symbol.memberName);
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof UnqualifiedSymbol && Objects.equals(memberName, ((UnqualifiedSymbol) o).memberName);
        }

        @Override
        public String getCanonicalName() {
            return memberName;
        }

        @Override
        public String getClassName() {
            throw new IllegalStateException("Can't get unqualified class name from symbol " + quote());
        }

        @Override
        public String getMemberName() {
            return memberName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberName);
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberName);
        }

        @Override
        public Symbol unqualify() {
            return this;
        }
    }
}
