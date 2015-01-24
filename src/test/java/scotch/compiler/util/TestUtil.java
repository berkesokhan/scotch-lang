package scotch.compiler.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.Compiler.compiler;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.List;
import java.util.Optional;
import scotch.compiler.Compiler;
import scotch.compiler.GeneratedClass;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.scanner.Token;
import scotch.compiler.scanner.Token.TokenKind;
import scotch.compiler.symbol.ClasspathResolver;
import scotch.compiler.symbol.DataConstructorDescriptor;
import scotch.compiler.symbol.DataFieldDescriptor;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.definition.ClassDefinition;
import scotch.compiler.syntax.definition.DataConstructorDefinition;
import scotch.compiler.syntax.definition.DataFieldDefinition;
import scotch.compiler.syntax.definition.DataTypeDefinition;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.definition.ModuleImport;
import scotch.compiler.syntax.definition.OperatorDefinition;
import scotch.compiler.syntax.definition.RootDefinition;
import scotch.compiler.syntax.definition.UnshuffledDefinition;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.DataReference;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.OperatorReference;
import scotch.compiler.syntax.reference.ScopeReference;
import scotch.compiler.syntax.reference.SignatureReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.BoolLiteral;
import scotch.compiler.syntax.value.CaptureMatch;
import scotch.compiler.syntax.value.CharLiteral;
import scotch.compiler.syntax.value.Conditional;
import scotch.compiler.syntax.value.DataConstructor;
import scotch.compiler.syntax.value.DoubleLiteral;
import scotch.compiler.syntax.value.EqualMatch;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Initializer;
import scotch.compiler.syntax.value.InitializerField;
import scotch.compiler.syntax.value.Instance;
import scotch.compiler.syntax.value.IntLiteral;
import scotch.compiler.syntax.value.Let;
import scotch.compiler.syntax.value.Method;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.PatternMatchers;
import scotch.compiler.syntax.value.StringLiteral;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.runtime.Callable;

public class TestUtil {

    public static Argument arg(String name, Type type) {
        return Value.arg(NULL_SOURCE, name, type);
    }

    public static Type boolType() {
        return sum("scotch.data.bool.Bool");
    }

    public static CaptureMatch capture(String name, Type type) {
        return PatternMatch.capture(NULL_SOURCE, Optional.empty(), Symbol.fromString(name), type);
    }

    public static CaptureMatch capture(String argument, String name, Type type) {
        return PatternMatch.capture(NULL_SOURCE, Optional.of(argument), Symbol.fromString(name), type);
    }

    public static ClassDefinition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return Definition.classDef(NULL_SOURCE, Symbol.fromString(name), arguments, members);
    }

    public static ClassReference classRef(String className) {
        return DefinitionReference.classRef(Symbol.fromString(className));
    }

    public static Conditional conditional(Value condition, Value whenTrue, Value whenFalse, Type type) {
        return Value.conditional(NULL_SOURCE, condition, whenTrue, whenFalse, type);
    }

    public static Value constant(String name, String dataType, Type type) {
        return Value.constant(NULL_SOURCE, Symbol.fromString(name), Symbol.fromString(dataType), type);
    }

    public static DataConstructor construct(String name, Type type, List<Value> arguments) {
        return Value.construct(NULL_SOURCE, Symbol.fromString(name), type, arguments);
    }

    public static DataConstructorDescriptor constructor(String dataType, String name) {
        return constructor(dataType, name, emptyList());
    }

    public static DataConstructorDescriptor constructor(String dataType, String name, List<DataFieldDescriptor> fields) {
        return DataConstructorDescriptor.builder(Symbol.fromString(dataType), Symbol.fromString(name))
            .withFields(fields)
            .build();
    }

    public static DataConstructorDefinition ctorDef(String dataType, String name) {
        return ctorDef(dataType, name, emptyList());
    }

    public static DataConstructorDefinition ctorDef(String dataType, String name, List<DataFieldDefinition> fields) {
        return DataConstructorDefinition.builder()
            .withSourceRange(NULL_SOURCE)
            .withDataType(Symbol.fromString(dataType))
            .withSymbol(Symbol.fromString(name))
            .withFields(fields)
            .build();
    }

    public static DataTypeDefinition dataDef(String name, List<Type> parameters, List<DataConstructorDefinition> constructors) {
        return DataTypeDefinition.builder()
            .withSourceRange(NULL_SOURCE)
            .withSymbol(Symbol.fromString(name))
            .withParameters(parameters)
            .withConstructors(constructors)
            .build();
    }

    public static DataReference dataRef(String name) {
        return DefinitionReference.dataRef(Symbol.fromString(name));
    }

    public static DataTypeDescriptor dataType(String name, List<Type> parameters, List<DataConstructorDescriptor> constructors) {
        return DataTypeDescriptor.builder(Symbol.fromString(name))
            .withParameters(parameters)
            .withConstructors(constructors)
            .build();
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

    @SuppressWarnings("unchecked")
    public static <A> A exec(String... lines) {
        try {
            ClasspathResolver resolver = new ClasspathResolver(Compiler.class.getClassLoader());
            BytecodeClassLoader classLoader = new BytecodeClassLoader();
            classLoader.defineAll(generateBytecode(resolver, lines));
            return ((Callable<A>) classLoader.loadClass("scotch.test.ScotchModule").getMethod("run").invoke(null)).call();
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static InitializerField field(String name, Value value) {
        return InitializerField.field(NULL_SOURCE, name, value);
    }

    public static DataFieldDefinition fieldDef(String name, Type type) {
        return DataFieldDefinition.builder()
            .withSourceRange(NULL_SOURCE)
            .withName(name)
            .withType(type)
            .build();
    }

    public static FunctionValue fn(String name, Argument argument, Value body) {
        return fn(name, asList(argument), body);
    }

    public static FunctionValue fn(String name, List<Argument> arguments, Value body) {
        return Value.fn(NULL_SOURCE, Symbol.fromString(name), arguments, body);
    }

    public static List<GeneratedClass> generateBytecode(ClasspathResolver resolver, String... lines) {
        return compiler(resolver, "$test", lines).generateBytecode();
    }

    public static Identifier id(String name, Type type) {
        return Value.id(NULL_SOURCE, Symbol.fromString(name), type);
    }

    public static Initializer initializer(Type type, Value value, List<InitializerField> fields) {
        return Value.initializer(NULL_SOURCE, type, value, fields);
    }

    public static Instance instance(InstanceReference reference, Type type) {
        return Value.instance(NULL_SOURCE, reference, type);
    }

    public static InstanceReference instanceRef(String moduleName, String className, List<Type> parameters) {
        return DefinitionReference.instanceRef(classRef(className), moduleRef(moduleName), parameters);
    }

    public static Type intType() {
        return sum("scotch.data.int.Int");
    }

    public static Let let(String name, List<DefinitionReference> definitions, Value body) {
        return Value.let(NULL_SOURCE, Symbol.fromString(name), definitions, body);
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

    public static Method method(String name, List<Type> instances, Type type) {
        return Value.method(NULL_SOURCE, valueRef(name), instances, type);
    }

    public static ModuleImport moduleImport(String moduleName) {
        return Import.moduleImport(NULL_SOURCE, moduleName);
    }

    public static OperatorDefinition operatorDef(String name, Fixity fixity, int precedence) {
        return Definition.operatorDef(NULL_SOURCE, Symbol.fromString(name), fixity, precedence);
    }

    public static OperatorReference operatorRef(String name) {
        return DefinitionReference.operatorRef(Symbol.fromString(name));
    }

    public static PatternMatcher pattern(String name, List<PatternMatch> matches, Value body) {
        return PatternMatcher.pattern(NULL_SOURCE, Symbol.fromString(name), matches, body);
    }

    public static PatternMatchers patterns(Type type, PatternMatcher... matchers) {
        return Value.patterns(NULL_SOURCE, type, asList(matchers));
    }

    public static RootDefinition root(List<DefinitionReference> definitions) {
        return Definition.root(NULL_SOURCE, definitions);
    }

    public static ScopeReference scopeRef(String name) {
        return DefinitionReference.scopeRef(Symbol.fromString(name));
    }

    public static SignatureReference signatureRef(String name) {
        return DefinitionReference.signatureRef(Symbol.fromString(name));
    }

    public static Type stringType() {
        return sum("scotch.data.string.String");
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
        return TypeClassDescriptor.typeClass(Symbol.fromString(name), parameters, members.stream()
            .map(Symbol::fromString)
            .collect(toList()));
    }

    public static TypeInstanceDescriptor typeInstance(String moduleName, String typeClass, List<Type> parameters, MethodSignature instanceGetter) {
        return TypeInstanceDescriptor.typeInstance(moduleName, Symbol.fromString(typeClass), parameters, instanceGetter);
    }

    public static UnshuffledValue unshuffled(Value... members) {
        return Value.unshuffled(NULL_SOURCE, asList(members));
    }

    public static UnshuffledDefinition unshuffled(String name, List<PatternMatch> matches, Value body) {
        return Definition.unshuffled(NULL_SOURCE, Symbol.fromString(name), matches, body);
    }

    public static ValueDefinition value(String name, Type type, Value value) {
        return Definition.value(NULL_SOURCE, Symbol.fromString(name), type, value);
    }

    public static ValueReference valueRef(String name) {
        return DefinitionReference.valueRef(Symbol.fromString(name));
    }

    private TestUtil() {
        // intentionally empty
    }
}
