package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;
import static scotch.compiler.syntax.Definition.classDef;
import static scotch.compiler.syntax.Type.var;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        classDef("Eq", asList(var("a")), asList());
    }

    @Ignore
    @Test
    public void shouldCreateInstance() {
        throw new UnsupportedOperationException(); // TODO
    }
}
