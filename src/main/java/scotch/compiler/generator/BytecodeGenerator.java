package scotch.compiler.generator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.JDKVersion.V1_8;
import static me.qmx.jitescript.util.CodegenUtils.c;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static scotch.compiler.syntax.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.CompileException;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoundValue;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Instance;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.LambdaValue;
import scotch.compiler.syntax.Value.Method;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

public class BytecodeGenerator implements
    DefinitionVisitor<Void>,
    ValueVisitor<CodeBlock>,
    PatternMatchVisitor<CodeBlock> {

    private final DefinitionGraph      graph;
    private final Deque<JiteClass>     jiteClasses;
    private final List<GeneratedClass> generatedClasses;
    private final Deque<Scope>         scopes;
    private final Deque<List<String>>  arguments;
    private final Deque<List<String>>  matches;
    private       int                  lambdas;
    private       int                  applies;

    public BytecodeGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableList.of()));
        this.matches = new ArrayDeque<>(asList(ImmutableList.of()));
    }

    public List<GeneratedClass> generate() {
        if (graph.hasErrors()) {
            throw new CompileException(graph.getErrors());
        }
        graph.getDefinition(rootRef()).map(definition -> definition.accept(this));
        return ImmutableList.copyOf(generatedClasses);
    }

    @Override
    public CodeBlock visit(Apply apply) {
        return new CodeBlock() {{
            newobj(p(SuppliedThunk.class));
            dup();
            append(captureApply());
            lambda(currentClass(), new LambdaBlock("apply$" + applies++) {{
                function(p(Supplier.class), "get", sig(Object.class));
                specialize(sig(Callable.class));
                capture(getCaptureAllTypes());
                Class<?> returnType = typeOf(apply.getType());
                delegateTo(ACC_STATIC, sig(returnType, getCaptureAllTypes()), new CodeBlock() {{
                    append(generate(apply.getFunction()));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    checkcast(p(Applicable.class));
                    append(generate(apply.getArgument()));
                    invokeinterface(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                    if (returnType != Callable.class) {
                        checkcast(p(returnType));
                    }
                    areturn();
                }});
            }});
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
    }

    @Override
    public CodeBlock visit(Argument argument) {
        return new CodeBlock() {{
            aload(getVariable(argument.getName()));
        }};
    }

    @Override
    public CodeBlock visit(BoundValue boundValue) {
        return boundValue.reference(currentScope());
    }

    @Override
    public CodeBlock visit(CaptureMatch match) {
        return new CodeBlock() {{
            getMatches().add(match.getName());
            aload(getVariable(match.getArgument()));
            astore(getVariable(match.getName()));
        }};
    }

    @Override
    public CodeBlock visit(DoubleLiteral literal) {
        return new CodeBlock() {{
            ldc(literal.getValue());
            invokestatic(p(Callable.class), "box", sig(Callable.class, double.class));
        }};
    }

    @Override
    public CodeBlock visit(FunctionValue function) {
        return scoped(function.getReference(), () -> {
            arguments.push(new ArrayList<>());
            CodeBlock result = generate(function.curry());
            arguments.pop();
            return result;
        });
    }

    @Override
    public CodeBlock visit(Instance instance) {
        return instance.reference(currentScope());
    }

    @Override
    public CodeBlock visit(IntLiteral literal) {
        return new CodeBlock() {{
            ldc(literal.getValue());
            invokestatic(p(Callable.class), "box", sig(Callable.class, int.class));
        }};
    }

    @Override
    public CodeBlock visit(LambdaValue lambda) {
        return new CodeBlock() {{
            append(captureLambda(lambda.getArgumentName()));
            lambda(currentClass(), new LambdaBlock("lambda$" + lambdas++) {{
                function(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                capture(getLambdaCaptureTypes());
                delegateTo(ACC_STATIC, sig(typeOf(lambda.getBody().getType()), getLambdaType()), new CodeBlock() {{
                    append(generate(lambda.getBody()));
                    areturn();
                }});
            }});
            releaseLambda(lambda.getArgumentName());
        }};
    }

    @Override
    public CodeBlock visit(Method method) {
        return method.reference(currentScope());
    }

    @Override
    public Void visit(ModuleDefinition definition) {
        return scoped(definition.getReference(), () -> {
            beginClass(definition.getClassName(), definition.getSourceRange());
            currentClass().defineDefaultConstructor(ACC_PRIVATE);
            generateAll(definition.getDefinitions());
            endClass();
            return null;
        });
    }

    @Override
    public CodeBlock visit(PatternMatchers matchers) {
        return new CodeBlock() {{
            matchers.getMatchers().forEach(matcher -> scoped(matcher.getReference(), () -> {
                matches.push(new ArrayList<>());
                matcher.getMatches().forEach(match -> append(generate(match)));
                append(generate(matcher.getBody()));
                matches.pop();
                return null;
            }));
        }};
    }

    @Override
    public Void visit(RootDefinition definition) {
        return scoped(definition.getReference(), () -> {
            generateAll(definition.getDefinitions());
            return null;
        });
    }

    @Override
    public CodeBlock visit(StringLiteral literal) {
        return new CodeBlock() {{
            ldc(literal.getValue());
            invokestatic(p(Callable.class), "box", sig(Callable.class, Object.class));
        }};
    }

    @Override
    public Void visit(ValueDefinition definition) {
        return scoped(definition.getReference(), () -> {
            String signature = definition.getType().accept(new TypeVisitor<String>() {
                @Override
                public String visit(FunctionType type) {
                    return sig(Applicable.class);
                }

                @Override
                public String visitOtherwise(Type type) {
                    return sig(Callable.class);
                }
            });
            method(definition.getMethodName(), ACC_STATIC | ACC_PUBLIC, signature, new CodeBlock() {{
                annotate(Value.class).value("memberName", definition.getSymbol().getMemberName());
                definition.markLine(this);
                append(generate(definition.getBody()));
                areturn();
            }});
            return null;
        });
    }

    private void beginClass(String className, SourceRange sourceRange) {
        jiteClasses.push(new JiteClass(className));
        currentClass().setSourceFile(sourceRange.getSourceName());
    }

    private CodeBlock captureApply() {
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

    private CodeBlock captureLambda(String lambdaArgument) {
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

    private List<String> getMatches() {
        return matches.peek();
    }

    private List<String> getArguments() {
        return arguments.peek();
    }

    private List<String> getCaptures() {
        return currentScope().getCaptures();
    }

    private JiteClass currentClass() {
        return jiteClasses.peek();
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private void endClass() {
        JiteClass jiteClass = jiteClasses.pop();
        generatedClasses.add(new GeneratedClass(c(jiteClass.getClassName()), jiteClass.toBytes(V1_8)));
    }

    private CodeBlock generate(Value value) {
        return value.accept(this);
    }

    private CodeBlock generate(PatternMatch match) {
        return match.accept(this);
    }

    private void generateAll(List<DefinitionReference> definitions) {
        definitions.stream()
            .map(this.graph::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(definition -> definition.accept(this));
    }

    private Class<?>[] getCaptureAllTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getLocals()))
            .addAll(getCaptureTypes(getArguments()))
            .addAll(getCaptureTypes(getMatches()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    private List<String> getLocals() {
        return currentScope().getLocals();
    }

    private List<Class<? extends Callable>> getCaptureTypes(List<String> captures) {
        return captures.stream()
            .map(Symbol::unqualified)
            .map(currentScope()::getValue)
            .map(this::typeOf)
            .collect(toList());
    }

    private Class<? extends Callable> typeOf(Type type) {
        return type instanceof FunctionType ? Applicable.class : Callable.class;
    }

    private Class<?>[] getLambdaCaptureTypes() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments().subList(0, getArguments().size() - 1)))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    private Class<?>[] getLambdaType() {
        List<Class<?>> types = ImmutableList.<Class<?>>builder()
            .addAll(getCaptureTypes(getCaptures()))
            .addAll(getCaptureTypes(getArguments()))
            .build();
        return types.toArray(new Class<?>[types.size()]);
    }

    private int getVariable(String name) {
        return ImmutableList.<String>builder()
            .addAll(getCaptures())
            .addAll(getLocals())
            .addAll(getArguments())
            .addAll(getMatches())
            .build()
            .indexOf(name);
    }

    private void method(String methodName, int modifiers, String signature, CodeBlock methodBody) {
        currentClass().defineMethod(methodName, modifiers, signature, methodBody);
    }

    private void releaseLambda(String lambdaArgument) {
        getArguments().remove(lambdaArgument);
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }
}
