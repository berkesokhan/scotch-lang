package scotch.data.ord;

import static scotch.runtime.RuntimeSupport.callable;
import static scotch.symbol.type.Types.sum;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.runtime.Callable;
import scotch.symbol.DataConstructor;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameters;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "Ordering")
public abstract class Ordering {

    public static final Type TYPE = sum("scotch.data.ord.Ordering");

    @Value(memberName = "EqualTo")
    public static Callable<Ordering> equalTo() {
        return EqualTo.INSTANCE;
    }

    @ValueType(forMember = "EqualTo")
    public static Type equalTo$type() {
        return TYPE;
    }

    @Value(memberName = "GreaterThan")
    public static Callable<Ordering> greaterThan() {
        return GreaterThan.INSTANCE;
    }

    @ValueType(forMember = "GreaterThan")
    public static Type egreaterThan$type() {
        return TYPE;
    }

    @Value(memberName = "LessThan")
    public static Callable<Ordering> lessThan() {
        return LessThan.INSTANCE;
    }

    @ValueType(forMember = "LessThan")
    public static Type lessThan$type() {
        return TYPE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }

    private Ordering() {
        // intentionally empty
    }

    @DataConstructor(ordinal = 1, memberName = "EqualTo", dataType = "Ordering")
    public static class EqualTo extends Ordering {

        public static final Callable<Ordering> INSTANCE = callable(EqualTo::new);

        private EqualTo() {
            // intentionally empty
        }

        @Override
        public String toString() {
            return "EqualTo";
        }
    }

    @DataConstructor(ordinal = 2, memberName = "GreaterThan", dataType = "Ordering")
    public static class GreaterThan extends Ordering {

        public static final Callable<Ordering> INSTANCE = callable(GreaterThan::new);

        private GreaterThan() {
            // intentionally empty
        }

        @Override
        public String toString() {
            return "GreaterThan";
        }
    }

    @DataConstructor(ordinal = 0, memberName = "LessThan", dataType = "Ordering")
    public static class LessThan extends Ordering {

        public static final Callable<Ordering> INSTANCE = callable(LessThan::new);

        private LessThan() {
            // intentionally empty
        }

        @Override
        public String toString() {
            return "LessThan";
        }
    }
}
