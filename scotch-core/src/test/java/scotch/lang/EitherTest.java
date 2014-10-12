package scotch.lang;

import static scotch.lang.Either.left;
import static scotch.lang.Either.right;

import org.junit.Test;

public class EitherTest {

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_whenGettingLeftFromRight() {
        right("value").getLeft();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_whenGettingRightFromLeft() {
        left("value").getRight();
    }
}
