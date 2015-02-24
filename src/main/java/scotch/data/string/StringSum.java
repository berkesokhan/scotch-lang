package scotch.data.string;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.DataType;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "String")
public class StringSum {

    @TypeParameters
    public static List<Type> parameters() {
        return ImmutableList.of();
    }
}
