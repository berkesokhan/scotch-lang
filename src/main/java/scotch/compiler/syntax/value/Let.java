package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class Let extends Value implements Scoped {

    private final SourceRange               sourceRange;
    private final Symbol                    symbol;
    private final List<DefinitionReference> definitions;
    private final Value                     body;

    Let(SourceRange sourceRange, Symbol symbol, List<DefinitionReference> definitions, Value body) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.definitions = ImmutableList.copyOf(definitions);
        this.body = body;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return state.keep(withDefinitions(state.accumulateDependencies(definitions))
        .withBody(body.accumulateDependencies(state)));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateNames(definitions))
            .withBody(body.accumulateNames(state)));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return state.scoped(this, () -> withBody(body.bindMethods(state))
            .withDefinitions(state.map(definitions, (definition, s) -> definition.asValue()
                .map(value -> state.scoped(definition, () -> value.withBody(body.bindMethods(state))))
                .orElseGet(state::keep))));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withDefinitions(state.map(definitions, Definition::bindTypes))
            .withBody(body.bindTypes(state));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return state.enclose(this, () -> withDefinitions(state.checkTypes(definitions))
            .withBody(body.checkTypes(state)));
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::defineOperators))
            .withBody(body.defineOperators(state)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Let) {
            Let other = (Let) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(definitions, other.definitions)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    public Value getBody() {
        return body;
    }

    @Override
    public Definition getDefinition() {
        return scopeDef(this);
    }

    public List<DefinitionReference> getDefinitions() {
        return definitions;
    }

    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Type getType() {
        return body.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, definitions, body);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return state.scoped(this, () -> withDefinitions(state.mapOptional(definitions, Definition::parsePrecedence))
            .withBody(body.parsePrecedence(state)));
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::qualifyNames))
            .withBody(body.qualifyNames(state)));
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    public Let withBody(Value body) {
        return new Let(sourceRange, symbol, definitions, body);
    }

    public Let withDefinitions(List<DefinitionReference> definitions) {
        return new Let(sourceRange, symbol, definitions, body);
    }

    @Override
    public Value withType(Type type) {
        return new Let(sourceRange, symbol, definitions, body.withType(type));
    }
}
