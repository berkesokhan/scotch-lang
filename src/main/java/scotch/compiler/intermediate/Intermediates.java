package scotch.compiler.intermediate;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.reference.DefinitionReference.classRef;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.symbol.Symbol.symbol;

import java.util.List;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.symbol.descriptor.TypeParameterDescriptor;
import scotch.symbol.type.Types;

public final class Intermediates {

    public static IntermediateApply apply(List<String> captures, IntermediateValue function, IntermediateValue argument) {
        return new IntermediateApply(captures, function, argument);
    }

    public static IntermediateFunction function(List<String> captures, String argument, IntermediateValue body) {
        return new IntermediateFunction(captures, argument, body);
    }

    public static IntermediateReference instanceRef(String className, String moduleName, String... dataTypes) {
        return new IntermediateReference(DefinitionReference.instanceRef(
            classRef(symbol(className)),
            moduleRef(moduleName),
            stream(dataTypes)
                .map(Types::sum)
                .map(TypeParameterDescriptor::typeParam)
                .collect(toList())
        ));
    }

    public static IntermediateValue instanceRef(InstanceReference instanceReference) {
        return new IntermediateReference(instanceReference);
    }

    public static IntermediateLiteral literal(Object value) {
        return new IntermediateLiteral(value);
    }

    public static IntermediatePattern pattern(DecisionTree tree) {
        return new IntermediatePattern(tree);
    }

    public static IntermediateDefinition value(String name, IntermediateValue value) {
        return value(DefinitionReference.valueRef(symbol(name)), value);
    }

    public static IntermediateDefinition value(DefinitionReference name, IntermediateValue value) {
        return new IntermediateDefinition(name, value);
    }

    public static IntermediateReference valueRef(String name) {
        return new IntermediateReference(DefinitionReference.valueRef(symbol(name)));
    }

    public static IntermediateReference valueRef(ValueReference valueReference) {
        return new IntermediateReference(valueReference);
    }

    private Intermediates() {
        // intentionally empty
    }
}
