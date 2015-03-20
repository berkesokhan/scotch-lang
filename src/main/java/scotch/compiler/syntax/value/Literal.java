package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.literal;

import java.util.Optional;
import java.util.function.Function;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;

public class Literal implements SyntaxBuilder<Value> {

    public static Literal builder() {
        return new Literal();
    }

    private Optional<Function<SourceLocation, Value>> constructor;
    private Optional<SourceLocation>                  sourceLocation;

    private Literal() {
        constructor = Optional.empty();
        sourceLocation = Optional.empty();
    }

    @Override
    public Value build() {
        return require(constructor, "Literal value").apply(require(sourceLocation, "Source location"));
    }

    @Override
    public SyntaxBuilder<Value> withSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = Optional.of(sourceLocation);
        return this;
    }

    public Literal withValue(char value) {
        constructor = Optional.of(sourceLocation -> literal(sourceLocation, value));
        return this;
    }

    public Literal withValue(double value) {
        constructor = Optional.of(sourceLocation -> literal(sourceLocation, value));
        return this;
    }

    public Literal withValue(int value) {
        constructor = Optional.of(sourceLocation -> literal(sourceLocation, value));
        return this;
    }

    public Literal withValue(String value) {
        constructor = Optional.of(sourceLocation -> literal(sourceLocation, value));
        return this;
    }

    public Literal withValue(boolean value) {
        constructor = Optional.of(sourceLocation -> literal(sourceLocation, value));
        return this;
    }
}
