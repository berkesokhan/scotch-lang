package scotch.compiler.ast;

public abstract class DefinitionEntry {

    public static DefinitionEntry scopedEntry(DefinitionReference reference, Definition definition, Scope scope) {
        return new ScopedEntry(reference, definition, scope);
    }

    public static DefinitionEntry unscopedEntry(DefinitionReference reference, Definition definition) {
        return new UnscopedEntry(reference, definition);
    }

    private DefinitionEntry() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionEntryVisitor<T> visitor);

    public abstract Definition getDefinition();

    public abstract DefinitionReference getReference();

    public abstract void setDefinition(Definition definition);

    public interface DefinitionEntryVisitor<T> {

        default T visit(UnscopedEntry entry) {
            return visitOtherwise(entry);
        }

        default T visit(ScopedEntry entry) {
            return visitOtherwise(entry);
        }

        default T visitOtherwise(DefinitionEntry entry) {
            throw new UnsupportedOperationException("Can't visit " + entry);
        }
    }

    public static class ScopedEntry extends DefinitionEntry {

        private final DefinitionReference reference;
        private final Scope               scope;
        private       Definition          definition;

        private ScopedEntry(DefinitionReference reference, Definition definition, Scope scope) {
            this.reference = reference;
            this.definition = definition;
            this.scope = scope;
        }

        @Override
        public <T> T accept(DefinitionEntryVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Definition getDefinition() {
            return definition;
        }

        @Override
        public DefinitionReference getReference() {
            return reference;
        }

        public Scope getScope() {
            return scope;
        }

        @Override
        public void setDefinition(Definition definition) {
            this.definition = definition;
        }
    }

    public static class UnscopedEntry extends DefinitionEntry {

        private final DefinitionReference reference;
        private       Definition          definition;

        private UnscopedEntry(DefinitionReference reference, Definition definition) {
            this.reference = reference;
            this.definition = definition;
        }

        @Override
        public <T> T accept(DefinitionEntryVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Definition getDefinition() {
            return definition;
        }

        @Override
        public DefinitionReference getReference() {
            return reference;
        }

        @Override
        public void setDefinition(Definition definition) {
            this.definition = definition;
        }
    }
}
