package scotch.data.string;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "String")
public class StringSum {

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }
}
