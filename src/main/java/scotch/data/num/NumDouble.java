package scotch.data.num;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.type.Type;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Types;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.num.Num")
public class NumDouble implements Num<Double> {

    private static final Callable<NumDouble> INSTANCE = callable(NumDouble::new);

    @InstanceGetter
    public static Callable<NumDouble> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(Types.sum("scotch.data.double.Double"));
    }

    @Override
    public Callable<Double> abs(Callable<Double> operand) {
        return callable(() -> Math.abs(operand.call()));
    }

    @Override
    public Callable<Double> add(Callable<Double> left, Callable<Double> right) {
        return callable(() -> left.call() + right.call());
    }

    @Override
    public Callable<Double> fromInteger(Callable<Integer> integer) {
        return callable(() -> integer.call().doubleValue());
    }

    @Override
    public Callable<Double> multiply(Callable<Double> left, Callable<Double> right) {
        return callable(() -> left.call() * right.call());
    }

    @Override
    public Callable<Double> signum(Callable<Double> operand) {
        return callable(() -> {
            double value = operand.call();
            if (value > 0) {
                return 1d;
            } else if (value < 0) {
                return -1d;
            } else {
                return 0d;
            }
        });
    }

    @Override
    public Callable<Double> sub(Callable<Double> left, Callable<Double> right) {
        return callable(() -> left.call() - right.call());
    }
}
