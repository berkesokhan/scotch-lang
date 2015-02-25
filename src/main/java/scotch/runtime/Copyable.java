package scotch.runtime;

import java.util.Map;

public interface Copyable {

    Copyable copy(Map<String, Callable> properties);
}
