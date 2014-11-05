package scotch.compiler.syntax;

public abstract class DefinitionEntry {

    public static DefinitionEntry patternEntry(PatternMatcher pattern, Scope scope) {
        return new PatternEntry(pattern, scope);
    }

    public static DefinitionEntry scopedEntry(Definition definition, Scope scope) {
        return new ScopedEntry(definition, scope);
    }

    public static DefinitionEntry unscopedEntry(Definition definition) {
        return new UnscopedEntry(definition);
    }

    private DefinitionEntry() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionEntryVisitor<T> visitor);

    public abstract DefinitionReference getReference();

    public abstract Scope getScope();

    public abstract DefinitionEntry withDefinition(Definition definition);

    public interface DefinitionEntryVisitor<T> {

        default T visit(UnscopedEntry entry) {
            return visitOtherwise(entry);
        }

        default T visit(ScopedEntry entry) {
            return visitOtherwise(entry);
        }

        default T visit(PatternEntry entry) {
            return visitOtherwise(entry);
        }

        default T visitOtherwise(DefinitionEntry entry) {
            throw new UnsupportedOperationException("Can't visit " + entry);
        }
    }

    public static class PatternEntry extends DefinitionEntry {

        private final Scope          scope;
        private final PatternMatcher pattern;

        private PatternEntry(PatternMatcher pattern, Scope scope) {
            this.scope = scope;
            this.pattern = pattern;
        }

        @Override
        public <T> T accept(DefinitionEntryVisitor<T> visitor) {
            return visitor.visit(this);
        }

        public PatternMatcher getPattern() {
            return pattern;
        }

        @Override
        public DefinitionReference getReference() {
            return pattern.getReference();
        }

        @Override
        public Scope getScope() {
            return scope;
        }

        @Override
        public DefinitionEntry withDefinition(Definition definition) {
            throw new IllegalStateException();
        }
    }

    public static class ScopedEntry extends DefinitionEntry {

        private final Scope      scope;
        private final Definition definition;

        private ScopedEntry(Definition definition, Scope scope) {
            this.definition = definition;
            this.scope = scope;
        }

        @Override
        public <T> T accept(DefinitionEntryVisitor<T> visitor) {
            return visitor.visit(this);
        }

        public Definition getDefinition() {
            return definition;
        }

        @Override
        public DefinitionReference getReference() {
            return definition.getReference();
        }

        @Override
        public Scope getScope() {
            return scope;
        }

        @Override
        public DefinitionEntry withDefinition(Definition definition) {
            return new ScopedEntry(definition, scope);
        }
    }

    public static class UnscopedEntry extends DefinitionEntry {

        private final Definition definition;

        private UnscopedEntry(Definition definition) {
            this.definition = definition;
        }

        @Override
        public <T> T accept(DefinitionEntryVisitor<T> visitor) {
            return visitor.visit(this);
        }

        public Definition getDefinition() {
            return definition;
        }

        @Override
        public DefinitionReference getReference() {
            return definition.getReference();
        }

        @Override
        public Scope getScope() {
            throw new IllegalStateException();
        }

        @Override
        public DefinitionEntry withDefinition(Definition definition) {
            return new UnscopedEntry(definition);
        }
    }
}
