package scotch.compiler.syntax;

import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public abstract class DefinitionReference {

    public static DefinitionReference classRef(String moduleName, String name) {
        return classRef(qualified(moduleName, name));
    }

    public static DefinitionReference classRef(Symbol symbol) {
        return new ClassReference(symbol);
    }

    public static DefinitionReference moduleRef(String name) {
        return new ModuleReference(name);
    }

    public static DefinitionReference operatorRef(String moduleName, String name) {
        return operatorRef(qualified(moduleName, name));
    }

    public static DefinitionReference operatorRef(Symbol symbol) {
        return new OperatorReference(symbol);
    }

    public static DefinitionReference patternRef(String moduleName, String name) {
        return patternRef(qualified(moduleName, name));
    }

    public static DefinitionReference patternRef(Symbol symbol) {
        return new PatternReference(symbol);
    }

    public static DefinitionReference rootRef() {
        return rootRef;
    }

    public static DefinitionReference signatureRef(String moduleName, String name) {
        return signatureRef(qualified(moduleName, name));
    }

    public static DefinitionReference signatureRef(Symbol symbol) {
        return new SignatureReference(symbol);
    }

    public static DefinitionReference valueRef(String moduleName, String name) {
        return valueRef(qualified(moduleName, name));
    }

    public static DefinitionReference valueRef(Symbol symbol) {
        return new ValueReference(symbol);
    }

    private static final DefinitionReference rootRef = new RootReference();

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

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
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

    public static class SignatureReference extends DefinitionReference {

        private final Symbol symbol;

        private SignatureReference(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof SignatureReference && Objects.equals(symbol, ((SignatureReference) o).symbol);
        }

        public String getName() {
            return symbol.getMemberName();
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

        public String getName() {
            return symbol.getMemberName();
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
