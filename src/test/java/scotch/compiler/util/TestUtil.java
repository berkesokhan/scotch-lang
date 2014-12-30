package scotch.compiler.util;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.List;
import java.util.Optional;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.scanner.Token;
import scotch.compiler.scanner.Token.TokenKind;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.ClassDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.ClassReference;
import scotch.compiler.syntax.DefinitionReference.InstanceReference;
import scotch.compiler.syntax.DefinitionReference.OperatorReference;
import scotch.compiler.syntax.DefinitionReference.PatternReference;
import scotch.compiler.syntax.DefinitionReference.ValueReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.Import.ModuleImport;
import scotch.compiler.syntax.InstanceMap;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.Instance;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.Method;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;

public class TestUtil {

    public static Argument arg(String name, Type type) {
        return Value.arg(NULL_SOURCE, name, type);
    }

    public static Value instanceMethod(String name, InstanceReference instance, Type type) {
        return Value.boundMethod(NULL_SOURCE, valueRef(name), instance, type);
    }

    public static CaptureMatch capture(String name, Type type) {
        return PatternMatch.capture(NULL_SOURCE, Optional.empty(), fromString(name), type);
    }

    public static CaptureMatch capture(String argument, String name, Type type) {
        return PatternMatch.capture(NULL_SOURCE, Optional.of(argument), fromString(name), type);
    }

    public static ClassDefinition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return Definition.classDef(NULL_SOURCE, fromString(name), arguments, members);
    }

    public static ClassReference classRef(String className) {
        return DefinitionReference.classRef(fromString(className));
    }

    public static Type doubleType() {
        return sum("scotch.data.double.Double");
    }

    public static EqualMatch equal(Value value) {
        return PatternMatch.equal(NULL_SOURCE, Optional.empty(), value);
    }

    public static EqualMatch equal(String argument, Value value) {
        return PatternMatch.equal(NULL_SOURCE, Optional.of(argument), value);
    }

    public static FunctionValue fn(String name, Argument argument, Value body) {
        return fn(name, asList(argument), body);
    }

    public static FunctionValue fn(String name, List<Argument> arguments, Value body) {
        return Value.fn(NULL_SOURCE, fromString(name), arguments, body);
    }

    public static Identifier id(String name, Type type) {
        return Value.id(NULL_SOURCE, fromString(name), type);
    }

    public static InstanceReference instanceRef(String moduleName, String className, List<Type> parameters) {
        return DefinitionReference.instanceRef(classRef(className), moduleRef(moduleName), parameters);
    }

    public static Type intType() {
        return sum("scotch.data.int.Int");
    }

    public static BoolLiteral literal(boolean value) {
        return Value.literal(NULL_SOURCE, value);
    }

    public static CharLiteral literal(char value) {
        return Value.literal(NULL_SOURCE, value);
    }

    public static DoubleLiteral literal(double value) {
        return Value.literal(NULL_SOURCE, value);
    }

    public static IntLiteral literal(int value) {
        return Value.literal(NULL_SOURCE, value);
    }

    public static StringLiteral literal(String value) {
        return Value.literal(NULL_SOURCE, value);
    }

    public static Message message(Value... members) {
        return Value.message(NULL_SOURCE, asList(members));
    }

    public static Method method(String name, List<Type> instances, Type type) {
        return Value.method(NULL_SOURCE, valueRef(name), instances, type);
    }

    public static Instance instance(InstanceReference reference, Type type) {
        return Value.instance(NULL_SOURCE, reference, type);
    }

    public static ModuleImport moduleImport(String moduleName) {
        return Import.moduleImport(NULL_SOURCE, moduleName);
    }

    public static OperatorDefinition operatorDef(String name, Fixity fixity, int precedence) {
        return Definition.operatorDef(NULL_SOURCE, fromString(name), fixity, precedence);
    }

    public static OperatorReference operatorRef(String name) {
        return DefinitionReference.operatorRef(fromString(name));
    }

    public static PatternMatcher pattern(String name, List<PatternMatch> matches, Value body) {
        return PatternMatcher.pattern(NULL_SOURCE, fromString(name), matches, body);
    }

    public static PatternReference patternRef(String name) {
        return DefinitionReference.patternRef(fromString(name));
    }

    public static PatternMatchers patterns(Type type, PatternMatcher... matchers) {
        return Value.patterns(NULL_SOURCE, type, asList(matchers));
    }

    public static RootDefinition root(List<DefinitionReference> definitions) {
        return Definition.root(NULL_SOURCE, definitions);
    }

    public static Token token(TokenKind kind, Object value) {
        return Token.token(kind, value, NULL_SOURCE);
    }

    public static Token tokenAt(Scanner scanner, int offset) {
        Token token = null;
        for (int i = 0; i < offset; i++) {
            token = scanner.nextToken();
        }
        return token;
    }

    public static TypeClassDescriptor typeClass(String name, List<Type> parameters, List<String> members) {
        return TypeClassDescriptor.typeClass(fromString(name), parameters, members.stream()
            .map(Symbol::fromString)
            .collect(toList()));
    }

    public static TypeInstanceDescriptor typeInstance(String moduleName, String typeClass, List<Type> parameters, MethodSignature instanceGetter) {
        return TypeInstanceDescriptor.typeInstance(moduleName, fromString(typeClass), parameters, instanceGetter);
    }

    public static UnshuffledPattern unshuffled(String name, List<PatternMatch> matches, Value body) {
        return Definition.unshuffled(NULL_SOURCE, fromString(name), matches, body);
    }

    public static ValueDefinition value(String name, Type type, Value value) {
        return Definition.value(NULL_SOURCE, fromString(name), type, value, InstanceMap.empty());
    }

    public static ValueReference valueRef(String name) {
        return DefinitionReference.valueRef(fromString(name));
    }

    private TestUtil() {
        // intentionally empty
    }
}
