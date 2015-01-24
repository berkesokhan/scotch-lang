package scotch.compiler.syntax.value;

import static scotch.data.either.Either.left;

import java.util.List;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulatorState;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.ScopeDefinition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;
import scotch.data.tuple.Tuple2;

public abstract class Value {

    public static Apply apply(Value function, Value argument, Type type) {
        return new Apply(function.getSourceRange().extend(argument.getSourceRange()), function, argument, type);
    }

    public static Argument arg(SourceRange sourceRange, String name, Type type) {
        return new Argument(sourceRange, name, type);
    }

    public static Conditional conditional(SourceRange sourceRange, Value condition, Value whenTrue, Value whenFalse, Type type) {
        return new Conditional(sourceRange, condition, whenTrue, whenFalse, type);
    }

    public static Constant constant(SourceRange sourceRange, Symbol symbol, Symbol dataType, Type type) {
        return new Constant(sourceRange, dataType, symbol, type);
    }

    public static DataConstructor construct(SourceRange sourceRange, Symbol symbol, Type type, List<Value> arguments) {
        return new DataConstructor(sourceRange, symbol, type, arguments);
    }

    public static DefinitionEntry entry(Scope scope, FunctionValue function) {
        return new DefinitionEntry(scope, Definition.scopeDef(function.getSourceRange(), function.getSymbol()));
    }

    public static DefinitionEntry entry(Scope scope, PatternMatcher matcher) {
        return DefinitionEntry.entry(scope, Definition.scopeDef(matcher.getSourceRange(), matcher.getSymbol()));
    }

    public static FunctionValue fn(SourceRange sourceRange, Symbol symbol, List<Argument> arguments, Value body) {
        return new FunctionValue(sourceRange, symbol, arguments, body, Optional.empty());
    }

    public static Identifier id(SourceRange sourceRange, Symbol symbol, Type type) {
        return new Identifier(sourceRange, symbol, type);
    }

    public static Initializer initializer(SourceRange sourceRange, Type type, Value value, List<InitializerField> fields) {
        return new Initializer(sourceRange, value, fields, type);
    }

    public static Instance instance(SourceRange sourceRange, InstanceReference reference, Type type) {
        return new Instance(sourceRange, reference, type);
    }

    public static Let let(SourceRange sourceRange, Symbol symbol, List<DefinitionReference> definitions, Value body) {
        return new Let(sourceRange, symbol, definitions, body);
    }

    public static BoolLiteral literal(SourceRange sourceRange, boolean value) {
        return new BoolLiteral(sourceRange, value);
    }

    public static CharLiteral literal(SourceRange sourceRange, char value) {
        return new CharLiteral(sourceRange, value);
    }

    public static DoubleLiteral literal(SourceRange sourceRange, double value) {
        return new DoubleLiteral(sourceRange, value);
    }

    public static IntLiteral literal(SourceRange sourceRange, int value) {
        return new IntLiteral(sourceRange, value);
    }

    public static StringLiteral literal(SourceRange sourceRange, String value) {
        return new StringLiteral(sourceRange, value);
    }

    public static Method method(SourceRange sourceRange, ValueReference valueRef, List<Type> instances, Type type) {
        return new Method(sourceRange, valueRef, instances, type);
    }

    public static PatternMatchers patterns(SourceRange sourceRange, Type type, List<PatternMatcher> patterns) {
        return new PatternMatchers(sourceRange, patterns, type);
    }

    public static ScopeDefinition scopeDef(FunctionValue function) {
        return Definition.scopeDef(function.getSourceRange(), function.getSymbol());
    }

    public static ScopeDefinition scopeDef(Let let) {
        return Definition.scopeDef(let.getSourceRange(), let.getSymbol());
    }

    public static ScopeDefinition scopeDef(PatternMatcher matcher) {
        return Definition.scopeDef(matcher.getSourceRange(), matcher.getSymbol());
    }

    public static UnboundMethod unboundMethod(SourceRange sourceRange, ValueReference valueRef, Type type) {
        return new UnboundMethod(sourceRange, valueRef, type);
    }

    public static UnshuffledValue unshuffled(SourceRange sourceRange, List<Value> members) {
        return new UnshuffledValue(sourceRange, members);
    }

    Value() {
        // intentionally empty
    }

    public abstract Value accumulateDependencies(DependencyAccumulator state);

    public abstract Value accumulateNames(NameAccumulatorState state);

    public Either<Value, FunctionValue> asFunction() {
        return left(this);
    }

    public Optional<Tuple2<Identifier, Operator>> asOperator(Scope scope) {
        return Optional.empty();
    }

    public abstract Value bindMethods(TypeChecker state);

    public abstract Value bindTypes(TypeChecker state);

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

    public abstract SourceRange getSourceRange();

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

    public abstract Value qualifyNames(NameQualifier state);

    @Override
    public abstract String toString();

    public Value unwrap() {
        return this;
    }

    public abstract Value withType(Type type);
}
