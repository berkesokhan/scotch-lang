package scotch.compiler.syntax.value;

import java.util.List;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.ScopeDefinition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

public abstract class Value {

    public static Apply apply(Value function, Value argument, Type type) {
        return new Apply(function.getSourceRange().extend(argument.getSourceRange()), function, argument, type);
    }

    public static Argument arg(SourceRange sourceRange, String name, Type type) {
        return new Argument(sourceRange, name, type);
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

    public abstract <T> T accept(ValueVisitor<T> visitor);

    public abstract Value accumulateDependencies(SyntaxTreeParser state);

    public abstract Value accumulateNames(SyntaxTreeParser state);

    public abstract Value bindMethods(TypeChecker state);

    public abstract Value bindTypes(TypeChecker state);

    public abstract Value checkTypes(TypeChecker state);

    public abstract Value defineOperators(SyntaxTreeParser state);

    @Override
    public abstract boolean equals(Object o);

    public abstract CodeBlock generateBytecode(BytecodeGenerator state);

    public abstract SourceRange getSourceRange();

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    public abstract Value parsePrecedence(SyntaxTreeParser state);

    public abstract Value qualifyNames(SyntaxTreeParser state);

    @Override
    public abstract String toString();

    public Value unwrap() {
        return this;
    }

    public abstract Value withType(Type type);

    public interface ValueVisitor<T> {

        default T visit(Apply apply) {
            return visitOtherwise(apply);
        }

        default T visit(Argument argument) {
            return visitOtherwise(argument);
        }

        default T visit(BoolLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(BoundValue boundValue) {
            return visitOtherwise(boundValue);
        }

        default T visit(CharLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(DoubleLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(FunctionValue function) {
            return visitOtherwise(function);
        }

        default T visit(Identifier identifier) {
            return visitOtherwise(identifier);
        }

        default T visit(IntLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(Instance instance) {
            return visitOtherwise(instance);
        }

        default T visit(LambdaValue lambda) {
            return visitOtherwise(lambda);
        }

        default T visit(Let let) {
            return visitOtherwise(let);
        }

        default T visit(UnshuffledValue value) {
            return visitOtherwise(value);
        }

        default T visit(Method method) {
            return visitOtherwise(method);
        }

        default T visit(PatternMatchers matchers) {
            return visitOtherwise(matchers);
        }

        default T visit(StringLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(UnboundMethod unboundMethod) {
            return visitOtherwise(unboundMethod);
        }

        default T visitOtherwise(Value value) {
            throw new UnsupportedOperationException("Can't visit " + value.getClass().getSimpleName());
        }
    }
}
