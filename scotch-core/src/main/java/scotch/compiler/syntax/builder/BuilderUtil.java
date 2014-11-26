package scotch.compiler.syntax.builder;

import java.util.Optional;
import java.util.OptionalInt;

public final class BuilderUtil {

    public static <T> T require(Optional<T> optional, String name) {
        return optional.orElseThrow(() -> required(name));
    }

    public static int require(OptionalInt optional, String name) {
        return optional.orElseThrow(() -> required(name));
    }

    private static IllegalArgumentException required(String name) {
        if (name.endsWith("s")) {
            return new IllegalArgumentException(name + " are required");
        } else {
            return new IllegalArgumentException(name + " is required");
        }
    }

    private BuilderUtil() {
        // intentionally empty
    }
}
