package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Value.literal;

import java.util.Optional;
import java.util.function.Function;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

public class Literal implements SyntaxBuilder<Value> {

    public static Literal builder() {
        return new Literal();
    }

    private Optional<Function<SourceRange, Value>> constructor;
    private Optional<SourceRange>                  sourceRange;

    private Literal() {
        constructor = Optional.empty();
        sourceRange = Optional.empty();
    }

    @Override
    public Value build() {
        return require(constructor, "Literal value").apply(require(sourceRange, "Source range"));
    }

    @Override
    public SyntaxBuilder<Value> withSourceRange(SourceRange sourceRange) {
        this.sourceRange = Optional.of(sourceRange);
        return this;
    }

    public Literal withValue(char value) {
        constructor = Optional.of(sourceRange -> literal(sourceRange, value));
        return this;
    }

    public Literal withValue(double value) {
        constructor = Optional.of(sourceRange -> literal(sourceRange, value));
        return this;
    }

    public Literal withValue(int value) {
        constructor = Optional.of(sourceRange -> literal(sourceRange, value));
        return this;
    }

    public Literal withValue(String value) {
        constructor = Optional.of(sourceRange -> literal(sourceRange, value));
        return this;
    }

    public Literal withValue(boolean value) {
        constructor = Optional.of(sourceRange -> literal(sourceRange, value));
        return this;
    }
}
