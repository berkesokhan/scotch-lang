package scotch.compiler.syntax.reference;

import java.util.List;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.TypeInstanceDescriptor;

public abstract class DefinitionReference {

    private static final RootReference rootRef = new RootReference();

    public static ClassReference classRef(Symbol symbol) {
        return new ClassReference(symbol);
    }

    public static DataReference dataRef(Symbol symbol) {
        return new DataReference(symbol);
    }

    public static InstanceReference instanceRef(ClassReference classReference, ModuleReference moduleReference, List<Type> types) {
        return new InstanceReference(classReference, moduleReference, types);
    }

    public static InstanceReference instanceRef(TypeInstanceDescriptor descriptor) {
        return instanceRef(classRef(descriptor.getTypeClass()), moduleRef(descriptor.getModuleName()), descriptor.getParameters());
    }

    public static ModuleReference moduleRef(String name) {
        return new ModuleReference(name);
    }

    public static OperatorReference operatorRef(Symbol symbol) {
        return new OperatorReference(symbol);
    }

    public static RootReference rootRef() {
        return rootRef;
    }

    public static ScopeReference scopeRef(Symbol symbol) {
        return new ScopeReference(symbol);
    }

    public static SignatureReference signatureRef(Symbol symbol) {
        return new SignatureReference(symbol);
    }

    public static ValueReference valueRef(Symbol symbol) {
        return new ValueReference(symbol);
    }

    DefinitionReference() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
