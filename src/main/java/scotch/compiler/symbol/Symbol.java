package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.util.Pair;
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
        .put('#', "$")
        .build();

    public static Symbol symbol(String name) {
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

    public static String moduleClass(String symbol) {
        return getPackagePath(symbol) + "/$$Module";
    }

    public static String normalizeQualified(String moduleName, String memberName) {
        if (memberName.matches("^\\d$")) {
            return moduleName + ".(#" + memberName + ")";
        } else if ("[]".equals(memberName) || tuplePattern.matcher(memberName).find() || !containsSymbolsPattern.matcher(memberName).find()) {
            return moduleName + '.' + memberName;
        } else {
            return moduleName + ".(" + memberName + ")";
        }
    }

    public static String normalizeQualified(String moduleName, List<String> memberNames) {
        if (memberNames.size() == 1) {
            return normalizeQualified(moduleName, memberNames.get(0));
        } else {
            return moduleName + ".(" + join(memberNames) + ")";
        }
    }

    public static Symbol qualified(String moduleName, String memberName) {
        return qualified(moduleName, asList(memberName.split("#")));
    }

    public static Symbol qualified(String moduleName, List<String> memberNames) {
        return new QualifiedSymbol(moduleName, memberNames);
    }

    public static Pair<Optional<String>, String> splitQualified(String name) {
        Matcher matcher = qualifiedPattern.matcher(name);
        if (matcher.matches()) {
            if (tuplePattern.matcher(matcher.group(2)).matches()) {
                return pair(Optional.of(matcher.group(1)), matcher.group(2));
            } else {
                return pair(Optional.of(matcher.group(1)), matcher.group(2).replaceAll("[\\(\\)]", ""));
            }
        } else {
            return pair(Optional.empty(), name);
        }
    }

    public static String toJavaName(String memberName) {
        return memberName.chars()
            .mapToObj(i -> javaSymbolMap.getOrDefault(i, String.valueOf(Character.toChars(i))))
            .collect(joining());
    }

    public static Symbol unqualified(String memberName) {
        return unqualified(asList(memberName.split("#")));
    }

    public static Symbol unqualified(List<String> memberNames) {
        return new UnqualifiedSymbol(memberNames);
    }

    private static Integer compareMemberNames(Symbol left, Symbol right) {
        int result = 0;
        Iterator<String> thisIterator = left.getMemberNames().iterator();
        Iterator<String> thatIterator = right.getMemberNames().iterator();
        while (thisIterator.hasNext()) {
            if (!thatIterator.hasNext()) {
                return 1;
            }
            result = thisIterator.next().compareTo(thatIterator.next());
            if (result != 0) {
                return result;
            }
        }
        return result;
    }

    private static String getPackageFor(String moduleName, String delimiter) {
        return Arrays.stream(moduleName.split("\\."))
            .map(section -> javaWords.contains(section) ? section + "_" : section)
            .collect(joining(delimiter));
    }

    private static String join(List<String> memberNames) {
        if (memberNames.get(0).matches("^\\d.*$")) {
            return "#" + join_(memberNames);
        } else {
            return join_(memberNames);
        }
    }

    private static String join_(List<String> memberNames) {
        return memberNames.stream().collect(joining("#"));
    }

    private static List<String> normalize(List<String> memberNames) {
        return ImmutableList.copyOf(memberNames.stream()
            .filter(name -> !name.isEmpty())
            .map(name -> name.startsWith("#") ? name.substring(1) : name)
            .collect(toList()));
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

    public String getClassNameAsChildOf(Symbol dataType) {
        return dataType.getClassName() + "$" + getSimpleName();
    }

    public String getMemberName() {
        String memberName = getMemberNames().stream().collect(joining("#"));
        if (memberName.matches("^\\d+.*")) {
            return "#" + memberName;
        } else {
            return memberName;
        }
    }

    public abstract List<String> getMemberNames();

    public String getMethodName() {
        return toJavaName(getSimpleName());
    }

    public abstract String getModuleClass();

    public String getSimpleName() {
        String simpleName = getMemberNames().get(getMemberNames().size() - 1);
        if (simpleName.matches("^\\d+.*")) {
            return "#" + simpleName;
        } else {
            return simpleName;
        }
    }

    @Override
    public abstract int hashCode();

    public abstract Symbol map(Function<QualifiedSymbol, Symbol> function);

    public Symbol nest(List<String> parentNames) {
        List<String> memberNames = new ArrayList<>();
        memberNames.addAll(parentNames);
        memberNames.add(getSimpleName());
        return withMemberNames(memberNames);
    }

    public abstract Symbol qualifyWith(String moduleName);

    public String quote() {
        return StringUtil.quote(getCanonicalName());
    }

    @Override
    public String toString() {
        return getCanonicalName();
    }

    public abstract Symbol unqualify();

    protected abstract Symbol withMemberNames(List<String> memberNames);

    public interface SymbolVisitor<T> {

        T visit(QualifiedSymbol symbol);

        T visit(UnqualifiedSymbol symbol);
    }

    public static class QualifiedSymbol extends Symbol {

        private final String moduleName;
        private final List<String> memberNames;

        private QualifiedSymbol(String moduleName, List<String> memberNames) {
            this.moduleName = moduleName;
            this.memberNames = normalize(memberNames);
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
                    } else {
                        return compareMemberNames(QualifiedSymbol.this, other);
                    }
                }

                @Override
                public Integer visit(UnqualifiedSymbol symbol) {
                    return 1;
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
                    && Objects.equals(memberNames, other.memberNames);
            } else {
                return false;
            }
        }

        @Override
        public String getCanonicalName() {
            return normalizeQualified(moduleName, memberNames);
        }

        @Override
        public String getClassName() {
            return Optional.ofNullable(javaTypeMap.get(this))
                .orElseGet(() -> getPackagePath() + "/" + toJavaName(getMemberName()));
        }

        @Override
        public List<String> getMemberNames() {
            return memberNames;
        }

        @Override
        public String getModuleClass() {
            return Optional.ofNullable(javaTypeMap.get(this))
                .orElseGet(() -> getPackagePath() + "/$$Module");
        }

        public String getModuleName() {
            return moduleName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, memberNames);
        }

        @Override
        public Symbol map(Function<QualifiedSymbol, Symbol> function) {
            return function.apply(this);
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberNames);
        }

        @Override
        public Symbol unqualify() {
            return unqualified(memberNames);
        }

        private String getPackagePath() {
            return getPackagePath(moduleName);
        }

        @Override
        protected Symbol withMemberNames(List<String> memberNames) {
            return new QualifiedSymbol(moduleName, memberNames);
        }
    }

    public static class UnqualifiedSymbol extends Symbol {

        private final List<String> memberNames;

        public UnqualifiedSymbol(List<String> memberNames) {
            this.memberNames = normalize(memberNames);
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
                    return -1;
                }

                @Override
                public Integer visit(UnqualifiedSymbol symbol) {
                    return compareMemberNames(UnqualifiedSymbol.this, symbol);
                }
            });
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof UnqualifiedSymbol && Objects.equals(memberNames, ((UnqualifiedSymbol) o).memberNames);
        }

        @Override
        public String getCanonicalName() {
            return join(memberNames);
        }

        @Override
        public String getClassName() {
            throw new IllegalStateException("Can't get unqualified class name from symbol " + quote());
        }

        @Override
        public List<String> getMemberNames() {
            return memberNames;
        }

        @Override
        public String getModuleClass() {
            throw new IllegalStateException();
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberNames);
        }

        @Override
        public Symbol map(Function<QualifiedSymbol, Symbol> function) {
            return this;
        }

        @Override
        public Symbol qualifyWith(String moduleName) {
            return qualified(moduleName, memberNames);
        }

        @Override
        public Symbol unqualify() {
            return this;
        }

        @Override
        protected Symbol withMemberNames(List<String> memberNames) {
            return new UnqualifiedSymbol(memberNames);
        }
    }
}
