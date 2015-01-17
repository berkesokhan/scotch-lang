package scotch.compiler.syntax.definition;

import java.util.List;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

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

    public static UnshuffledPattern unshuffled(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledPattern(sourceRange, symbol, matches, body);
    }

    public static ValueDefinition value(SourceRange sourceRange, Symbol symbol, Type type, Value value) {
        return new ValueDefinition(sourceRange, symbol, value, type);
    }

    Definition() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionVisitor<T> visitor);

    public abstract Definition accumulateDependencies(SyntaxTreeParser state);

    public abstract Definition accumulateNames(SyntaxTreeParser state);

    public abstract Definition bindTypes(TypeChecker state);

    public abstract Definition checkTypes(TypeChecker state);

    public abstract Definition defineOperators(SyntaxTreeParser state);

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

    public abstract Optional<Definition> parsePrecedence(SyntaxTreeParser state);

    public abstract Definition qualifyNames(SyntaxTreeParser state);

    @Override
    public abstract String toString();

    public interface DefinitionVisitor<T> {

        default T visit(ClassDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(ModuleDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(OperatorDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(RootDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(ScopeDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(UnshuffledPattern pattern) {
            return visitOtherwise(pattern);
        }

        default T visit(ValueDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(ValueSignature signature) {
            return visitOtherwise(signature);
        }

        default T visitOtherwise(Definition definition) {
            throw new UnsupportedOperationException("Can't visit " + definition.getClass().getSimpleName());
        }
    }
}
