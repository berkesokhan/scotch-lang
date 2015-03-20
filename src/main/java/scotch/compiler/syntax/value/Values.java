package scotch.compiler.syntax.value;

import java.util.List;
import java.util.Optional;
import scotch.symbol.FieldSignature;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.Definitions;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;

public class Values {

    public static Apply apply(Value function, Value argument, Type type) {
        return new Apply(function.getSourceLocation().extend(argument.getSourceLocation()), function, argument, type);
    }

    public static Argument arg(SourceLocation sourceLocation, String name, Type type) {
        return new Argument(sourceLocation, name, type);
    }

    public static Conditional conditional(SourceLocation sourceLocation, Value condition, Value whenTrue, Value whenFalse, Type type) {
        return new Conditional(sourceLocation, condition, whenTrue, whenFalse, type);
    }

    public static ConstantReference constantRef(SourceLocation sourceLocation, Symbol symbol, Symbol dataType, FieldSignature fieldSignature, Type type) {
        return new ConstantReference(sourceLocation, symbol, dataType, fieldSignature, type);
    }

    public static ConstantValue constantValue(SourceLocation sourceLocation, Symbol symbol, Symbol dataType, Type type) {
        return new ConstantValue(sourceLocation, dataType, symbol, type);
    }

    public static DataConstructor construct(SourceLocation sourceLocation, Symbol symbol, Type type, List<Value> arguments) {
        return new DataConstructor(sourceLocation, symbol, type, arguments);
    }

    public static DefinitionEntry entry(Scope scope, FunctionValue function) {
        return new DefinitionEntry(scope, Definitions.scopeDef(function.getSourceLocation(), function.getSymbol()));
    }

    public static DefinitionEntry entry(Scope scope, PatternCase matcher) {
        return DefinitionEntry.entry(scope, Definitions.scopeDef(matcher.getSourceLocation(), matcher.getSymbol()));
    }

    public static FunctionValue fn(SourceLocation sourceLocation, Symbol symbol, List<Argument> arguments, Value body) {
        return new FunctionValue(sourceLocation, symbol, arguments, body, Optional.empty());
    }

    public static Identifier id(SourceLocation sourceLocation, Symbol symbol, Type type) {
        return new Identifier(sourceLocation, symbol, type);
    }

    public static Initializer initializer(SourceLocation sourceLocation, Type type, Value value, List<InitializerField> fields) {
        return new Initializer(sourceLocation, value, fields, type);
    }

    public static Instance instance(SourceLocation sourceLocation, InstanceReference reference, Type type) {
        return new Instance(sourceLocation, reference, type);
    }

    public static Let let(SourceLocation sourceLocation, Symbol symbol, List<DefinitionReference> definitions, Value body) {
        return new Let(sourceLocation, symbol, definitions, body);
    }

    public static BoolLiteral literal(SourceLocation sourceLocation, boolean value) {
        return new BoolLiteral(sourceLocation, value);
    }

    public static CharLiteral literal(SourceLocation sourceLocation, char value) {
        return new CharLiteral(sourceLocation, value);
    }

    public static DoubleLiteral literal(SourceLocation sourceLocation, double value) {
        return new DoubleLiteral(sourceLocation, value);
    }

    public static IntLiteral literal(SourceLocation sourceLocation, int value) {
        return new IntLiteral(sourceLocation, value);
    }

    public static StringLiteral literal(SourceLocation sourceLocation, String value) {
        return new StringLiteral(sourceLocation, value);
    }

    public static PatternMatcher matcher(SourceLocation sourceLocation, Symbol symbol, Type type, List<Argument> arguments, List<PatternCase> patterns) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patterns, type);
    }

    public static Value method(SourceLocation sourceLocation, ValueReference valueRef, List<? extends Type> instances, Type type) {
        return new Method(sourceLocation, valueRef, instances, type);
    }

    public static UnboundMethod unboundMethod(SourceLocation sourceLocation, ValueReference valueRef, Type type) {
        return new UnboundMethod(sourceLocation, valueRef, type);
    }

    public static UnshuffledValue unshuffled(SourceLocation sourceLocation, List<Value> members) {
        return new UnshuffledValue(sourceLocation, members);
    }

    private Values() {
        // intentionally empty
    }
}
