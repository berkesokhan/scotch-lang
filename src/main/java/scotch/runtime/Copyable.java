package scotch.runtime;

import java.util.Map;

/**
 * Support for copying objects.
 */
public interface Copyable {

    /**
     * Copies this object with the given property bag.
     *
     * @param properties The properties to overwrite.
     * @return The new object with the changed properties.
     */
    Copyable copy(Map<String, Callable> properties);
}
