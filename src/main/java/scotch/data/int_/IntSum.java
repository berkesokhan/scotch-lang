package scotch.data.int_;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.DataType;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;

@DataType(memberName = "Int")
public class IntSum {

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }
}
