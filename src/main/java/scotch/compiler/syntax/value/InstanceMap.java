package scotch.compiler.syntax.value;

import static java.util.Collections.sort;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import scotch.compiler.symbol.type.VariableType;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.util.Pair;

public class InstanceMap {

    private static final InstanceMap EMPTY = new InstanceMap(ImmutableList.of(), ImmutableMap.of());

    public static Builder builder() {
        return new Builder();
    }

    public static InstanceMap empty() {
        return EMPTY;
    }

    private final List<VariableType>                      arity;
    private final Map<VariableType, List<ClassReference>> instances;

    private InstanceMap(List<VariableType> arity, Map<VariableType, List<ClassReference>> instances) {
        this.arity = ImmutableList.copyOf(arity);
        this.instances = ImmutableMap.copyOf(instances);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof InstanceMap) {
            InstanceMap other = (InstanceMap) o;
            return Objects.equals(arity, other.arity)
                && Objects.equals(instances, other.instances);
        } else {
            return false;
        }
    }

    public List<ClassReference> getInstances(VariableType type) {
        return instances.getOrDefault(type.simplify(), ImmutableList.of());
    }

    @Override
    public int hashCode() {
        return Objects.hash(arity, instances);
    }

    public boolean isEmpty() {
        return arity.isEmpty();
    }

    public Stream<Pair<VariableType, List<ClassReference>>> stream() {
        return arity.stream().map(type -> pair(type, instances.get(type)));
    }

    public static class Builder {

        private final List<VariableType> arity;
        private final Map<VariableType, List<ClassReference>> instances;

        private Builder() {
            arity = new ArrayList<>();
            instances = new HashMap<>();
        }

        public Builder addInstance(VariableType type, ClassReference instance) {
            addInstance_(type.simplify(), instance);
            return this;
        }

        public InstanceMap build() {
            Map<VariableType, List<ClassReference>> sortedInstances = new HashMap<>();
            instances.forEach((type, list) -> {
                sort(list, (left, right) -> left.getSymbol().compareTo(right.getSymbol()));
                sortedInstances.put(type, list);
            });
            return new InstanceMap(arity, sortedInstances);
        }

        private void addInstance_(VariableType type, ClassReference instance) {
            if (!arity.contains(type)) {
                arity.add(type);
            }
            List<ClassReference> list = instances.computeIfAbsent(type, k -> new ArrayList<>());
            if (!list.contains(instance)) {
                list.add(instance);
            }
        }
    }
}
