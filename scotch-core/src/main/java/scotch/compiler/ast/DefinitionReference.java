package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.normalizeQualified;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public abstract class DefinitionReference {

    private static final DefinitionReference rootRef = new RootReference();

    public static DefinitionReference classRef(String moduleName, String name) {
        return new ClassReference(moduleName, name);
    }

    public static DefinitionReference moduleRef(String name) {
        return new ModuleReference(name);
    }

    public static DefinitionReference opRef(String moduleName, String name) {
        return new OperatorReference(moduleName, name);
    }

    public static DefinitionReference rootRef() {
        return rootRef;
    }

    public static DefinitionReference signatureRef(String moduleName, String name) {
        return new SignatureReference(moduleName, name);
    }

    public static DefinitionReference valueRef(String moduleName, String name) {
        return new ValueReference(moduleName, name);
    }

    private DefinitionReference() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionReferenceVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract String qualify();

    @Override
    public abstract String toString();

    public interface DefinitionReferenceVisitor<T> {

        default T visit(ClassReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(ModuleReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(OperatorReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(RootReference reference) {
            return visitOtherwise(reference);
        }

        default T visit(SignatureReference reference) {
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

        private final String moduleName;
        private final String name;

        private ClassReference(String moduleName, String name) {
            this.moduleName = moduleName;
            this.name = name;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ClassReference) {
                ClassReference other = (ClassReference) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(name, other.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, name);
        }

        @Override
        public String qualify() {
            return normalizeQualified(moduleName, name);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + normalizeQualified(moduleName, name) + ")";
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

        @Override
        public String qualify() {
            return name;
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
        }
    }

    public static class OperatorReference extends DefinitionReference {

        private final String moduleName;
        private final String name;

        private OperatorReference(String moduleName, String name) {
            this.moduleName = moduleName;
            this.name = name;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof OperatorReference) {
                OperatorReference other = (OperatorReference) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(name, other.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, name);
        }

        @Override
        public String qualify() {
            return normalizeQualified(moduleName, name);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + normalizeQualified(moduleName, name) + ")";
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
        public String qualify() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return stringify(this);
        }
    }

    public static class SignatureReference extends DefinitionReference {

        private final String moduleName;
        private final String name;

        private SignatureReference(String moduleName, String name) {
            this.moduleName = moduleName;
            this.name = name;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof SignatureReference) {
                SignatureReference other = (SignatureReference) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(name, other.name);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, name);
        }

        @Override
        public String qualify() {
            return normalizeQualified(moduleName, name);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + normalizeQualified(moduleName, name) + ")";
        }
    }

    public static class ValueReference extends DefinitionReference {

        private final String moduleName;
        private final String name;

        private ValueReference(String moduleName, String name) {
            this.moduleName = moduleName;
            this.name = name;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ValueReference) {
                ValueReference other = (ValueReference) o;
                return Objects.equals(moduleName, other.moduleName)
                    && Objects.equals(name, other.name);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, name);
        }

        @Override
        public String qualify() {
            return normalizeQualified(moduleName, name);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + normalizeQualified(moduleName, name) + ")";
        }
    }
}
