package scotch.compiler.syntax.definition;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;
import static scotch.compiler.util.TestUtil.classDef;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.symbol.type.Types;

public class ClassDefinitionTest {

    @Rule
    public final ExpectedException exception = none();

    @Test
    public void shouldThrow_whenCreatingDefinitionWithNoArguments() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't create class definition with 0 arguments");
        classDef("Eq", asList(), asList());
    }

    @Test
    public void shouldThrow_whenCreatingDefinitionWithNoMembers() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't create class definition with 0 members");
        classDef("Eq", asList(Types.var("a")), asList());
    }

    @Ignore
    @Test
    public void shouldCreateInstance() {
        throw new UnsupportedOperationException(); // TODO
    }
}
