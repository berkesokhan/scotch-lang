package scotch.compiler.syntax;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.UnshuffledPattern;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public interface PrecedenceParser {

    void addPattern(Symbol symbol, PatternMatcher matcher);

    default void defineOperator(Symbol symbol, Operator operator) {
        scope().defineOperator(symbol, operator);
    }

    default void defineValue(Symbol symbol, Type type) {
        scope().defineValue(symbol, type);
    }

    void enterScope(Definition definition);

    void error(SyntaxError error);

    Optional<Definition> getDefinition(DefinitionReference reference);

    DefinitionGraph getGraph();

    default boolean isOperator(Symbol symbol) {
        return scope().isOperator(symbol);
    }

    @SuppressWarnings("unchecked")
    <T extends Scoped> T keep(Scoped scoped);

    void leaveScope();

    List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, PrecedenceParser, ? extends Definition> function);

    List<DefinitionReference> mapOptional(List<DefinitionReference> references, BiFunction<? super Definition, PrecedenceParser, Optional<? extends Definition>> function);

    void parsePrecedence();

    List<DefinitionReference> processPatterns();

    Optional<Symbol> qualify(Symbol symbol);

    Symbol reserveSymbol();

    Scope scope();

    <T extends Definition> T scoped(T definition, Supplier<? extends T> supplier);

    <T extends Scoped> T scoped(Scoped value, Supplier<? extends T> supplier);

    <T extends Definition> Optional<Definition> scopedOptional(T definition, Supplier<Optional<? extends T>> supplier);

    Optional<Definition> shuffle(UnshuffledPattern pattern);

    Value shuffle(UnshuffledValue value);

    void symbolNotFound(Symbol symbol, SourceRange sourceRange);
}
