package scotch.compiler.parser;

import static scotch.symbol.Symbol.qualified;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Initializer;
import scotch.compiler.syntax.value.InitializerField;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

public final class TupleBuilder implements SyntaxBuilder<Value> {

    public static TupleBuilder builder() {
        return new TupleBuilder();
    }

    private final List<Value>              members     = new ArrayList<>();
    private       Optional<Type>           type        = Optional.empty();
    private       Optional<Type>           tupleType   = Optional.empty();
    private       Optional<SourceLocation> sourceLocation = Optional.empty();

    @Override
    public Value build() {
        if (members.size() == 1) {
            return members.get(0);
        } else if (members.isEmpty()) {
            throw new IllegalArgumentException("Can't have tuple with 0 members");
        } else if (hasTooManyMembers()) {
            throw new IllegalArgumentException("Can't have tuple with more than " + maxMembers() + " members");
        } else {
            return buildTuple();
        }
    }

    public boolean hasTooManyMembers() {
        return members.size() > 24;
    }

    public boolean isTuple() {
        return !members.isEmpty();
    }

    public int maxMembers() {
        return 24;
    }

    public TupleBuilder withMember(Value member) {
        members.add(member);
        return this;
    }

    @Override
    public TupleBuilder withSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = Optional.of(sourceLocation);
        return this;
    }

    public TupleBuilder withTupleType(Type type) {
        this.tupleType = Optional.of(type);
        return this;
    }

    public TupleBuilder withType(Type type) {
        this.type = Optional.of(type);
        return this;
    }

    private void buildConstructor(StringBuilder commas, Initializer.Builder builder) {
        builder.withValue(Identifier.builder()
            .withSourceLocation(require(sourceLocation, "Source location").getStartPoint())
            .withSymbol(qualified("scotch.data.tuple", "(" + commas.toString() + ")"))
            .withType(require(tupleType, "Constructor type"))
            .build());
    }

    private void buildFields(StringBuilder commas, Initializer.Builder builder) {
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                commas.append(",");
            }
            builder.addField(InitializerField.builder()
                .withSourceLocation(members.get(i).getSourceLocation())
                .withName("_" + i)
                .withValue(members.get(i))
                .build());
        }
    }

    private Value buildTuple() {
        StringBuilder commas = new StringBuilder();
        Initializer.Builder builder = Initializer.builder()
            .withType(require(type, "Type"))
            .withSourceLocation(require(sourceLocation, "Source location"));
        buildFields(commas, builder);
        buildConstructor(commas, builder);
        return builder.build();
    }
}
