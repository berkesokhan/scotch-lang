package scotch.compiler;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.symbol.MethodSignature.methodSignature;
import static scotch.symbol.Operator.operator;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.symbol.descriptor.DataFieldDescriptor.field;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.compiler.util.TestUtil.constructor;
import static scotch.compiler.util.TestUtil.dataType;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.typeClass;
import static scotch.compiler.util.TestUtil.typeInstance;

import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import scotch.symbol.MethodSignature;
import scotch.symbol.SymbolEntry;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.Type;
import scotch.data.num.NumInt;

public class ClassLoaderResolverTest {

    private ClassLoaderResolver resolver;

    @Before
    public void setUp() {
        resolver = new ClassLoaderResolver(Optional.empty(), getClass().getClassLoader());
    }

    @Test
    public void shouldResolveJavaSymbol() {
        Type a = var("a", asList("scotch.data.num.Num"));
        SymbolEntry entry = resolver.getEntry(symbol("scotch.data.num.(+)")).get();

        assertThat(entry.getValue(), is(Optional.of(fn(a, fn(a, a)))));
        assertThat(entry.getOperator(), is(Optional.of(operator(LEFT_INFIX, 7))));
        assertThat(entry.getValueMethod(), is(Optional.of(methodSignature("scotch/data/num/Num:add:()Lscotch/runtime/Applicable;"))));
        assertThat(entry.getMemberOf(), is(Optional.of((symbol("scotch.data.num.Num")))));
    }

    @Test
    public void shouldResolveJavaTypeClass() {
        SymbolEntry entry = resolver.getEntry(symbol("scotch.data.num.Num")).get();
        assertThat(entry.getTypeClass(), is(Optional.of(typeClass("scotch.data.num.Num", asList(var("a")), asList(
            "scotch.data.num.(+)",
            "scotch.data.num.(-)",
            "scotch.data.num.(*)",
            "scotch.data.num.fromInteger",
            "scotch.data.num.signum",
            "scotch.data.num.negate",
            "scotch.data.num.abs"
        )))));
    }

    @Test
    public void shouldResolveJavaTypeInstanceByClassAndType() {
        assertThat(resolver.getTypeInstances(symbol("scotch.data.num.Num"), asList(intType())), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveJavaTypeInstanceByModuleName() {
        assertThat(resolver.getTypeInstancesByModule("scotch.data.num"), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveDataType() {
        assertThat(resolver.getEntry(symbol("scotch.data.maybe.Maybe")).get().getDataType(), is(Optional.of(dataType(
            "scotch.data.maybe.Maybe",
            asList(var("a")),
            asList(
                constructor(
                    0, "scotch.data.maybe.Maybe",
                    "scotch.data.maybe.Nothing"
                ),
                constructor(
                    1, "scotch.data.maybe.Maybe",
                    "scotch.data.maybe.Just",
                    asList(field(0, "value", var("a")))
                )
            )
        ))));
    }

    @Test
    public void shouldResolveEqForListOfInt() {
        Set<TypeInstanceDescriptor> typeInstances = resolver.getTypeInstances(
            symbol("scotch.data.eq.Eq"),
            asList(sum("scotch.data.list.[]", asList(intType())))
        );
        assertThat(typeInstances, hasSize(1));
    }
}
