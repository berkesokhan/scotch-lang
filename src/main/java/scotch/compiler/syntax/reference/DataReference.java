package scotch.compiler.syntax.reference;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.symbol.Symbol;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class DataReference extends DefinitionReference {

    @NonNull @Getter private final Symbol symbol;
}
