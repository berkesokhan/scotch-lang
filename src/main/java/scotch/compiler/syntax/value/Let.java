package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class Let extends Value implements Scoped {

    public static Builder builder() {
        return new Builder();
    }
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
        return state.scoped(this, () -> withDefinitions(state.accumulateDependencies(definitions))
            .withBody(body.accumulateDependencies(state)));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateNames(definitions))
            .withBody(body.accumulateNames(state)));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return state.scoped(this, () -> withBody(body.bindMethods(state)));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return state.scoped(this, () -> withBody(body.bindTypes(state)));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return state.enclose(this, () -> withBody(body.checkTypes(state)));
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
        return state.scoped(this, () -> withDefinitions(state.defineDefinitionOperators(definitions))
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
        state.generateBytecode(definitions);
        return state.scoped(this, () -> body.generateBytecode(state));
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
        return state.scoped(this, () -> withBody(body.parsePrecedence(state))
            .withDefinitions(new ArrayList<DefinitionReference>() {{
                addAll(state.mapOptional(definitions, Definition::parsePrecedence));
                addAll(state.processPatterns());
            }}));
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions))
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

    public static class Builder implements SyntaxBuilder<Let> {

        private Optional<SourceRange>               sourceRange;
        private Optional<Symbol>                    symbol;
        private Optional<List<DefinitionReference>> definitions;
        private Optional<Value>                     body;

        private Builder() {
            sourceRange = Optional.empty();
            symbol = Optional.empty();
            definitions = Optional.empty();
            body = Optional.empty();
        }

        @Override
        public Let build() {
            return let(
                require(sourceRange, "Source range"),
                require(symbol, "Let symbol"),
                require(definitions, "Let definitions"),
                require(body, "Let body")
            );
        }

        public Builder withBody(Value body) {
            this.body = Optional.of(body);
            return this;
        }

        public Builder withDefinitions(List<DefinitionReference> definitions) {
            this.definitions = Optional.of(definitions);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
