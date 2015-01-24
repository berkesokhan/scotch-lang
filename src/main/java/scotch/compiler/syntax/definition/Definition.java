package scotch.compiler.syntax.definition;

import static scotch.data.either.Either.left;

import java.util.List;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;

public abstract class Definition implements Scoped {

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

    public static ValueSignature signature(SourceRange sourceRange, Symbol symbol, Type type) {
        return new ValueSignature(sourceRange, symbol, type);
    }

    public static UnshuffledDefinition unshuffled(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledDefinition(sourceRange, symbol, matches, body);
    }

    public static ValueDefinition value(SourceRange sourceRange, Symbol symbol, Type type, Value value) {
        return new ValueDefinition(sourceRange, symbol, value, type);
    }

    Definition() {
        // intentionally empty
    }

    public abstract Definition accumulateDependencies(DependencyAccumulator state);

    public abstract Definition accumulateNames(NameAccumulator state);

    public Either<Definition, ValueSignature> asSignature() {
        return left(this);
    }

    public Optional<Symbol> asSymbol() {
        return Optional.empty();
    }

    public Either<Definition, ValueDefinition> asValue() {
        return left(this);
    }

    public abstract Definition bindTypes(TypeChecker state);

    public abstract Definition checkTypes(TypeChecker state);

    public abstract Definition defineOperators(OperatorDefinitionParser state);

    @Override
    public abstract boolean equals(Object o);

    public abstract void generateBytecode(BytecodeGenerator state);

    @Override
    public Definition getDefinition() {
        return this;
    }

    public abstract SourceRange getSourceRange();

    @Override
    public abstract int hashCode();

    public void markLine(CodeBlock codeBlock) {
        getSourceRange().markLine(codeBlock);
    }

    public abstract Optional<Definition> parsePrecedence(PrecedenceParser state);

    public abstract Definition qualifyNames(NameQualifier state);

    @Override
    public abstract String toString();
}
