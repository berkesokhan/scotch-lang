package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static scotch.symbol.Symbol.symbol;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.type.Type;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class OrdinalField {

    private final SourceLocation   sourceLocation;
    private final Optional<String> argument;
    private final Optional<String> field;
    private final Type             type;
    private final PatternMatch     patternMatch;

    public OrdinalField accumulateNames(NameAccumulator state) {
        state.defineValue(symbol(argument.get() + "#" + field.get()), type);
        return withPatternMatch(patternMatch.accumulateNames(state));
    }

    public OrdinalField bindMethods(TypeChecker state) {
        return new OrdinalField(sourceLocation, argument, field, type, patternMatch.bindMethods(state));
    }

    public OrdinalField bindTypes(TypeChecker state) {
        return new OrdinalField(sourceLocation, argument, field, state.generate(type), patternMatch.bindTypes(state));
    }

    public OrdinalField checkTypes(TypeChecker state) {
        return new OrdinalField(sourceLocation, argument, field, type, patternMatch.checkTypes(state));
    }

    private OrdinalField withPatternMatch(PatternMatch patternMatch) {
        return new OrdinalField(sourceLocation, argument, field, type, patternMatch);
    }

    public OrdinalField bind(String argument, int ordinal, Scope scope) {
        String field = "_" + ordinal;
        return new OrdinalField(sourceLocation, Optional.of(argument), Optional.of(field), type, patternMatch.bind(argument + "#" + field, scope));
    }
}
