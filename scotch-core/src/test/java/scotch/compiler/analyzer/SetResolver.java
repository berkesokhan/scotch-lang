package scotch.compiler.analyzer;

import static scotch.compiler.util.TextUtil.normalizeQualified;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SetResolver implements SymbolResolver {

    private final Set<String> names = new HashSet<>();

    public SetResolver addName(String name) {
        names.add(name);
        return this;
    }

    @Override
    public Optional<String> qualify(String moduleName, String name) {
        String qualifiedName = normalizeQualified(moduleName, name);
        if (names.contains(qualifiedName)) {
            return Optional.of(qualifiedName);
        } else {
            return Optional.empty();
        }
    }
}
