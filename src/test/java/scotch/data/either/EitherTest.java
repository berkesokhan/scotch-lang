package scotch.data.either;

import static scotch.data.either.Either.left;
import static scotch.data.either.Either.right;

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
