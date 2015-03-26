package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.symbol.Symbol.symbol;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Callable;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class TupleField {

    private final SourceLocation   sourceLocation;
    private final Optional<String> argument;
    private final Optional<String> field;
    private final Type             type;
    private final PatternMatch     patternMatch;

    public TupleField accumulateNames(NameAccumulator state) {
        state.defineValue(getSymbol(), type);
        state.specialize(type);
        return withPatternMatch(patternMatch.accumulateNames(state));
    }

    public TupleField bind(String argument, int ordinal, Scope scope) {
        String field = "_" + ordinal;
        return new TupleField(sourceLocation, Optional.of(argument), Optional.of(field), type, patternMatch.bind(argument + "#" + field, scope));
    }

    public TupleField bindMethods(TypeChecker state) {
        return new TupleField(sourceLocation, argument, field, type, patternMatch.bindMethods(state));
    }

    public TupleField bindTypes(TypeChecker state) {
        return new TupleField(sourceLocation, argument, field, state.generate(type), patternMatch.bindTypes(state));
    }

    public TupleField checkTypes(TypeChecker state) {
        state.addLocal(getSymbol());
        PatternMatch checkedMatch = patternMatch.checkTypes(state);
        return new TupleField(sourceLocation, argument, field, checkedMatch.getType(), checkedMatch);
    }

    public CodeBlock generateBytecode(String className, BytecodeGenerator state) {
        return new CodeBlock() {{
            invokevirtual(className, "get" + field.get(), sig(Callable.class));
            astore(state.getVariable(getSymbol().getCanonicalName()));
            append(patternMatch.generateBytecode(state));
        }};
    }

    public Type getType() {
        return type;
    }

    private Symbol getSymbol() {
        return symbol(argument.get() + "#" + field.get());
    }

    private TupleField withPatternMatch(PatternMatch patternMatch) {
        return new TupleField(sourceLocation, argument, field, type, patternMatch);
    }
}
