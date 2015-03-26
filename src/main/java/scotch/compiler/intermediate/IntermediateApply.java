package scotch.compiler.intermediate;

import java.util.List;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediateApply extends IntermediateValue {

    private final List<String>      captures;
    private final IntermediateValue function;
    private final IntermediateValue argument;

    public IntermediateApply(List<String> captures, IntermediateValue function, IntermediateValue argument) {
        this.captures = ImmutableList.copyOf(captures);
        this.function = function;
        this.argument = argument;
    }
}
