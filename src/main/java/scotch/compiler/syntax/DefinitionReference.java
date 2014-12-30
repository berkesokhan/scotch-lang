package scotch.compiler.syntax;

import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;

public abstract class DefinitionReference {

    private static final RootReference rootRef = new RootReference();

    public static ClassReference classRef(Symbol symbol) {
        return new ClassReference(symbol);
    }

    public static InstanceReference instanceRef(ClassReference classReference, ModuleReference moduleReference, List<Type> types) {
        return new InstanceReference(classReference, moduleReference, types);
    }

    public static ModuleReference moduleRef(String name) {
        return new ModuleReference(name);
    }

    public static OperatorReference operatorRef(Symbol symbol) {
        return new OperatorReference(symbol);
    }

    public static PatternReference patternRef(Symbol symbol) {
        return new PatternReference(symbol);
    }

    public static RootReference rootRef() {
        return rootRef;
    }

    public static ScopeReference scopeRef(Symbol symbol) {
        return new ScopeReference(symbol);
    }

    public static ValueReference valueRef(Symbol symbol) {
        return new ValueReference(symbol);
    }

    private DefinitionReference() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionReferenceVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public interface DefinitionReferenceVisitor<T> {

        default T visit(ClassReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(InstanceReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(ModuleReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(OperatorReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(PatternReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(RootReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(ScopeReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(ValueReference reference) {
            return visitOtherwise(reference);
        }

        default T visitOtherwise(DefinitionReference reference) {
            throw new UnsupportedOperationException("Can't visit " + reference);
        }
    }

    public static class ClassReference extends DefinitionReference {

        private final Symbol symbol;

        private ClassReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ClassReference && Objects.equals(symbol, ((ClassReference) o).symbol);
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }

    public static class InstanceReference extends DefinitionReference {

        private final ClassReference  classReference;
        private final ModuleReference moduleReference;
        private final List<Type>      types;

        public InstanceReference(ClassReference classReference, ModuleReference moduleReference, List<Type> types) {
            this.classReference = classReference;
            this.moduleReference = moduleReference;
            this.types = types;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof DefinitionReference) {
                InstanceReference other = (InstanceReference) o;
                return Objects.equals(classReference, other.classReference)
                    && Objects.equals(moduleReference, other.moduleReference)
                    && Objects.equals(types, other.types);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(classReference, moduleReference, types);
        }

        public CodeBlock reference(Scope scope) {
            return scope.getTypeInstance(classReference, moduleReference, types).reference();
        }

        @Override
        public String toString() {
            return stringify(this) + "(classReference=" + classReference + ", moduleReference=" + moduleReference + ", types=" + types + ")";
        }
    }

    public static class ModuleReference extends DefinitionReference {

        private final String name;

        private ModuleReference(String name) {
            this.name = name;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ModuleReference && Objects.equals(name, ((ModuleReference) o).name);
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        public boolean is(String otherName) {
            return Objects.equals(name, otherName);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
        }
    }

    public static class OperatorReference extends DefinitionReference {

        private final Symbol symbol;

        private OperatorReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof OperatorReference && Objects.equals(symbol, ((OperatorReference) o).symbol);
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }

    public static class PatternReference extends DefinitionReference {

        private final Symbol symbol;

        public PatternReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof PatternReference && Objects.equals(symbol, ((PatternReference) o).symbol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }

    public static class RootReference extends DefinitionReference {

        private RootReference() {
            // intentionally empty
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return Objects.hash();
        }

        @Override
        public String toString() {
            return stringify(this);
        }
    }

    public static class ScopeReference extends DefinitionReference {

        private final Symbol symbol;

        public ScopeReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ScopeReference && Objects.equals(symbol, ((ScopeReference) o).symbol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol.getCanonicalName() + ")";
        }
    }

    public static class ValueReference extends DefinitionReference {

        private final Symbol symbol;

        private ValueReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ValueReference && Objects.equals(symbol, ((ValueReference) o).symbol);
        }

        public String getMemberName() {
            return symbol.getMemberName();
        }

        public String getName() {
            return symbol.getMemberName();
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }
}
