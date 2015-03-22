package scotch.data.int_;

import static scotch.symbol.type.Types.sum;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "Int")
public class Int {

    public static Type TYPE = sum("scotch.data.int.Int");

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }
}
