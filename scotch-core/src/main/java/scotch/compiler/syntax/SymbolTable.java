package scotch.compiler.syntax;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.syntax.DefinitionEntry.ScopedEntry;
import scotch.compiler.syntax.DefinitionEntry.UnscopedEntry;

public class SymbolTable {

    public static SymbolTableBuilder symbols(Collection<DefinitionEntry> entries) {
        return new SymbolTableBuilder(entries);
    }

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final int                                       sequence;
    private final List<SyntaxError>                         errors;

    private SymbolTable(Collection<DefinitionEntry> entries, int sequence, List<SyntaxError> errors) {
        this.sequence = sequence;
        this.errors = ImmutableList.copyOf(errors);
        ImmutableMap.Builder<DefinitionReference, DefinitionEntry> builder = ImmutableMap.builder();
        entries.forEach(entry -> builder.put(entry.getReference(), entry));
        this.definitions = builder.build();
    }

    public SymbolTableBuilder copyWith(Collection<DefinitionEntry> entries) {
        return symbols(entries)
            .withErrors(errors)
            .withSequence(sequence);
    }

    public Definition getDefinition(DefinitionReference reference) {
        return definitions.get(reference).accept(new DefinitionEntryVisitor<Definition>() {
            @Override
            public Definition visit(ScopedEntry entry) {
                return entry.getDefinition();
            }

            @Override
            public Definition visit(UnscopedEntry entry) {
                return entry.getDefinition();
            }
        });
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public Scope getScope(DefinitionReference reference) {
        return definitions.get(reference).getScope();
    }

    public int getSequence() {
        return sequence;
    }

    public Type getValue(DefinitionReference reference) {
        return getDefinition(reference).accept(new DefinitionVisitor<Type>() {
            @Override
            public Type visit(ValueDefinition definition) {
                return definition.getType();
            }

            @Override
            public Type visitOtherwise(Definition definition) {
                throw new IllegalArgumentException("Can't get type of " + definition.getClass().getSimpleName());
            }
        });
    }

    public static class SymbolTableBuilder {

        private final Collection<DefinitionEntry> definitions;
        private       Optional<Integer>           optionalSequence;
        private       Optional<List<SyntaxError>> optionalErrors;

        private SymbolTableBuilder(Collection<DefinitionEntry> definitions) {
            this.definitions = definitions;
            this.optionalSequence = Optional.empty();
            this.optionalErrors = Optional.empty();
        }

        public SymbolTable build() {
            return new SymbolTable(definitions, optionalSequence.orElse(0), optionalErrors.orElse(emptyList()));
        }

        public SymbolTableBuilder withErrors(List<SyntaxError> errors) {
            optionalErrors = Optional.of(errors);
            return this;
        }

        public SymbolTableBuilder withSequence(int sequence) {
            optionalSequence = Optional.of(sequence);
            return this;
        }
    }
}
