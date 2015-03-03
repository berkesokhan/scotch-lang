package scotch.compiler.syntax.definition;

import java.util.List;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class Definitions {

    public static ClassDefinition classDef(SourceRange sourceRange, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        return new ClassDefinition(sourceRange, symbol, arguments, members);
    }

    public static ModuleDefinition module(SourceRange sourceRange, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceRange, symbol, imports, definitions);
    }

    public static OperatorDefinition operatorDef(SourceRange sourceRange, Symbol symbol, Fixity fixity, int precedence) {
        return new OperatorDefinition(sourceRange, symbol, fixity, precedence);
    }

    public static RootDefinition root(SourceRange sourceRange, List<DefinitionReference> definitions) {
        return new RootDefinition(sourceRange, definitions);
    }

    public static ScopeDefinition scopeDef(SourceRange sourceRange, Symbol symbol) {
        return new ScopeDefinition(sourceRange, symbol);
    }

    public static Definition scopeDef(Scoped scoped) {
        return scoped.getDefinition();
    }

    public static ValueSignature signature(SourceRange sourceRange, Symbol symbol, Type type) {
        return new ValueSignature(sourceRange, symbol, type);
    }

    public static UnshuffledDefinition unshuffled(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledDefinition(sourceRange, symbol, matches, body);
    }

    public static ValueDefinition value(SourceRange sourceRange, Symbol symbol, Type type, Value value) {
        return new ValueDefinition(sourceRange, symbol, value, type);
    }

    private Definitions() {
        // intentionally empty
    }
}
