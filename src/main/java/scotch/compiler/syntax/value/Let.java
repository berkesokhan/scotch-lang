package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.value.Values.let;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;

@ToString(exclude = "sourceLocation", doNotUseGetters = true)
public class Let extends Value implements Scoped {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation            sourceLocation;
    private final Symbol                    symbol;
    private final List<DefinitionReference> definitions;
    private final Value                     body;

    Let(SourceLocation sourceLocation, Symbol symbol, List<DefinitionReference> definitions, Value body) {
        this.sourceLocation = sourceLocation;
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
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
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
    public Value defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.defineDefinitionOperators(definitions))
            .withBody(body.defineOperators(state)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Let) {
            Let other = (Let) o;
            return Objects.equals(sourceLocation, other.sourceLocation)
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
        return scopeDef(sourceLocation, symbol);
    }

    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
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
    public Value qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions))
            .withBody(body.qualifyNames(state)));
    }

    public Let withBody(Value body) {
        return new Let(sourceLocation, symbol, definitions, body);
    }

    public Let withDefinitions(List<DefinitionReference> definitions) {
        return new Let(sourceLocation, symbol, definitions, body);
    }

    @Override
    public Value withType(Type type) {
        return new Let(sourceLocation, symbol, definitions, body.withType(type));
    }

    public static class Builder implements SyntaxBuilder<Let> {

        private Optional<SourceLocation>            sourceLocation;
        private Optional<Symbol>                    symbol;
        private Optional<List<DefinitionReference>> definitions;
        private Optional<Value>                     body;

        private Builder() {
            sourceLocation = Optional.empty();
            symbol = Optional.empty();
            definitions = Optional.empty();
            body = Optional.empty();
        }

        @Override
        public Let build() {
            return let(
                require(sourceLocation, "Source location"),
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
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
