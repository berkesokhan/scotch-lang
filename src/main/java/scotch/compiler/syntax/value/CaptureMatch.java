package scotch.compiler.syntax.value;

import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definition.classDef;
import static scotch.compiler.util.Either.right;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.ClassDefinition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Either;
import scotch.data.tuple.Tuple2;

public class CaptureMatch extends PatternMatch {

    private final SourceRange      sourceRange;
    private final Optional<String> argument;
    private final Symbol           symbol;
    private final Type             type;

    CaptureMatch(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.argument = argument;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        state.defineValue(symbol, type);
        state.specialize(type);
        return this;
    }

    @Override
    public Either<PatternMatch, CaptureMatch> asCapture() {
        return right(this);
    }

    @Override
    public Optional<Tuple2<CaptureMatch, Operator>> asOperator(Scope scope) {
        return scope.qualify(symbol)
            .map(scope::getOperator)
            .map(operator -> tuple2(this, operator));
    }

    @Override
    public PatternMatch bind(String argument, Scope scope) {
        if (this.argument.isPresent()) {
            throw new IllegalStateException();
        } else {
            return capture(sourceRange, Optional.of(argument), symbol, type);
        }
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        Scope scope = state.scope();
        state.addLocal(symbol);
        return scope.generate(type)
            .unify(scope.getValue(unqualified(getArgument())), scope)
            .accept(new UnificationVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Unified unified) {
                    return withType(unified.getUnifiedType());
                }

                @Override
                public PatternMatch visitOtherwise(Unification unification) {
                    state.error(typeError(unification, sourceRange));
                    return CaptureMatch.this;
                }
            });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof CaptureMatch) {
            CaptureMatch other = (CaptureMatch) o;
            return Objects.equals(argument, other.argument)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            state.addMatch(getName());
            aload(state.getVariable(getArgument()));
            astore(state.getVariable(getName()));
        }};
    }

    public String getArgument() {
        return argument.orElseThrow(IllegalStateException::new);
    }

    public String getName() {
        return symbol.getCanonicalName();
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
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(argument, symbol, type);
    }

    @Override
    public boolean isOperator(Scope scope) {
        return scope.isOperator(symbol);
    }

    @Override
    public PatternMatch qualifyNames(NameQualifier state) {
        return this;
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public CaptureMatch withSourceRange(SourceRange sourceRange) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    @Override
    public PatternMatch withType(Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    public static class CaptureMatchBuilder implements SyntaxBuilder<CaptureMatch> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();

        public CaptureMatchBuilder() {
            // intentionally empty
        }

        @Override
        public CaptureMatch build() {
            return capture(
                require(sourceRange, "Source range"),
                Optional.empty(),
                require(symbol, "Capture symbol"),
                require(type, "Capture type")
            );
        }

        public CaptureMatchBuilder withIdentifier(Identifier identifier) {
            this.symbol = Optional.of(identifier.getSymbol());
            this.type = Optional.of(identifier.getType());
            return this;
        }

        @Override
        public CaptureMatchBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }

    public static class ClassDefinitionBuilder implements SyntaxBuilder<ClassDefinition> {

        private Optional<Symbol>                    symbol;
        private Optional<List<Type>>                arguments;
        private Optional<List<DefinitionReference>> members;
        private Optional<SourceRange>               sourceRange;

        public ClassDefinitionBuilder() {
            symbol = Optional.empty();
            arguments = Optional.empty();
            members = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public ClassDefinition build() {
            return classDef(
                require(sourceRange, "Source range"),
                require(symbol, "Class symbol"),
                require(arguments, "Class arguments"),
                require(members, "Class member definitions")
            );
        }

        public ClassDefinitionBuilder withArguments(List<Type> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public ClassDefinitionBuilder withMembers(List<DefinitionReference> members) {
            this.members = Optional.of(members);
            return this;
        }

        @Override
        public ClassDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ClassDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
