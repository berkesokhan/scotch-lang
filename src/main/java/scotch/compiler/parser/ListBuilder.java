package scotch.compiler.parser;

import static scotch.symbol.Symbol.symbol;
import static scotch.compiler.syntax.value.InitializerField.field;

import java.util.ArrayDeque;
import java.util.Deque;
import scotch.symbol.util.SymbolGenerator;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.value.ConstantValue;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Initializer;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

public class ListBuilder implements SyntaxBuilder<Value> {

    public static ListBuilder builder(SymbolGenerator generator) {
        return new ListBuilder(generator);
    }

    private final SymbolGenerator generator;
    private final Deque<Value>    values;

    private ListBuilder(SymbolGenerator generator) {
        this.generator = generator;
        this.values = new ArrayDeque<>();
    }

    @Override
    public Value build() {
        Value tail = ConstantValue.builder()
            .withDataType(symbol("scotch.data.list.[]"))
            .withSymbol(symbol("scotch.data.list.[]"))
            .withType(generator.reserveType())
            .withSourceLocation(values.peek().getSourceLocation().getEndPoint())
            .build();
        while (!values.isEmpty()) {
            Value head = values.pop();
            Initializer.Builder builder = Initializer.builder()
                .withSourceLocation(head.getSourceLocation().extend(tail.getSourceLocation()))
                .withType(generator.reserveType())
                .withValue(Identifier.builder()
                    .withType(generator.reserveType())
                    .withSourceLocation(head.getSourceLocation().getStartPoint())
                    .withSymbol(symbol("scotch.data.list.(:)"))
                    .build());
            builder.addField(field(head.getSourceLocation(), "_0", head));
            builder.addField(field(tail.getSourceLocation(), "_1", tail));
            tail = builder.build();
        }
        return tail;
    }

    @Override
    public ListBuilder withSourceLocation(SourceLocation sourceLocation) {
        // noop
        return this;
    }

    public ListBuilder addValue(Value value) {
        values.push(value);
        return this;
    }
}
