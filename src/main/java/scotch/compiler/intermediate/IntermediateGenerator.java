package scotch.compiler.intermediate;

import static scotch.compiler.intermediate.Intermediates.value;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import scotch.compiler.error.CompileException;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;

public class IntermediateGenerator {

    private final DefinitionGraph              graph;
    private final List<IntermediateDefinition> definitions;
    private final Deque<Scope>                 scopes;

    public IntermediateGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
    }

    public void defineValue(DefinitionReference reference, IntermediateValue body) {
        definitions.add(value(reference, body));
    }

    public IntermediateGraph generateIntermediateCode() {
        if (graph.hasErrors()) {
            throw new CompileException(graph.getErrors());
        } else {
            graph.getDefinition(rootRef()).get().generateIntermediateCode(this);
            return new IntermediateGraph(definitions);
        }
    }

    public void generateIntermediateCode(DefinitionReference reference) {
        graph.getDefinition(reference).get().generateIntermediateCode(this);
    }

    public List<String> getCaptures() {
        return Collections.emptyList();
    }

    public <T extends Scoped> void scoped(T scoped, Runnable runnable) {
        scopes.push(graph.getScope(scoped.getReference()));
        try {
            runnable.run();
        } finally {
            scopes.pop();
        }
    }
}
