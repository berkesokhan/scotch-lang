package scotch.compiler.util;

import static lombok.AccessLevel.PRIVATE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class Pair<A, B> {

    public static <A, B> Pair<A, B> pair(A left, B right) {
        return new Pair<>(left, right);
    }

    @NonNull @Getter private final A left;
    @NonNull @Getter private final B right;

    public <C> C into(Destructure<A, B, C> destructure) {
        return destructure.apply(left, right);
    }

    @FunctionalInterface
    public interface Destructure<A, B, C> {

        C apply(A left, B right);
    }
}
