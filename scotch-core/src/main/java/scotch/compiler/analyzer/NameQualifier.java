package scotch.compiler.analyzer;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.Deque;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ModuleDefinition;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.Definition.RootDefinition;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.Definition.ValueSignature;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.ast.Value.Apply;
import scotch.compiler.ast.Value.Identifier;
import scotch.compiler.ast.Value.LiteralValue;
import scotch.compiler.ast.Value.PatternMatchers;
import scotch.compiler.ast.Value.ValueVisitor;
import scotch.lang.Type;
import scotch.lang.Type.FunctionType;
import scotch.lang.Type.TypeVisitor;
import scotch.lang.Type.UnionLookup;
import scotch.lang.Type.VariableType;

public class NameQualifier implements DefinitionVisitor<Definition>, ValueVisitor<Value>, TypeVisitor<Type> {

    private final SymbolResolver             resolver;
    private final SymbolTable                symbols;
    private final Deque<DefinitionReference> references;

    public NameQualifier(SymbolResolver resolver, SymbolTable symbols) {
        this.resolver = resolver;
        this.symbols = symbols;
        this.references = new ArrayDeque<>();
    }

    public SymbolTable analyze() {
        analyzeReference(rootRef());
        return symbols;
    }

    public void analyzeReference(DefinitionReference reference) {
        references.push(reference);
        try {
            symbols.setDefinition(reference, symbols.getDefinition(reference).accept(this));
        } finally {
            references.pop();
        }
    }

    public DefinitionReference getReference() {
        return references.peek();
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(analyze(apply.getFunction()))
            .withArgument(analyze(apply.getArgument()));
    }

    @Override
    public Type visit(FunctionType functionType) {
        return functionType
            .withArgument(analyze(functionType.getArgument()))
            .withResult(analyze(functionType.getResult()));
    }

    @Override
    public Value visit(Identifier identifier) {
        return identifier.withName(qualify(identifier.getName()));
    }

    @Override
    public Value visit(LiteralValue value) {
        return value;
    }

    @Override
    public Type visit(UnionLookup lookup) {
        return lookup
            .withName(qualify(lookup.getName()))
            .withArguments(lookup.getArguments().stream()
                    .map(this::analyze)
                    .collect(toList())
            );
    }

    @Override
    public Type visit(VariableType variableType) {
        return variableType;
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        definition.getDefinitions().forEach(this::analyzeReference);
        return definition;
    }

    @Override
    public Definition visit(RootDefinition definition) {
        definition.getDefinitions().forEach(this::analyzeReference);
        return definition;
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        return definition
            .withName(getReference().qualify())
            .withType(analyze(definition.getType()))
            .withBody(analyze(definition.getBody()));
    }

    @Override
    public Definition visit(OperatorDefinition definition) {
        return definition.withName(getReference().qualify());
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(matchers.getMatchers().stream()
                .map(matcher -> matcher.withBody(matcher.getBody().accept(this)))
                .collect(toList())
        );
    }

    @Override
    public Definition visit(ValueSignature signature) {
        return signature
            .withName(getReference().qualify())
            .withType(analyze(signature.getType()));
    }

    private Value analyze(Value value) {
        return value.accept(this);
    }

    private Type analyze(Type type) {
        return type.accept(this);
    }

    private Scope getScope() {
        return symbols.getScope(getReference());
    }

    private String qualify(String name) {
        return getScope().qualify(name, resolver);
    }
}
