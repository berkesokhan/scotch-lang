package scotch.compiler.syntax;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.UnshuffledPattern;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;

public interface SyntaxTreeParser extends NameQualifier, TypeScope {

    default List<DefinitionReference> accumulateDependencies(List<DefinitionReference> references) {
        return map(references, Definition::accumulateNames);
    }

    default List<DefinitionReference> accumulateNames(List<DefinitionReference> references) {
        return map(references, Definition::accumulateNames);
    }

    Identifier addDependency(Identifier identifier);

    void addPattern(Symbol symbol, PatternMatcher matcher);

    @Override
    default Unification bind(VariableType variableType, Type targetType) {
        return scope().bind(variableType, targetType);
    }

    Definition collect(Definition definition);

    Definition collect(PatternMatcher pattern);

    default void defineOperator(Symbol symbol, Operator operator) {
        scope().defineOperator(symbol, operator);
    }

    default void defineSignature(Symbol symbol, Type type) {
        scope().defineSignature(symbol, type);
    }

    default void defineValue(Symbol symbol, Type type) {
        scope().defineValue(symbol, type);
    }

    void enterScope(Definition definition);

    void error(SyntaxError error);

    @Override
    default void extendContext(Type type, Set<Symbol> additionalContext) {
        scope().extendContext(type, additionalContext);
    }

    void fromRoot(BiFunction<Definition, SyntaxTreeParser, ? extends Definition> function);

    @Override
    default void generalize(Type type) {
        scope().generalize(type);
    }

    @Override
    default Type generate(Type type) {
        return scope().generate(type);
    }

    @Override
    default Type genericCopy(Type type) {
        return scope().genericCopy(type);
    }

    @Override
    default Set<Symbol> getContext(Type type) {
        return scope().getContext(type);
    }

    Optional<Definition> getDefinition(DefinitionReference reference);

    DefinitionGraph getGraph();

    @Override
    default Type getTarget(Type type) {
        return scope().getTarget(type);
    }

    @Override
    default boolean isBound(VariableType variableType) {
        return scope().isBound(variableType);
    }

    default boolean isOperator(Symbol symbol) {
        return scope().isOperator(symbol);
    }

    @SuppressWarnings("unchecked")
    <T extends Scoped> T keep(Scoped scoped);

    void leaveScope();

    List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, SyntaxTreeParser, ? extends Definition> function);

    List<DefinitionReference> mapOptional(List<DefinitionReference> references, BiFunction<? super Definition, SyntaxTreeParser, Optional<? extends Definition>> function);

    void popSymbol();

    List<DefinitionReference> processPatterns();

    void pushSymbol(Symbol symbol);

    Scope scope();

    <T extends Definition> T scoped(T definition, Supplier<? extends T> supplier);

    <T extends Scoped> T scoped(Scoped value, Supplier<? extends T> supplier);

    <T extends Definition> Optional<Definition> scopedOptional(T definition, Supplier<Optional<? extends T>> supplier);

    Optional<Definition> shuffle(UnshuffledPattern pattern);

    Value shuffle(UnshuffledValue value);

    @Override
    default void specialize(Type type) {
        scope().specialize(type);
    }
}
