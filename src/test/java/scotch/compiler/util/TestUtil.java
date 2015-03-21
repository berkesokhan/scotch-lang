package scotch.compiler.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.Compiler.compiler;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.compiler.text.SourceLocation.NULL_SOURCE;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.type.Types.sum;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import scotch.compiler.ClassLoaderResolver;
import scotch.compiler.output.GeneratedClass;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.scanner.Token;
import scotch.compiler.scanner.Token.TokenKind;
import scotch.compiler.syntax.definition.ClassDefinition;
import scotch.compiler.syntax.definition.DataConstructorDefinition;
import scotch.compiler.syntax.definition.DataFieldDefinition;
import scotch.compiler.syntax.definition.DataTypeDefinition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.Definitions;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.definition.ModuleImport;
import scotch.compiler.syntax.definition.OperatorDefinition;
import scotch.compiler.syntax.definition.RootDefinition;
import scotch.compiler.syntax.definition.UnshuffledDefinition;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.pattern.CaptureMatch;
import scotch.compiler.syntax.pattern.EqualMatch;
import scotch.compiler.syntax.pattern.IgnorePattern;
import scotch.compiler.syntax.pattern.OrdinalField;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.pattern.PatternMatch;
import scotch.compiler.syntax.pattern.Patterns;
import scotch.compiler.syntax.pattern.OrdinalStructureMatch;
import scotch.compiler.syntax.pattern.UnshuffledStructureMatch;
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
import scotch.compiler.syntax.value.CharLiteral;
import scotch.compiler.syntax.value.Conditional;
import scotch.compiler.syntax.value.DataConstructor;
import scotch.compiler.syntax.value.DoubleLiteral;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Initializer;
import scotch.compiler.syntax.value.InitializerField;
import scotch.compiler.syntax.value.Instance;
import scotch.compiler.syntax.value.IntLiteral;
import scotch.compiler.syntax.value.Let;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.StringLiteral;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.syntax.value.Values;
import scotch.symbol.FieldSignature;
import scotch.symbol.MethodSignature;
import scotch.symbol.Symbol;
import scotch.symbol.Value.Fixity;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.DataFieldDescriptor;
import scotch.symbol.descriptor.DataTypeDescriptor;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.descriptor.TypeParameterDescriptor;
import scotch.symbol.type.Type;

public class TestUtil {

    public static Argument arg(String name, Type type) {
        return Values.arg(NULL_SOURCE, name, type);
    }

    public static Type boolType() {
        return sum("scotch.data.bool.Bool");
    }

    public static CaptureMatch capture(String name, Type type) {
        return Patterns.capture(NULL_SOURCE, Optional.empty(), symbol(name), type);
    }

    public static CaptureMatch capture(String argument, String name, Type type) {
        return Patterns.capture(NULL_SOURCE, Optional.of(argument), symbol(name), type);
    }

    public static ClassDefinition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return Definitions.classDef(NULL_SOURCE, symbol(name), arguments, members);
    }

    public static ClassReference classRef(String className) {
        return DefinitionReference.classRef(symbol(className));
    }

    public static Conditional conditional(Value condition, Value whenTrue, Value whenFalse, Type type) {
        return Values.conditional(NULL_SOURCE, condition, whenTrue, whenFalse, type);
    }

    public static Value constantRef(String name, String dataType, FieldSignature fieldSignature, Type type) {
        return Values.constantRef(NULL_SOURCE, symbol(name), symbol(dataType), fieldSignature, type);
    }

    public static Value constantValue(String name, String dataType, Type type) {
        return Values.constantValue(NULL_SOURCE, symbol(name), symbol(dataType), type);
    }

    public static DataConstructor construct(String name, Type type, List<Value> arguments) {
        return Values.construct(NULL_SOURCE, symbol(name), type, arguments);
    }

    public static DataConstructorDescriptor constructor(int ordinal, String dataType, String name) {
        return constructor(ordinal, dataType, name, emptyList());
    }

    public static DataConstructorDescriptor constructor(int ordinal, String dataType, String name, List<DataFieldDescriptor> fields) {
        return DataConstructorDescriptor.builder(ordinal, symbol(dataType), symbol(name))
            .withFields(fields)
            .build();
    }

    public static DataConstructorDefinition ctorDef(int ordinal, String dataType, String name) {
        return ctorDef(ordinal, dataType, name, emptyList());
    }

    public static DataConstructorDefinition ctorDef(int ordinal, String dataType, String name, List<DataFieldDefinition> fields) {
        return DataConstructorDefinition.builder()
            .withSourceLocation(NULL_SOURCE)
            .withOrdinal(ordinal)
            .withDataType(symbol(dataType))
            .withSymbol(symbol(name))
            .withFields(fields)
            .build();
    }

    public static DataTypeDefinition dataDef(String name, List<Type> parameters, List<DataConstructorDefinition> constructors) {
        return DataTypeDefinition.builder()
            .withSourceLocation(NULL_SOURCE)
            .withSymbol(symbol(name))
            .withParameters(parameters)
            .withConstructors(constructors)
            .build();
    }

    public static DataReference dataRef(String name) {
        return DefinitionReference.dataRef(symbol(name));
    }

    public static DataTypeDescriptor dataType(String name, List<Type> parameters, List<DataConstructorDescriptor> constructors) {
        return DataTypeDescriptor.builder(symbol(name))
            .withParameters(parameters)
            .withConstructors(constructors)
            .build();
    }

    public static Type doubleType() {
        return sum("scotch.data.double.Double");
    }

    public static EqualMatch equal(Value value) {
        return Patterns.equal(NULL_SOURCE, Optional.empty(), value);
    }

    public static EqualMatch equal(String argument, Value value) {
        return Patterns.equal(NULL_SOURCE, Optional.of(argument), value);
    }

    public static InitializerField field(String name, Value value) {
        return InitializerField.field(NULL_SOURCE, name, value);
    }

    public static DataFieldDefinition fieldDef(int ordinal, String name, Type type) {
        return DataFieldDefinition.builder()
            .withSourceLocation(NULL_SOURCE)
            .withOrdinal(ordinal)
            .withName(name)
            .withType(type)
            .build();
    }

    public static FunctionValue fn(String name, Argument argument, Value body) {
        return fn(name, asList(argument), body);
    }

    public static FunctionValue fn(String name, List<Argument> arguments, Value body) {
        return Values.fn(NULL_SOURCE, symbol(name), arguments, body);
    }

    public static List<GeneratedClass> generateBytecode(String methodName, ClassLoaderResolver resolver, String... lines) {
        return compiler(resolver, URI.create("test://" + methodName), lines).generateBytecode();
    }

    public static Identifier id(String name, Type type) {
        return Values.id(NULL_SOURCE, symbol(name), type);
    }

    public static IgnorePattern ignore(Type type) {
        return Patterns.ignore(NULL_SOURCE, type);
    }

    public static Initializer initializer(Type type, Value value, List<InitializerField> fields) {
        return Values.initializer(NULL_SOURCE, type, value, fields);
    }

    public static Instance instance(InstanceReference reference, Type type) {
        return Values.instance(NULL_SOURCE, reference, type);
    }

    public static InstanceReference instanceRef(String moduleName, String className, List<TypeParameterDescriptor> parameters) {
        return DefinitionReference.instanceRef(classRef(className), moduleRef(moduleName), parameters);
    }

    public static Type intType() {
        return sum("scotch.data.int.Int");
    }

    public static Let let(String name, List<DefinitionReference> definitions, Value body) {
        return Values.let(NULL_SOURCE, symbol(name), definitions, body);
    }

    public static BoolLiteral literal(boolean value) {
        return Values.literal(NULL_SOURCE, value);
    }

    public static CharLiteral literal(char value) {
        return Values.literal(NULL_SOURCE, value);
    }

    public static DoubleLiteral literal(double value) {
        return Values.literal(NULL_SOURCE, value);
    }

    public static IntLiteral literal(int value) {
        return Values.literal(NULL_SOURCE, value);
    }

    public static StringLiteral literal(String value) {
        return Values.literal(NULL_SOURCE, value);
    }

    public static PatternMatcher matcher(String symbol, Type type, Argument argument, PatternCase... matchers) {
        return matcher(symbol, type, asList(argument), matchers);
    }

    public static PatternMatcher matcher(String symbol, Type type, List<Argument> arguments, PatternCase... matchers) {
        return Values.matcher(NULL_SOURCE, symbol(symbol), type, arguments, asList(matchers));
    }

    public static Value method(String name, List<Type> instances, Type type) {
        return Values.method(NULL_SOURCE, valueRef(name), instances, type);
    }

    public static ModuleImport moduleImport(String moduleName) {
        return Import.moduleImport(NULL_SOURCE, moduleName);
    }

    public static OperatorDefinition operatorDef(String name, Fixity fixity, int precedence) {
        return Definitions.operatorDef(NULL_SOURCE, symbol(name), fixity, precedence);
    }

    public static OperatorReference operatorRef(String name) {
        return DefinitionReference.operatorRef(symbol(name));
    }

    public static OrdinalField ordinalField(Type type, PatternMatch patternMatch) {
        return Patterns.ordinalField(NULL_SOURCE, Optional.empty(), Optional.empty(), type, patternMatch);
    }

    public static OrdinalField ordinalField(String argument, String field, Type type, PatternMatch patternMatch) {
        return Patterns.ordinalField(NULL_SOURCE, Optional.of(argument), Optional.of(field), type, patternMatch);
    }

    public static PatternCase pattern(String name, List<PatternMatch> matches, Value body) {
        return Patterns.pattern(NULL_SOURCE, symbol(name), matches, body);
    }

    public static RootDefinition root(List<DefinitionReference> definitions) {
        return Definitions.root(NULL_SOURCE, definitions);
    }

    public static ScopeReference scopeRef(String name) {
        return DefinitionReference.scopeRef(symbol(name));
    }

    public static SignatureReference signatureRef(String name) {
        return DefinitionReference.signatureRef(symbol(name));
    }

    public static Type stringType() {
        return sum("scotch.data.string.String");
    }

    public static OrdinalStructureMatch ordinalStruct(String dataType, Type type, List<OrdinalField> fields) {
        return Patterns.structure(NULL_SOURCE, Optional.empty(), symbol(dataType), type, fields);
    }

    public static OrdinalStructureMatch ordinalStruct(String argument, String dataType, Type type, List<OrdinalField> fields) {
        return Patterns.structure(NULL_SOURCE, Optional.of(argument), symbol(dataType), type, fields);
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
        return TypeClassDescriptor.typeClass(symbol(name), parameters, members.stream()
            .map(Symbol::symbol)
            .collect(toList()));
    }

    public static TypeInstanceDescriptor typeInstance(String moduleName, String typeClass, List<Type> parameters, MethodSignature instanceGetter) {
        return TypeInstanceDescriptor.typeInstance(moduleName, symbol(typeClass), parameters, instanceGetter);
    }

    public static UnshuffledValue unshuffled(Value... members) {
        return Values.unshuffled(NULL_SOURCE, asList(members));
    }

    public static UnshuffledDefinition unshuffled(String name, List<PatternMatch> matches, Value body) {
        return Definitions.unshuffled(NULL_SOURCE, symbol(name), matches, body);
    }

    public static UnshuffledStructureMatch unshuffledMatch(Type type, PatternMatch... matches) {
        return Patterns.unshuffledMatch(NULL_SOURCE, type, asList(matches));
    }

    public static ValueDefinition value(String name, Value value) {
        return Definitions.value(NULL_SOURCE, symbol(name), value);
    }

    public static ValueReference valueRef(String name) {
        return DefinitionReference.valueRef(symbol(name));
    }

    private TestUtil() {
        // intentionally empty
    }
}
