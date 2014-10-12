package scotch.compiler.analyzer;

import java.util.Optional;

public interface SymbolResolver {

    Optional<String> qualify(String moduleName, String name);
}
