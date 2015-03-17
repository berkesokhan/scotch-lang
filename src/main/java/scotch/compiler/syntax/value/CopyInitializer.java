package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;
import scotch.runtime.Copyable;
import scotch.runtime.SuppliedThunk;

@EqualsAndHashCode(callSuper = false)
@ToString
public class CopyInitializer extends Value {

    private final SourceRange            sourceRange;
    private final Value                  value;
    private final List<InitializerField> fields;

    public CopyInitializer(SourceRange sourceRange, Value value, List<InitializerField> fields) {
        this.sourceRange = sourceRange;
        this.value = value;
        this.fields = fields;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return new CopyInitializer(sourceRange, value.bindTypes(state), fields.stream()
            .map(field -> field.bindTypes(state))
            .collect(toList()));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return new CopyInitializer(sourceRange, value.bindTypes(state), fields.stream()
            .map(field -> field.bindMethods(state))
            .collect(toList()));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            newobj(p(SuppliedThunk.class));
            dup();
            append(state.captureApply());
            lambda(state.currentClass(), new LambdaBlock(state.reserveApply()) {{
                function(p(Supplier.class), "get", sig(Object.class));
                specialize(sig(Callable.class));
                capture(state.getCaptureAllTypes());
                Class<?> returnType = state.typeOf(value.getType());
                delegateTo(ACC_STATIC, sig(returnType, state.getCaptureAllTypes()), new CodeBlock() {{
                    if (returnType != Callable.class) {
                        checkcast(p(returnType));
                    }
                    append(value.generateBytecode(state));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    checkcast(p(Copyable.class));
                    newobj(p(HashMap.class));
                    dup();
                    invokespecial(p(HashMap.class), "<init>", sig(void.class));
                    fields.forEach(field -> {
                        dup();
                        ldc(field.getName());
                        append(field.getValue().generateBytecode(state));
                        invokeinterface(p(Map.class), "put", sig(Object.class, Object.class, Object.class));
                        pop();
                    });
                    invokeinterface(p(Copyable.class), "copy", sig(Copyable.class, Map.class));
                    invokestatic(p(Callable.class), "box", sig(Callable.class, Object.class));
                    areturn();
                }});
            }});
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return value.getType();
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value withType(Type type) {
        throw new UnsupportedOperationException();
    }
}
