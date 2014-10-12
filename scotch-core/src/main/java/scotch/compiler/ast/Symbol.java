package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.quote;

import java.util.Optional;
import scotch.lang.Type;

public class Symbol {

    public static Symbol symbol(String name) {
        return new Symbol(name);
    }

    private final String name;
    private Optional<Type> type      = Optional.empty();
    private Optional<Type> valueType = Optional.empty();

    private Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type.orElseThrow(() -> new IllegalStateException("No type found for symbol " + quote(name)));
    }

    public Type getValueType() {
        return valueType.orElseThrow(() -> new IllegalStateException("No value type for found symbol " + quote(name)));
    }

    public boolean hasValueType() {
        return valueType.isPresent();
    }

    public void setType(Type type) {
        this.type = Optional.of(type);
    }

    public void setValueType(Type valueType) {
        this.valueType = Optional.of(valueType);
    }
}
