package scotch.data.int_;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@DataType(memberName = "Int")
public class IntSum {

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }
}
