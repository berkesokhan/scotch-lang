package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.JDKVersion.V1_8;
import static me.qmx.jitescript.util.CodegenUtils.c;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.output.GeneratedClass.ClassType.DATA_CONSTRUCTOR;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.error.CompileException;
import scotch.compiler.output.GeneratedClass;
import scotch.compiler.output.GeneratedClass.ClassType;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.descriptor.TypeInstanceDescriptor;
import scotch.compiler.symbol.type.FunctionType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.ModuleReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.Copyable;

public class BytecodeGenerator {

    private final DefinitionGraph                   graph;
    private final Deque<Pair<JiteClass, ClassType>> jiteClasses;
    private final List<GeneratedClass>              generatedClasses;
    private final Deque<Scope>                      scopes;
    private final Deque<List<String>>               arguments;
    private final Deque<List<String>>               matches;
    private final Deque<CaseEntry>                  cases;
    private       int                               lambdas;
    private       int                               applies;

    public BytecodeGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableList.of()));
        this.matches = new ArrayDeque<>(asList(ImmutableList.of()));
        this.cases = new ArrayDeque<>();
    }

    public void addMatch(String name) {
        matches.peek().add(name);
    }

    public LabelNode beginCase() {
        return cases.peek().beginCase();
    }

    public void beginCases(int size) {
        cases.push(new CaseEntry(size));
    }

    public void beginClass(ClassType classType, String className, SourceRange sourceRange) {
        jiteClasses.push(pair(new JiteClass(className), classType));
        currentClass().setSourceFile(sourceRange.getPath());
    }

    public void beginClass(ClassType classType, String className, String superClass, SourceRange sourceRange) {
        jiteClasses.push(pair(new JiteClass(className, superClass, new String[0]), classType));
        currentClass().setSourceFile(sourceRange.getPath());
    }

    public void beginConstant(String className, SourceRange sourceRange) {
        jiteClasses.push(pair(new JiteClass(className, currentClass().getClassName(), new String[0]), DATA_CONSTRUCTOR));
        currentClass().setSourceFile(sourceRange.getPath());
    }

    public void beginConstructor(String className, SourceRange sourceRange) {
        jiteClasses.push(pair(new JiteClass(className, currentClass().getClassName(), new String[] { p(Copyable.class) }), DATA_CONSTRUCTOR));
        currentClass().setSourceFile(sourceRange.getPath());
    }

    public void beginMatches() {
        matches.push(new ArrayList<>());
    }

    public CodeBlock captureApply() {
        List<String> variables = getAllVariables();
        return variables.stream()
            .map(variables::indexOf)
            .map(new CodeBlock()::aload)
            .reduce(new CodeBlock(), CodeBlock::append);
    }

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

    public JiteClass currentClass() {
        return jiteClasses.peek().getLeft();
    }

    public void defineDefaultConstructor(int access) {
        currentClass().defineDefaultConstructor(access);
    }

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

    public LabelNode endCase() {
        return cases.peek().endCase();
    }

    public LabelNode endCases() {
        return cases.pop().endCase();
    }

    public void endClass() {
        jiteClasses.pop().into((jiteClass, type) ->
            generatedClasses.add(new GeneratedClass(type, c(jiteClass.getClassName()), jiteClass.toBytes(V1_8))));
    }

    public void endMatches() {
        matches.pop();
    }

    public void field(String fieldName, int access, String type) {
        currentClass().defineField(fieldName, access, type, null);
    }

    public void fromRoot() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        generate(root, () -> root.generateBytecode(this));
    }

    public <T extends Scoped> void generate(T scoped, Runnable runnable) {
        enterScope(scoped);
        try {
            runnable.run();
        } finally {
            leaveScope();
        }
    }

    public List<GeneratedClass> generateBytecode() {
        if (graph.hasErrors()) {
            throw new CompileException(graph.getErrors());
        } else {
            BytecodeGenerator state = new BytecodeGenerator(graph);
            state.fromRoot();
            return state.getClasses();
        }
    }

    public void generateBytecode(List<DefinitionReference> references) {
        references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(definition -> definition.generateBytecode(this));
    }

    public Class<?>[] getCaptureAllTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getLocals()))
            .addAll(getCaptureTypes(getArguments()))
            .addAll(getCaptureTypes(getMatches()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    public List<GeneratedClass> getClasses() {
        return generatedClasses.stream()
            .sorted()
            .collect(toList());
    }

    public Class<?>[] getLambdaCaptureTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments().subList(0, getArguments().size() - 1)))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    public Class<?>[] getLambdaType() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    public int getVariable(String name) {
        return getAllVariables()
            .indexOf(name);
    }

    public void method(String methodName, int access, String signature, CodeBlock body) {
        currentClass().defineMethod(methodName, access, signature, body);
    }

    public LabelNode nextCase() {
        return cases.peek().nextCase();
    }

    public void releaseLambda(String lambdaArgument) {
        getArguments().remove(lambdaArgument);
    }

    public String reserveApply() {
        return "apply$" + applies++;
    }

    public String reserveLambda() {
        return "lambda$" + lambdas++;
    }

    public Scope scope() {
        return scopes.peek();
    }

    public <T extends Scoped> CodeBlock scoped(T scoped, Supplier<CodeBlock> supplier) {
        enterScope(scoped);
        try {
            return supplier.get();
        } finally {
            leaveScope();
        }
    }

    public Class<? extends Callable> typeOf(Type type) {
        return type instanceof FunctionType ? Applicable.class : Callable.class;
    }

    private <T extends Scoped> void enterScope(T scoped) {
        scopes.push(graph.getScope(scoped.getReference()));
    }

    private ImmutableList<String> getAllVariables() {
        return ImmutableList.<String>builder()
            .addAll(getCaptures())
            .addAll(getLocals())
            .addAll(getArguments())
            .addAll(getMatches())
            .build();
    }

    private List<String> getArguments() {
        return arguments.peek();
    }

    private List<Class<? extends Callable>> getCaptureTypes(List<String> captures) {
        return captures.stream()
            .map(Symbol::unqualified)
            .map(scope()::getValue)
            .map(value -> value.map(this::typeOf))
            .filter(Optional::isPresent)
            .map(Optional::get)
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

    public String getDataConstructorClass(Symbol symbol) {
        return scope().getDataConstructorClass(symbol);
    }

    public TypeInstanceDescriptor getTypeInstance(ClassReference classRef, ModuleReference moduleRef, List<Type> parameters) {
        return scope().getTypeInstance(classRef, moduleRef, parameters).get();
    }

    public MethodSignature getValueSignature(Symbol symbol) {
        return scope()
            .getValueSignature(symbol)
            .orElseThrow(() -> new IllegalStateException("Could not get value method for " + symbol));
    }

    private static class CaseEntry {

        private final List<LabelNode> labels;
        private       int             position;

        public CaseEntry(int size) {
            labels = new ArrayList<>();
            for (int i = 0; i <= size; i++) {
                labels.add(new LabelNode());
            }
        }

        public LabelNode beginCase() {
            return labels.get(position++);
        }

        public LabelNode endCase() {
            return labels.get(labels.size() - 1);
        }

        public LabelNode nextCase() {
            return labels.get(position);
        }
    }
}
