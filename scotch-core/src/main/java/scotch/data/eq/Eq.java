package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.syntax.Type.var;
import static scotch.lang.ClassMember.optionalMember;

import java.util.List;
import java.util.function.Function;
import scotch.compiler.syntax.Type;
import scotch.lang.Class;
import scotch.lang.ClassMember;
import scotch.lang.InstanceProvider;
import scotch.lang.InstanceProviderFactory;
import scotch.lang.TypeClass;
import scotch.lang.TypeMember;

@TypeClass(name = "scotch.data.eq.Eq")
public class Eq implements Class {

    private final InstanceProvider provider;

    public Eq(InstanceProviderFactory factory) {
        this.provider = factory.getProvider(this);
    }

    @TypeMember(name = "==", mandatory = false)
    public <A> Function<A, Function<A, Boolean>> eq(List<Type> arguments) {
        return this.<A>getInstance(arguments)::eq;
    }

    @Override
    public List<Type> getArguments() {
        return asList(var("a"));
    }

    @Override
    public List<ClassMember> getMembers() {
        return asList(
            optionalMember("==", fn(var("a"), fn(var("a"), sum("Bool")))),
            optionalMember("/=", fn(var("a"), fn(var("a"), sum("Bool"))))
        );
    }

    @TypeMember(name = "/=", mandatory = false)
    public <A> Function<A, Function<A, Boolean>> notEq(List<Type> arguments) {
        return this.<A>getInstance(arguments)::notEq;
    }

    @SuppressWarnings("unchecked")
    private <A> EqInstance<A> getInstance(List<Type> arguments) {
        return provider.getInstance(arguments, EqInstance.class);
    }
}
