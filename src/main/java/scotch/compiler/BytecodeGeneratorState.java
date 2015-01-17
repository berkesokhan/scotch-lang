package scotch.compiler;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.JDKVersion.V1_8;
import static me.qmx.jitescript.util.CodegenUtils.c;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

public class BytecodeGeneratorState implements BytecodeGenerator {

    private final DefinitionGraph      graph;
    private final Deque<JiteClass>     jiteClasses;
    private final List<GeneratedClass> generatedClasses;
    private final Deque<Scope>         scopes;
    private final Deque<List<String>>  arguments;
    private final Deque<List<String>>  matches;
    private       int                  lambdas;
    private       int                  applies;

    public BytecodeGeneratorState(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableList.of()));
        this.matches = new ArrayDeque<>(asList(ImmutableList.of()));
    }

    @Override
    public void addMatch(String name) {
        matches.peek().add(name);
    }

    @Override
    public void beginClass(String className, SourceRange sourceRange) {
        jiteClasses.push(new JiteClass(className));
        currentClass().setSourceFile(sourceRange.getSourceName());
    }

    @Override
    public void beginMatches() {
        matches.push(new ArrayList<>());
    }

    @Override
    public CodeBlock captureApply() {
        List<String> variables = ImmutableList.<String>builder()
            .addAll(getCaptures())
            .addAll(getLocals())
            .addAll(getArguments())
            .addAll(getMatches())
            .build();
        return variables.stream()
            .map(variables::indexOf)
            .map(new CodeBlock()::aload)
            .reduce(new CodeBlock(), CodeBlock::append);
    }

    @Override
    public CodeBlock captureLambda(String lambdaArgument) {
        List<String> variables = ImmutableList.<String>builder()
            .addAll(getCaptures())
            .addAll(getArguments())
            .addAll(getMatches())
            .build();
        CodeBlock block = variables.stream()
            .map(this::getVariable)
            .map(new CodeBlock()::aload)
            .reduce(new CodeBlock(), CodeBlock::append);
        getArguments().add(lambdaArgument);
        return block;
    }

    @Override
    public JiteClass currentClass() {
        return jiteClasses.peek();
    }

    @Override
    public void defineDefaultConstructor(int access) {
        currentClass().defineDefaultConstructor(access);
    }

    @Override
    public CodeBlock enclose(Scoped scoped, Supplier<CodeBlock> supplier) {
        return scoped(scoped, () -> {
            arguments.push(new ArrayList<>());
            try {
                return supplier.get();
            } finally {
                arguments.pop();
            }
        });
    }

    @Override
    public void endClass() {
        JiteClass jiteClass = jiteClasses.pop();
        generatedClasses.add(new GeneratedClass(c(jiteClass.getClassName()), jiteClass.toBytes(V1_8)));
    }

    @Override
    public void endMatches() {
        matches.pop();
    }

    @Override
    public void fromRoot() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        generate(root, () -> root.generateBytecode(this));
    }

    @Override
    public <T extends Scoped> void generate(T scoped, Runnable runnable) {
        enterScope(scoped);
        try {
            runnable.run();
        } finally {
            leaveScope();
        }
    }

    @Override
    public Class<?>[] getCaptureAllTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getLocals()))
            .addAll(getCaptureTypes(getArguments()))
            .addAll(getCaptureTypes(getMatches()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    @Override
    public List<GeneratedClass> getClasses() {
        return ImmutableList.copyOf(generatedClasses);
    }

    @Override
    public Class<?>[] getLambdaCaptureTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments().subList(0, getArguments().size() - 1)))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    @Override
    public Class<?>[] getLambdaType() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    @Override
    public int getVariable(String name) {
        return ImmutableList.<String>builder()
            .addAll(getCaptures())
            .addAll(getLocals())
            .addAll(getArguments())
            .addAll(getMatches())
            .build()
            .indexOf(name);
    }

    @Override
    public void map(List<DefinitionReference> references) {
        references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(definition -> definition.generateBytecode(this));
    }

    @Override
    public void method(String methodName, int access, String signature, CodeBlock body) {
        currentClass().defineMethod(methodName, access, signature, body);
    }

    @Override
    public void releaseLambda(String lambdaArgument) {
        getArguments().remove(lambdaArgument);
    }

    @Override
    public String reserveApply() {
        return "apply$" + applies++;
    }

    @Override
    public String reserveLambda() {
        return "lambda$" + lambdas++;
    }

    @Override
    public Scope scope() {
        return scopes.peek();
    }

    @Override
    public <T extends Scoped> CodeBlock scoped(T scoped, Supplier<CodeBlock> supplier) {
        enterScope(scoped);
        try {
            return supplier.get();
        } finally {
            leaveScope();
        }
    }

    @Override
    public Class<? extends Callable> typeOf(Type type) {
        return type instanceof FunctionType ? Applicable.class : Callable.class;
    }

    private <T extends Scoped> void enterScope(T scoped) {
        scopes.push(graph.getScope(scoped.getReference()));
    }

    private List<String> getArguments() {
        return arguments.peek();
    }

    private List<Class<? extends Callable>> getCaptureTypes(List<String> captures) {
        return captures.stream()
            .map(Symbol::unqualified)
            .map(scope()::getValue)
            .map(this::typeOf)
            .collect(toList());
    }

    private List<String> getCaptures() {
        return scope().getCaptures();
    }

    private Optional<Definition> getDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference);
    }

    private List<String> getLocals() {
        return scope().getLocals();
    }

    private List<String> getMatches() {
        return matches.peek();
    }

    private void leaveScope() {
        scopes.pop();
    }
}
