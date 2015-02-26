package scotch.compiler.util;

import java.io.File;
import java.net.URI;

public class CompilerUtil {

    public static File toFile(URI uri) {
        return new File(uri.getPath());
    }
}
