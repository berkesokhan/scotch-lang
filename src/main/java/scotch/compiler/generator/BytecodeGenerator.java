package scotch.compiler.generator;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.LambdaBlock.METAFACTORY;
import static me.qmx.jitescript.util.CodegenUtils.c;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Type.getMethodType;
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
import me.qmx.jitescript.MethodDefinition;
import org.objectweb.asm.Handle;
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
import scotch.compiler.syntax.Value.BoundMethod;
import scotch.compiler.syntax.Value.BoundValue;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.IntLiteral;
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
    private       int                  sequence;

    public BytecodeGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableList.of()));
    }

    public List<GeneratedClass> generate() {
        graph.getDefinition(rootRef()).map(definition -> definition.accept(this));
        return ImmutableList.copyOf(generatedClasses);
    }

    @Override
    public CodeBlock visit(Apply apply) {
        Class<?>[] args = new Class<?>[arguments.peek().size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = Callable.class;
        }
        String methodName = "thunk$" + sequence++;
        String signature = sig(Callable.class, args);
        method(methodName, ACC_STATIC | ACC_PRIVATE, signature, new CodeBlock()
            .append(generate(apply.getFunction()))
            .invokeinterface(p(Callable.class), "call", sig(Object.class))
            .checkcast(p(Applicable.class))
            .append(generate(apply.getArgument()))
            .invokeinterface(p(Applicable.class), "apply", sig(Callable.class, Callable.class))
            .areturn());
        return new CodeBlock() {{
            newobj(p(SuppliedThunk.class));
            dup();
            for (int i = 0; i < args.length; i++) {
                aload(i);
            }
            invokedynamic("get", sig(Supplier.class), METAFACTORY,
                getMethodType(sig(Object.class)),
                new Handle(H_INVOKESTATIC, currentClass().getClassName(), methodName, signature),
                getMethodType(signature)
            );
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
    }

    @Override
    public CodeBlock visit(Argument argument) {
        if (arguments.peek().contains(argument.getName())) {
            return new CodeBlock().aload(arguments.peek().indexOf(argument.getName()));
        } else {
            throw new UnsupportedOperationException(); // TODO
        }
    }

    @Override
    public CodeBlock visit(BoundMethod boundMethod) {
        return boundMethod.reference(currentScope());
    }

    @Override
    public CodeBlock visit(BoundValue boundValue) {
        return boundValue.reference();
    }

    @Override
    public CodeBlock visit(FunctionValue function) {
        arguments.push(ImmutableList.<String>builder()
                .addAll(arguments.peek())
                .addAll(function.getArguments().stream().map(Argument::getName).collect(toList()))
                .build()
        );
        String methodName = "function$" + sequence++;
        String signature = sig(
            function.getBody().getType() instanceof FunctionType ? Applicable.class : Callable.class,
            function.getArguments().get(0).getType() instanceof FunctionType ? Applicable.class : Callable.class
        );
        try {
            method(methodName, ACC_STATIC | ACC_PRIVATE, signature, generate(function.getBody()).areturn());
        } finally {
            arguments.pop();
        }
        return new CodeBlock().invokedynamic("apply", sig(Applicable.class), METAFACTORY,
            getMethodType(sig(Callable.class, Callable.class)),
            new Handle(H_INVOKESTATIC, currentClass().getClassName(), methodName, signature),
            getMethodType(signature)
        );
    }

    @Override
    public CodeBlock visit(IntLiteral literal) {
        return new CodeBlock()
            .ldc(literal.getValue())
            .invokestatic(p(Callable.class), "box", sig(Callable.class, int.class));
    }

    @Override
    public Void visit(RootDefinition definition) {
        scoped(definition, () -> generateAll(definition.getDefinitions()));
        return null;
    }

    @Override
    public CodeBlock visit(StringLiteral literal) {
        return new CodeBlock().ldc(literal.getValue());
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

    private void beginClass(String className, SourceRange sourceRange) {
        jiteClasses.push(new JiteClass(className));
        currentClass().setSourceFile(sourceRange.getSourceName());
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
