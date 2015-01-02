package scotch.compiler.syntax;

import static java.util.Spliterators.spliterator;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionReference.ValueReference;

public class DefinitionGraph {

    public static DefinitionGraphBuilder createGraph(Collection<DefinitionEntry> entries) {
        return new DefinitionGraphBuilder(entries);
    }

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final SymbolGenerator                           symbolGenerator;
    private final List<SyntaxError>                         errors;

    private DefinitionGraph(Collection<DefinitionEntry> entries, SymbolGenerator symbolGenerator, List<SyntaxError> errors) {
        this.symbolGenerator = symbolGenerator;
        this.errors = ImmutableList.copyOf(errors);
        this.definitions = new LinkedHashMap<>();
        entries.forEach(entry -> definitions.put(entry.getReference(), entry));
    }

    public DefinitionGraphBuilder copyWith(Collection<DefinitionEntry> entries) {
        return createGraph(entries)
            .withErrors(errors)
            .withSequence(symbolGenerator);
    }

    public Optional<ValueDefinition> getDefinition(ValueReference reference) {
        return getDefinition((DefinitionReference) reference).map(definition -> (ValueDefinition) definition);
    }

    public Optional<Definition> getDefinition(DefinitionReference reference) {
        return Optional.ofNullable(definitions.get(reference)).map(DefinitionEntry::getDefinition);
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public Scope getScope(DefinitionReference reference) {
        return tryGetScope(reference).orElseThrow(() -> new IllegalArgumentException("No scope found for reference: " + reference));
    }

    public List<DefinitionReference> getSortedReferences() {
        List<DefinitionReference> references = definitions.keySet().stream()
            .filter(reference -> !(reference instanceof ValueReference))
            .collect(toList());
        List<DefinitionReference> values = definitions.keySet().stream()
            .filter(reference -> reference instanceof ValueReference)
            .collect(toList());
        references.addAll(values);
        return references;
    }

    public SymbolGenerator getSymbolGenerator() {
        return symbolGenerator;
    }

    public Optional<Type> getValue(DefinitionReference reference) {
        return getDefinition(reference).map(definition -> definition.accept(new DefinitionVisitor<Type>() {
            @Override
            public Type visit(ValueDefinition definition) {
                return definition.getType();
            }

            @Override
            public Type visit(ValueSignature signature) {
                return signature.getType();
            }

            @Override
            public Type visitOtherwise(Definition definition) {
                throw new IllegalArgumentException("Can't get type of " + definition.getClass().getSimpleName());
            }
        }));
    }

    public List<ValueReference> getValues() {
        return definitions.keySet().stream()
            .filter(reference -> reference instanceof ValueReference)
            .map(reference -> (ValueReference) reference)
            .collect(toList());
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Stream<DefinitionEntry> stream() {
        return StreamSupport.stream(spliterator(definitions.values(), definitions.size()), false);
    }

    public Optional<Scope> tryGetScope(DefinitionReference reference) {
        return Optional.ofNullable(definitions.get(reference)).map(DefinitionEntry::getScope);
    }

    public static class DefinitionGraphBuilder {

        private final Collection<DefinitionEntry> definitions;
        private       Optional<SymbolGenerator>   optionalSequence;
        private       Optional<List<SyntaxError>> optionalErrors;

        private DefinitionGraphBuilder(Collection<DefinitionEntry> definitions) {
            this.definitions = definitions;
            this.optionalSequence = Optional.empty();
            this.optionalErrors = Optional.empty();
        }

        public DefinitionGraphBuilder appendErrors(List<SyntaxError> errors) {
            optionalErrors = optionalErrors.map(e -> ImmutableList.<SyntaxError>builder().addAll(e).addAll(errors).build());
            optionalErrors = Optional.of(optionalErrors.orElseGet(() -> ImmutableList.copyOf(errors)));
            return this;
        }

        public DefinitionGraph build() {
            return new DefinitionGraph(definitions, optionalSequence.orElseGet(SymbolGenerator::new), optionalErrors.orElse(ImmutableList.of()));
        }

        public DefinitionGraphBuilder withErrors(List<SyntaxError> errors) {
            optionalErrors = Optional.of(errors);
            return this;
        }

        public DefinitionGraphBuilder withSequence(SymbolGenerator symbolGenerator) {
            optionalSequence = Optional.of(symbolGenerator);
            return this;
        }
    }
}
