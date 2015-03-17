package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.descriptor.TypeParameterDescriptor.typeParam;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.instance;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.t;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.symbol.type.Unification.mismatch;
import static scotch.compiler.syntax.StubResolver.defaultBind;
import static scotch.compiler.syntax.StubResolver.defaultDollarSign;
import static scotch.compiler.syntax.StubResolver.defaultEither;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultEqClass;
import static scotch.compiler.syntax.StubResolver.defaultEqOf;
import static scotch.compiler.syntax.StubResolver.defaultFromInteger;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultLeft;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultMonad;
import static scotch.compiler.syntax.StubResolver.defaultMonadOf;
import static scotch.compiler.syntax.StubResolver.defaultNum;
import static scotch.compiler.syntax.StubResolver.defaultNumOf;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultRight;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.boolType;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.doubleType;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.fieldDef;
import static scotch.compiler.util.TestUtil.instance;
import static scotch.compiler.util.TestUtil.instanceRef;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.matcher;
import static scotch.compiler.util.TestUtil.method;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.scopeRef;
import static scotch.compiler.util.TestUtil.stringType;

import java.util.List;
import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.ParserTest;
import scotch.compiler.symbol.type.InstanceType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.util.TestUtil;

public class TypeCheckerTest extends ParserTest {

    private Type intType;
    private Type doubleType;
    private Type boolType;
    private Type stringType;

    @Test
    public void identityOfIntShouldBeInt() {
        parse(
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.id", fn(t(1), t(1)));
        shouldHaveValue("scotch.test.test", intType);
    }

    @Test
    public void fibShouldBeIntOfInt() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fib", fn(intType, intType));
    }

    @Test
    public void mismatchedSignatureAndValueShouldReportTypeError() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn n = \"Hello, World! I'm a type error\""
        );
        shouldHaveErrors(typeError(
            mismatch(intType, stringType),
            source("test://mismatchedSignatureAndValueShouldReportTypeError", point(59, 4, 1), point(98, 4, 40))
        ));
    }

    @Test
    public void mismatchedPatternCaseShouldReportTypeError() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn 0 = 0",
            "fn 1 = \"Oops, I broke it :)\""
        );
        shouldHaveErrors(typeError(
            mismatch(intType, stringType),
            source("test://mismatchedPatternCaseShouldReportTypeError", point(90, 6, 1), point(118, 6, 29))
        ));
    }

    @Test
    public void shouldReportAllTypeErrors() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn1 :: Int -> Int",
            "fn2 :: String -> String",
            "fn1 n = \"Oops!\"",
            "fn2 n = 9"
        );
        shouldHaveErrors(
            typeError(mismatch(stringType, intType), source("test://shouldReportAllTypeErrors", point(126, 7, 1), point(135, 7, 10))),
            typeError(mismatch(intType, stringType), source("test://shouldReportAllTypeErrors", point(110, 6, 1), point(125, 6, 16)))
        );
    }

    @Test
    public void shouldReportMismatchedFunctionAndArgumentTypes() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn :: String -> Int",
            "fn n = 2",
            "val = fn 3"
        );
        shouldHaveErrors(typeError(
            mismatch(stringType, intType),
            source("test://shouldReportMismatchedFunctionAndArgumentTypes", point(106, 6, 10), point(107, 6, 11))
        ));
    }

    @Test
    public void shouldAnalyzeTypeOf2PlusAsIntegerIfOperandsAreIntegers() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "val = 2 + 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.val", intType);
    }

    @Test
    public void shouldAnalyzeTypeOf2PlusAsDoubleIfOperandsAreDoubles() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "val = 2.1 + 2.2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.val", doubleType);
    }

    @Test
    public void genericFunctionWithPlusShouldInheritContext() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "add a b = a + b"
        );
        Type a = t(11, asList("scotch.data.num.Num"));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.add", fn(a, fn(a, a)));
    }

    @Test
    public void shouldAnalyzeFunction() {
        parse(
            "module scotch.test",
            "apply = \\x y -> x y"
        );
        shouldHaveValue("scotch.test.apply", fn(fn(t(3), t(8)), fn(t(3), t(8))));
    }

    @Test
    public void shouldAnalyzeBoundMethod() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "four = 2 + 2"
        );
        InstanceType numType = instance("scotch.data.num.Num", t(32));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.four", apply(
            apply(
                apply(
                    method("scotch.data.num.(+)", asList(numType), fn(numType, fn(intType, fn(intType, intType)))),
                    instance(instanceRef("scotch.data.num", "scotch.data.num.Num", asList(typeParam(intType))), numType),
                    fn(intType, fn(intType, intType))
                ),
                literal(2),
                fn(intType, intType)
            ),
            literal(2),
            intType
        ));
    }

    @Test
    public void shouldAnalyzeMultiTypeClassExpression() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fn a b = a + b == b + a"
        );
        shouldNotHaveErrors();
        Type bool = sum("scotch.data.bool.Bool");
        Type t = t(17, asList("scotch.data.num.Num", "scotch.data.eq.Eq"));
        InstanceType numType = instance("scotch.data.num.Num", t(17));
        InstanceType eqType = instance("scotch.data.eq.Eq", t(17));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", TestUtil.matcher("scotch.test.(fn#0)", fn(t, fn(t, bool)),
            asList(arg("#0i", eqType), arg("#1i", numType), arg("#0", t), arg("#1", t)),
            pattern("scotch.test.(fn#0#0)", asList(capture("#0", "a", t), capture("#1", "b", t)), apply(
                apply(
                    apply(
                        method("scotch.data.eq.(==)", asList(eqType), fn(eqType, fn(t, fn(t, bool)))),
                        arg("#0i", eqType),
                        fn(t, fn(t, bool))
                    ),
                    apply(
                        apply(
                            apply(
                                method("scotch.data.num.(+)", asList(numType), fn(numType, fn(t, fn(t, t)))),
                                arg("#1i", numType),
                                fn(t, fn(t, t))
                            ),
                            arg("a", t),
                            fn(t, t)
                        ),
                        arg("b", t),
                        t
                    ),
                    fn(t, bool)
                ),
                apply(
                    apply(
                        apply(
                            method("scotch.data.num.(+)", asList(numType), fn(numType, fn(t, fn(t, t)))),
                            arg("#1i", numType),
                            fn(t, fn(t, t))
                        ),
                        arg("b", t),
                        fn(t, t)
                    ),
                    arg("a", t),
                    t
                ),
                bool
            ))
        ));
    }

    @Test
    public void shouldAnalyzeUnboundValue() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fn a b = a + b == b + a",
            "commutative? a b = fn a b"
        );
        Type t = t(12, asList("scotch.data.eq.Eq", "scotch.data.num.Num"));
        Type bool = sum("scotch.data.bool.Bool");
        InstanceType numType = instance("scotch.data.num.Num", t(12));
        InstanceType eqType = instance("scotch.data.eq.Eq", t(12));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(commutative?)", TestUtil.matcher(
            "scotch.test.(commutative?#0)", fn(t, fn(t, bool)), asList(arg("#0i", eqType), arg("#1i", numType), arg("#0", t), arg("#1", t)),
            pattern("scotch.test.(commutative?#0#0)", asList(capture("#0", "a", t), capture("#1", "b", t)), apply(
                apply(
                    apply(
                        apply(
                            method("scotch.test.fn", asList(eqType, numType), fn(eqType, fn(numType, fn(t, fn(t, bool))))),
                            arg("#0i", eqType),
                            fn(numType, fn(t, fn(t, bool)))
                        ),
                        arg("#1i", numType),
                        fn(t, fn(t, bool))
                    ),
                    arg("a", t),
                    fn(t, bool)
                ),
                arg("b", t),
                bool
            ))
        ));
    }

    @Test
    public void shouldBindInstanceOfPlus() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "val = 2 + 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.val", apply(
            apply(
                apply(
                    method(
                        "scotch.data.num.(+)",
                        asList(instance("scotch.data.num.Num", var("a"))),
                        fn(instance("scotch.data.num.Num", intType), fn(intType, fn(intType, intType)))
                    ),
                    instance(
                        instanceRef("scotch.data.num", "scotch.data.num.Num", asList(typeParam(intType))),
                        instance("scotch.data.num.Num", intType)
                    ),
                    fn(intType, fn(intType, intType))
                ),
                literal(2),
                fn(intType, intType)
            ),
            literal(2),
            intType
        ));
    }

    @Test
    public void shouldBubbleUpMethodBindingWithPattern() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "fn a b = a + b"
        );
        String num = "scotch.data.num.Num";
        Type t = t(11, asList(num));
        InstanceType instance = instance(num, t(11));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", TestUtil.matcher("scotch.test.(fn#0)", fn(t, fn(t, t)),
            asList(arg("#0i", instance), arg("#0", t), arg("#1", t)),
            pattern("scotch.test.(fn#0#0)", asList(capture("#0", "a", t), capture("#1", "b", t)), apply(
                apply(
                    apply(
                        method("scotch.data.num.(+)", asList(instance), fn(instance, fn(t, fn(t, t)))),
                        arg("#0i", instance),
                        fn(t, fn(t, t))
                    ),
                    arg("a", t),
                    fn(t, t)
                ),
                arg("b", t),
                t
            ))
        ));
    }

    @Test
    public void shouldBubbleUpMethodBindingWithFunction() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "fn = \\x y -> x + y"
        );
        String num = "scotch.data.num.Num";
        Type t = t(10, asList(num));
        InstanceType instance = instance(num, t(10));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", matcher(
            "scotch.test.(fn#0)",
            fn(t, fn(t, t)),
            asList(arg("#0i", instance), arg("#0", t), arg("#1", t)),
            pattern(
                "scotch.test.(fn#0#1)", asList(capture("#0", "x", t), capture("#1", "y", t)), apply(
                apply(
                    apply(
                        method("scotch.data.num.(+)", asList(instance(num, var("a"))), fn(instance, fn(t, fn(t, t)))),
                        arg("#0i", instance),
                        fn(t, fn(t, t))
                    ),
                    arg("x", t),
                    fn(t, t)
                ),
                arg("y", t),
                t
            ))
        ));
    }

    @Test
    public void shouldBindInstanceToBubbledUpMethodBinding() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "fn = \\x y -> x + y",
            "result = fn 3 2"
        );
        String num = "scotch.data.num.Num";
        InstanceType instance = instance(num, t(16));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.result", apply(
            apply(
                apply(
                    method("scotch.test.fn", asList(instance), fn(instance, fn(intType, fn(intType, intType)))),
                    instance(instanceRef("scotch.data.num", "scotch.data.num.Num", asList(typeParam((intType)))), instance),
                    fn(intType, fn(intType, intType))
                ),
                literal(3),
                fn(intType, intType)
            ),
            literal(2),
            intType
        ));
    }

    @Test
    public void shouldCaptureEnclosedVariables() {
        parse(
            "module scotch.test",
            "fn a b = \\x y -> x a b y"
        );
        shouldNotHaveErrors();
        shouldHaveCaptures(scopeRef("scotch.test.(fn#0#1)"), asList("a", "b"));
        shouldHaveCaptures(scopeRef("scotch.test.(fn#0#0)"), asList());
    }

    @Test
    public void shouldMarkLocalVariables() {
        parse(
            "module scotch.test",
            "fn a b = \\x y -> x a b y"
        );
        shouldNotHaveErrors();
        shouldHaveLocals(scopeRef("scotch.test.(fn#0#1)"), asList("#0", "#1", "x", "y"));
        shouldHaveLocals(scopeRef("scotch.test.(fn#0)"), asList("#0", "#1", "a", "b"));
    }

    @Test
    public void shouldGetTypeOfLet() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        Type num = t(20, asList("scotch.data.num.Num"));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.main", t(11));
        shouldHaveValue("scotch.test.(main#f)", fn(intType(), intType()));
        shouldHaveValue("scotch.test.(main#a)", fn(num, num));
    }

    @Test
    public void shouldBindMethodsInEqualMatch() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.int",
            "fib 0 = 0"
        );
        InstanceType instance = instance("scotch.data.eq.Eq", intType);
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fib", matcher("scotch.test.(fib#0)", fn(intType, intType), arg("#0", intType), pattern(
            "scotch.test.fib#0#0",
            asList(equal("#0", apply(
                apply(
                    apply(
                        method("scotch.data.eq.(==)", asList(instance), fn(instance, fn(intType, fn(intType, boolType)))),
                        instance(instanceRef("scotch.data.eq", "scotch.data.eq.Eq", asList(typeParam(intType))), instance),
                        fn(intType, fn(intType, boolType))
                    ),
                    arg("#0", intType),
                    fn(intType, boolType)
                ),
                literal(0),
                boolType
            ))),
            literal(0)
        )));
    }

    @Test
    public void shouldGetTypeOfRight() {
        parse(
            "module scotch.test",
            "import scotch.data.either",
            "run = Right 1"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.run", sum("scotch.data.either.Either", t(4), intType));
    }

    @Test
    public void shouldGetTypeOfLeft() {
        parse(
            "module scotch.test",
            "import scotch.data.either",
            "run = Left \"Albuquerque\""
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.run", sum("scotch.data.either.Either", stringType, t(4)));
    }

    @Test
    public void bindShouldGiveEitherOfStringAndSomething() {
        parse(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.either",
            "run = Right 1 >>= \\i -> Left \"Oops\""
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.run", sum("scotch.data.either.Either", stringType, t(18)));
    }

    @Test
    public void idPatternShouldHaveGenericType() {
        parse(
            "module scotch.test",
            "import scotch.data.function",
            "import scotch.data.num",
            "id x = x",
            "run = (fromInteger $ id 1) + id 2.2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.run", doubleType);
    }

    @Test
    public void idFunctionShouldHaveGenericType() {
        parse(
            "module scotch.test",
            "import scotch.data.function",
            "import scotch.data.num",
            "id = \\x -> x",
            "run = (fromInteger $ id 1) + id 2.2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.run", doubleType);
    }

    @Test
    public void shouldCreateFunctionForInitializer() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "",
            "data QuantifiedThing a { howMany Int, thing a }"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.QuantifiedThing", fn(intType, fn(t(0), sum("scotch.test.QuantifiedThing", asList(t(0))))));
        shouldHaveData(0, "scotch.test.QuantifiedThing", asList(var("a")), asList(
            fieldDef(0, "howMany", intType),
            fieldDef(1, "thing", var("a"))
        ));
    }

    @Test
    public void shouldParseListSignature() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "",
            "fn :: [Int] -> Int"
        );
        shouldNotHaveErrors();
        shouldHaveSignature("scotch.test.fn", fn(sum("scotch.data.list.[]", asList(intType)), intType));
    }

    @Test
    public void shouldParseTupleSignature() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "",
            "fn :: (Int, Int) -> Int"
        );
        shouldNotHaveErrors();
        shouldHaveSignature("scotch.test.fn", fn(sum("scotch.data.tuple.(,)", asList(intType, intType)), intType));
    }

    private void shouldHaveLocals(DefinitionReference reference, List<String> locals) {
        assertThat(getScope(reference).getLocals(), is(locals));
    }

    private void shouldHaveCaptures(DefinitionReference reference, List<String> captures) {
        assertThat(getScope(reference).getCaptures(), is(captures));
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::checkTypes;
    }

    @Override
    protected void setUp() {
        intType = intType();
        doubleType = doubleType();
        boolType = boolType();
        stringType = stringType();
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver
            // types
            .define(defaultInt())
            .define(defaultString())
            // num
            .define(defaultNum())
            .define(defaultNumOf(intType))
            .define(defaultNumOf(doubleType))
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultFromInteger())
            // eq
            .define(defaultEq())
            .define(defaultEqOf(intType))
            .define(defaultEqClass())
            // either
            .define(defaultEither())
            .define(defaultRight())
            .define(defaultLeft())
            // monad
            .define(defaultMonad())
            .define(defaultBind())
            .define(defaultMonadOf(sum("scotch.data.either.Either", var("a"))))
            // $
            .define(defaultDollarSign());
    }
}
