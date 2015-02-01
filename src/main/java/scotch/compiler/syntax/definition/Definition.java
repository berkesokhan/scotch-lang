package scotch.compiler.syntax.definition;

import static scotch.compiler.util.Either.left;

import java.util.List;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Either;

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

    public abstract Definition defineOperators(OperatorAccumulator state);

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
