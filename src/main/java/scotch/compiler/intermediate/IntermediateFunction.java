package scotch.compiler.intermediate;

import java.util.List;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediateFunction extends IntermediateValue {

    private final List<String>      captures;
    private final String            argument;
    private final IntermediateValue body;

    public IntermediateFunction(List<String> captures, String argument, IntermediateValue body) {
        this.captures = ImmutableList.copyOf(captures);
        this.argument = argument;
        this.body = body;
    }
}
