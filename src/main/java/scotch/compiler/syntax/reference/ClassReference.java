package scotch.compiler.syntax.reference;

import static lombok.AccessLevel.PACKAGE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import scotch.symbol.Symbol;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class ClassReference extends DefinitionReference {

    @Getter
    private final Symbol symbol;
}
