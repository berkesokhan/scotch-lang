package scotch.util;

import static java.lang.String.format;
import static org.apache.commons.lang.StringEscapeUtils.escapeJava;

public class StringUtil {

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

    public static String stringify(Object o) {
        return format("%s@%08X", o.getClass().getSimpleName(), o.hashCode());
    }
}
