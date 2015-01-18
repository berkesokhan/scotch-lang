package scotch.compiler.syntax;

import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;

public interface Scoped {

    default Optional<Symbol> asSymbol() {
        return Optional.empty();
    }

    Definition getDefinition();

    DefinitionReference getReference();
}
