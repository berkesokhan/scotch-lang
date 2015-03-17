package scotch.compiler.syntax.value;

import java.util.List;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.Definitions;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

public class Values {

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
        return new DefinitionEntry(scope, Definitions.scopeDef(function.getSourceRange(), function.getSymbol()));
    }

    public static DefinitionEntry entry(Scope scope, PatternCase matcher) {
        return DefinitionEntry.entry(scope, Definitions.scopeDef(matcher.getSourceRange(), matcher.getSymbol()));
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

    public static PatternMatcher matcher(SourceRange sourceRange, Symbol symbol, Type type, List<Argument> arguments, List<PatternCase> patterns) {
        return new PatternMatcher(sourceRange, symbol, arguments, patterns, type);
    }

    public static Value method(SourceRange sourceRange, ValueReference valueRef, List<? extends Type> instances, Type type) {
        return new Method(sourceRange, valueRef, instances, type);
    }

    public static UnboundMethod unboundMethod(SourceRange sourceRange, ValueReference valueRef, Type type) {
        return new UnboundMethod(sourceRange, valueRef, type);
    }

    public static UnshuffledValue unshuffled(SourceRange sourceRange, List<Value> members) {
        return new UnshuffledValue(sourceRange, members);
    }

    private Values() {
        // intentionally empty
    }
}
