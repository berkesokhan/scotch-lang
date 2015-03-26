package scotch.compiler.intermediate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediatePattern extends IntermediateValue {

    private final DecisionTree tree;
}
