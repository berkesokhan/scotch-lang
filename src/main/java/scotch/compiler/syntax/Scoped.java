package scotch.compiler.syntax;

import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;

public interface Scoped {

    Definition getDefinition();

    DefinitionReference getReference();
}
