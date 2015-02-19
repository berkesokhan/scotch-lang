package scotch.compiler.syntax.definition;

import static java.util.Spliterators.spliterator;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;

public class DefinitionGraph {

    public static DefinitionGraphBuilder createGraph(Collection<DefinitionEntry> entries) {
        return new DefinitionGraphBuilder(entries);
    }

    public static SyntaxError cyclicDependency(DependencyCycle cycle) {
        return new CyclicDependencyError(cycle);
    }

    private static DependencyCycle fromNodes(Collection<DefinitionNode> nodes) {
        DependencyCycle.Builder builder = DependencyCycle.builder();
        nodes.forEach(builder::addNode);
        return builder.build();
    }

    public final  Map<DefinitionReference, DefinitionEntry> definitions;
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

    public Optional<Type> getValue(DefinitionReference reference) {
        return getDefinition(reference).map(definition -> definition.asValue()
            .map(ValueDefinition::getType)
            .orElseGet(def1 -> definition.asSignature()
                .map(ValueSignature::getType)
                .orElseThrow(def2 -> new IllegalArgumentException("Can't get type of " + definition.getClass().getSimpleName()))));
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

    public DefinitionGraph sort() {
        List<SyntaxError> errors = new ArrayList<>();
        return copyWith(sort_(errors))
            .appendErrors(errors)
            .build();
    }

    public Stream<DefinitionEntry> stream() {
        return StreamSupport.stream(spliterator(definitions.values(), definitions.size()), false);
    }

    public Optional<Scope> tryGetScope(DefinitionReference reference) {
        return Optional.ofNullable(definitions.get(reference)).map(DefinitionEntry::getScope);
    }

    private List<DefinitionEntry> sortValues(List<SyntaxError> errors, List<DefinitionNode> input) {
        List<DefinitionNode> roots = new ArrayList<>();
        List<DefinitionNode> nodes = new ArrayList<>();
        List<DefinitionEntry> output = new ArrayList<>();
        input.forEach(node -> {
            if (node.hasDependencies()) {
                nodes.add(node);
            } else {
                roots.add(node);
            }
        });
        while (!roots.isEmpty()) {
            DefinitionNode root = roots.remove(0);
            output.add(root.getEntry());
            Iterator<DefinitionNode> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                DefinitionNode node = iterator.next();
                if (node.isDependentOn(root)) {
                    node.removeDependency(root);
                    if (!node.hasDependencies()) {
                        roots.add(node);
                        iterator.remove();
                    }
                }
            }
        }
        if (nodes.isEmpty()) {
            return output;
        } else {
            errors.add(cyclicDependency(fromNodes(nodes)));
            output.addAll(nodes.stream().map(DefinitionNode::getEntry).collect(toList()));
            return output;
        }
    }

    private List<DefinitionEntry> sort_(List<SyntaxError> errors) {
        List<DefinitionEntry> entries = stream()
            .filter(entry -> !(entry.getReference() instanceof ValueReference))
            .collect(toList());
        List<DefinitionNode> values = stream()
            .filter(entry -> entry.getReference() instanceof ValueReference)
            .map(DefinitionNode::new)
            .collect(toList());
        entries.addAll(sortValues(errors, values));
        return entries;
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class CyclicDependencyError extends SyntaxError {

        private final DependencyCycle cycle;

        @Override
        public String prettyPrint() {
            return cycle.prettyPrint();
        }
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
