package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.value.WithArguments.withoutArguments;
import static scotch.compiler.util.Either.left;

import java.util.List;
import java.util.Optional;
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
import scotch.symbol.Operator;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Either;
import scotch.compiler.util.Pair;

public abstract class Value {

    Value() {
        // intentionally empty
    }

    public abstract Value accumulateDependencies(DependencyAccumulator state);

    public abstract Value accumulateNames(NameAccumulator state);

    public abstract IntermediateValue generateIntermediateCode(IntermediateGenerator state);

    public WithArguments withArguments() {
        return withoutArguments(this);
    }

    public Optional<Value> asInitializer(Initializer initializer, TypeChecker state) {
        Value checkedValue = checkTypes(state);
        if (checkedValue.getType() instanceof SumType) {
            return Optional.of(new CopyInitializer(
                initializer.getSourceLocation(),
                checkedValue,
                initializer.getFields().stream()
                    .map(field -> field.checkTypes(state))
                    .collect(toList())));
        } else {
            throw new UnsupportedOperationException(); // TODO
        }
    }

    public Optional<Pair<Identifier, Operator>> asOperator(Scope scope) {
        return Optional.empty();
    }

    public abstract Value bindTypes(TypeChecker state);

    public abstract Value bindMethods(TypeChecker state);

    public abstract Value checkTypes(TypeChecker state);

    public Value collapse() {
        return this;
    }

    public abstract Value defineOperators(OperatorAccumulator state);

    public Either<Value, List<Value>> destructure() {
        return left(this);
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract CodeBlock generateBytecode(BytecodeGenerator state);

    public abstract SourceLocation getSourceLocation();

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    public boolean isOperator(Scope scope) {
        return false;
    }

    public abstract Value parsePrecedence(PrecedenceParser state);

    public String prettyPrint() {
        return "[" + getClass().getSimpleName() + "]";
    }

    public abstract Value qualifyNames(ScopedNameQualifier state);

    @Override
    public abstract String toString();

    public Value unwrap() {
        return this;
    }

    public abstract Value withType(Type type);
}
