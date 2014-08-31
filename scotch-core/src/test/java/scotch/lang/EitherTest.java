package scotch.lang;

import static scotch.lang.Either.*;

import org.junit.Test;

public class EitherTest {

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_whenGettingRightFromLeft() {
        left("value").getRight();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_whenGettingLeftFromRight() {
        right("value").getLeft();
    }
}
