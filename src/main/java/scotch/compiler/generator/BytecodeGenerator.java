package scotch.compiler.generator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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
import me.qmx.jitescript.MethodDefinition;
import scotch.compiler.CompileException;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
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
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

public class BytecodeGenerator implements DefinitionVisitor<Void>, ValueVisitor<CodeBlock> {

    private final DefinitionGraph      graph;
    private final Deque<JiteClass>     jiteClasses;
    private final List<GeneratedClass> generatedClasses;
    private final Deque<Scope>         scopes;
    private final Deque<List<String>>  arguments;

    public BytecodeGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableList.of()));
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
            currentArgs().forEach(arg -> aload(currentArgs().indexOf(arg)));
            lambda(currentClass(), new LambdaBlock() {{
                function(p(Supplier.class), "get", sig(Object.class));
                specialize(sig(Callable.class));
                capture(getArgumentTypes());
                delegateTo(ACC_STATIC, sig(Callable.class, getArgumentTypes()), new CodeBlock() {{
                    append(generate(apply.getFunction()));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    checkcast(p(Applicable.class));
                    append(generate(apply.getArgument()));
                    invokeinterface(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                    areturn();
                }});
            }});
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
    }

    @Override
    public CodeBlock visit(Argument argument) {
        return new CodeBlock() {{
            aload(arguments.peek().indexOf(argument.getName()));
        }};
    }

    @Override
    public CodeBlock visit(BoundValue boundValue) {
        return boundValue.reference(currentScope());
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
        return generate(function.curry()).areturn();
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
            currentArgs().forEach(arg -> aload(currentArgs().indexOf(arg)));
            lambda(currentClass(), new LambdaBlock() {{
                function(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                capture(getArgumentTypes());
                arguments.push(ImmutableList.<String>builder()
                    .addAll(currentArgs())
                    .add(lambda.getArgumentName())
                    .build());
                try {
                    delegateTo(ACC_STATIC, sig(Callable.class, getArgumentTypes()), new CodeBlock() {{
                        append(generate(lambda.getBody()));
                        areturn();
                    }});
                } finally {
                    arguments.pop();
                }
            }});
        }};
    }

    @Override
    public CodeBlock visit(Method method) {
        return method.reference(currentScope());
    }

    @Override
    public Void visit(ModuleDefinition definition) {
        scoped(definition, () -> {
            beginClass(definition.getClassName(), definition.getSourceRange());
            currentClass().defineDefaultConstructor(ACC_PRIVATE);
            generateAll(definition.getDefinitions());
            endClass();
        });
        return null;
    }

    @Override
    public Void visit(RootDefinition definition) {
        scoped(definition, () -> generateAll(definition.getDefinitions()));
        return null;
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
        scoped(definition, () -> {
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
        });
        return null;
    }

    private void beginClass(String className, SourceRange sourceRange) {
        jiteClasses.push(new JiteClass(className));
        currentClass().setSourceFile(sourceRange.getSourceName());
    }

    private List<String> currentArgs() {
        return arguments.peek();
    }

    private JiteClass currentClass() {
        return jiteClasses.peek();
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private void endClass() {
        JiteClass jiteClass = jiteClasses.pop();
        generatedClasses.add(new GeneratedClass(c(jiteClass.getClassName()), jiteClass.toBytes()));
    }

    private CodeBlock generate(Value value) {
        return value.accept(this);
    }

    private void generateAll(List<DefinitionReference> definitions) {
        definitions.stream()
            .map(this.graph::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(definition -> definition.accept(this));
    }

    private Class<?>[] getArgumentTypes() {
        return currentArgs().stream()
            .map(arg -> Callable.class)
            .collect(toList())
            .toArray(new Class<?>[currentArgs().size()]);
    }

    private void method(String methodName, int modifiers, String signature, CodeBlock methodBody) {
        currentClass().addMethod(new MethodDefinition(methodName, modifiers, signature, methodBody));
    }

    private void scoped(Definition definition, Runnable runnable) {
        scopes.push(graph.getScope(definition.getReference()));
        try {
            runnable.run();
        } finally {
            scopes.pop();
        }
    }
}
