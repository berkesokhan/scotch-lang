package scotch.compiler.util;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import scotch.data.tuple.Tuple2;

public final class TextUtil {

    private static final String  moduleSubPattern       = "[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*";
    private static final String  memberSubPattern       = "(\\[\\]|\\((?:[^\\)]+)\\)|(?:[^\\.]+)|\\(,*\\))";
    private static final Pattern qualifiedPattern       = compile("^(\\$?" + moduleSubPattern + ")\\." + memberSubPattern + "$");
    private static final Pattern containsSymbolsPattern = compile("\\W");
    private static final Pattern tuplePattern           = compile("\\(,*\\)");

    public static boolean isAsciiEscape(int c) {
        return c == '\\' || c == 'b' || c == 't' || c == 'n' || c == 'f' || c == 'r' || c == '"' || c == '\'';
    }

    public static boolean isBackslash(int c) {
        return c == '\\';
    }

    public static boolean isBacktick(int c) {
        return c == '`';
    }

    public static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isDot(int c) {
        return c == '.';
    }

    public static boolean isDoubleQuote(int c) {
        return c == '"';
    }

    public static boolean isHex(int c) {
        return c >= '0' && c <= '9'
            || c >= 'a' && c <= 'f'
            || c >= 'A' && c <= 'F';
    }

    public static boolean isHorizontalWhitespace(int c) {
        return c == ' ' || c == '\t';
    }

    public static boolean isIdentifier(int c) {
        return isLetter(c) || isIdentifierDigit(c) || isSymbol(c);
    }

    public static boolean isIdentifierDigit(int c) {
        return isDigit(c)
            || c >= '\u0660' && c <= '\u0669'
            || c >= '\u06F0' && c <= '\u06F9'
            || c >= '\u0966' && c <= '\u096F'
            || c >= '\u09E6' && c <= '\u09EF'
            || c >= '\u0A66' && c <= '\u0A6F'
            || c >= '\u0AE6' && c <= '\u0AEF'
            || c >= '\u0B66' && c <= '\u0B6F'
            || c >= '\u0BE7' && c <= '\u0BEF'
            || c >= '\u0C66' && c <= '\u0C6F'
            || c >= '\u0CE6' && c <= '\u0CEF'
            || c >= '\u0D66' && c <= '\u0D6F'
            || c >= '\u0E50' && c <= '\u0E59'
            || c >= '\u0ED0' && c <= '\u0ED9'
            || c >= '\u1040' && c <= '\u1049';
    }

    public static boolean isLetter(int c) {
        return c >= 'A' && c <= 'Z'
            || c >= 'a' && c <= 'z'
            || c == '_'
            || c >= '\u00C0' && c <= '\u00D6'
            || c >= '\u00D8' && c <= '\u00F6'
            || c >= '\u00F8' && c <= '\u1FFF'
            || c >= '\u2200' && c <= '\u22FF'
            || c >= '\u27C0' && c <= '\u27EF'
            || c >= '\u2980' && c <= '\u2AFF'
            || c >= '\u3040' && c <= '\u318F'
            || c >= '\u3300' && c <= '\u337F'
            || c >= '\u3400' && c <= '\u3D2D'
            || c >= '\u4E00' && c <= '\u9FFF'
            || c >= '\uF900' && c <= '\uFAFF';
    }

    public static boolean isNewLineOrEOF(int c) {
        return c == '\n' || c == '\r' || c == -1;
    }

    public static boolean isOctal(int c) {
        return c >= '0' && c <= '7';
    }

    public static boolean isSingleQuote(int c) {
        return c == '\'';
    }

    public static boolean isSymbol(int c) {
        return c == '~'
            || c == '|'
            || c == '!'
            || c == '$'
            || c == '%'
            || c == '^'
            || c == '&'
            || c == '*'
            || c == '-'
            || c == '='
            || c == '+'
            || c == '/'
            || c == '?'
            || c == '<'
            || c == '>';
    }

    public static String normalizeQualified(String qualifiedName) {
        return splitQualified(qualifiedName).into(
            (optionalModuleName, memberName) -> optionalModuleName
                .map(moduleName -> normalizeQualified(moduleName, memberName))
                .orElse(memberName)
        );
    }

    public static String normalizeQualified(String moduleName, String memberName) {
        if (!"[]".equals(memberName) && !memberName.contains("(") && containsSymbolsPattern.matcher(memberName).find()) {
            return moduleName + ".(" + memberName + ")";
        } else {
            return moduleName + '.' + memberName;
        }
    }

    public static String normalizeQualified(Optional<String> optionalModuleName, String memberName) {
        return optionalModuleName
            .map(moduleName -> normalizeQualified(moduleName, memberName))
            .orElse(memberName);
    }

    public static String quote(Object o) {
        if (o instanceof String) {
            return "'" + escapeJava((String) o).replace("'", "\\'") + "'";
        } else if (o instanceof Character) {
            return quote("" + (char) o);
        } else if (o == null) {
            return quote("null");
        } else {
            return o.toString();
        }
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

    public static String stringify(Object o) {
        return format("%s@%08X", o.getClass().getSimpleName(), o.hashCode());
    }

    private TextUtil() {
        // intentionally empty
    }
}
