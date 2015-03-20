package scotch.compiler.syntax.definition;

import java.util.List;
import scotch.symbol.Symbol;
import scotch.symbol.Value.Fixity;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.pattern.PatternMatch;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

public class Definitions {

    public static ClassDefinition classDef(SourceLocation sourceLocation, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        return new ClassDefinition(sourceLocation, symbol, arguments, members);
    }

    public static ModuleDefinition module(SourceLocation sourceLocation, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceLocation, symbol, imports, definitions);
    }

    public static OperatorDefinition operatorDef(SourceLocation sourceLocation, Symbol symbol, Fixity fixity, int precedence) {
        return new OperatorDefinition(sourceLocation, symbol, fixity, precedence);
    }

    public static RootDefinition root(SourceLocation sourceLocation, List<DefinitionReference> definitions) {
        return new RootDefinition(sourceLocation, definitions);
    }

    public static ScopeDefinition scopeDef(SourceLocation sourceLocation, Symbol symbol) {
        return new ScopeDefinition(sourceLocation, symbol);
    }

    public static Definition scopeDef(Scoped scoped) {
        return scoped.getDefinition();
    }

    public static ValueSignature signature(SourceLocation sourceLocation, Symbol symbol, Type type) {
        return new ValueSignature(sourceLocation, symbol, type);
    }

    public static UnshuffledDefinition unshuffled(SourceLocation sourceLocation, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledDefinition(sourceLocation, symbol, matches, body);
    }

    public static ValueDefinition value(SourceLocation sourceLocation, Symbol symbol, Value value) {
        return new ValueDefinition(sourceLocation, symbol, value);
    }

    private Definitions() {
        // intentionally empty
    }
}
