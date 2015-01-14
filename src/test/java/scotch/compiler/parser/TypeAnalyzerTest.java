package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.instance;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultEqClass;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultNum;
import static scotch.compiler.syntax.StubResolver.defaultNumOf;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.doubleType;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.instance;
import static scotch.compiler.util.TestUtil.instanceRef;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.method;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;
import static scotch.compiler.util.TestUtil.scopeRef;

import java.util.List;
import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.StubResolver;

public class TypeAnalyzerTest extends ParserTest {

    private Type intType;
    private Type doubleType;
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
            source("mismatchedSignatureAndValueShouldReportTypeError", point(59, 4, 1), point(98, 4, 40))
        ));
    }

    @Test
    public void mismatchedPatternCaseShouldReportTypeError() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn 0 = 0",
            "fn 1 = \"Oops, I broke it :)\""
        );
        shouldHaveErrors(typeError(
            mismatch(intType, stringType),
            source("mismatchedPatternCaseShouldReportTypeError", point(68, 5, 1), point(96, 5, 29))
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
            typeError(mismatch(stringType, intType), source("shouldReportAllTypeErrors", point(126, 7, 1), point(135, 7, 10))),
            typeError(mismatch(intType, stringType), source("shouldReportAllTypeErrors", point(110, 6, 1), point(125, 6, 16)))
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
            source("shouldReportMismatchedFunctionAndArgumentTypes", point(103, 6, 7), point(107, 6, 11))
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
        Type a = t(12, asList("scotch.data.num.Num"));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.add", fn(a, fn(a, a)));
    }

    @Test
    public void shouldAnalyzeFunction() {
        parse(
            "module scotch.test",
            "apply = \\x y -> x y"
        );
        shouldHaveValue("scotch.test.apply", fn(fn(t(2), t(6)), fn(t(2), t(6))));
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
        shouldHaveValue("scotch.test.four", apply(
            apply(
                apply(
                    method("scotch.data.num.(+)", asList(numType), fn(numType, fn(intType, fn(intType, intType)))),
                    instance(instanceRef("scotch.data.num", "scotch.data.num.Num", asList(intType)), numType),
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
        Type t = t(20, asList("scotch.data.num.Num", "scotch.data.eq.Eq"));
        InstanceType numType = instance("scotch.data.num.Num", t(20));
        InstanceType eqType = instance("scotch.data.eq.Eq", t(20));
        shouldHaveValue("scotch.test.fn", fn(
            "scotch.test.($1)",
            asList(arg("$i0", eqType), arg("$i1", numType), arg("$0", t), arg("$1", t)),
            patterns(bool, pattern("scotch.test.($0)", asList(capture("$0", "a", t), capture("$1", "b", t)), apply(
                apply(
                    apply(
                        method("scotch.data.eq.(==)", asList(eqType), fn(eqType, fn(t, fn(t, bool)))),
                        arg("$i0", eqType),
                        fn(t, fn(t, bool))
                    ),
                    apply(
                        apply(
                            apply(
                                method("scotch.data.num.(+)", asList(numType), fn(numType, fn(t, fn(t, t)))),
                                arg("$i1", numType),
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
                            arg("$i1", numType),
                            fn(t, fn(t, t))
                        ),
                        arg("b", t),
                        fn(t, t)
                    ),
                    arg("a", t),
                    t
                ),
                bool
            )))
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
        Type t = t(41, asList("scotch.data.eq.Eq", "scotch.data.num.Num"));
        Type bool = sum("scotch.data.bool.Bool");
        InstanceType numType = instance("scotch.data.num.Num", t(32));
        InstanceType eqType = instance("scotch.data.eq.Eq", t(32));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(commutative?)", fn(
            "scotch.test.($3)",
            asList(arg("$i0", eqType), arg("$i1", numType), arg("$0", t), arg("$1", t)),
            patterns(bool, pattern(
                "scotch.test.($1)",
                asList(capture("$0", "a", t), capture("$1", "b", t)),
                apply(
                    apply(
                        apply(
                            apply(
                                method("scotch.test.fn", asList(eqType, numType), fn(eqType, fn(numType, fn(t, fn(t, bool))))),
                                arg("$i0", eqType),
                                fn(numType, fn(t, fn(t, bool)))
                            ),
                            arg("$i1", numType),
                            fn(t, fn(t, bool))
                        ),
                        arg("a", t),
                        fn(t, bool)
                    ),
                    arg("b", t),
                    bool
                )
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
                        instanceRef("scotch.data.num", "scotch.data.num.Num", asList(intType)),
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
        Type t = t(12, asList(num));
        InstanceType instance = instance(num, t(12));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", fn(
            "scotch.test.($1)",
            asList(arg("$i0", instance), arg("$0", t), arg("$1", t)),
            patterns(t, pattern("scotch.test.($0)", asList(capture("$0", "a", t), capture("$1", "b", t)), apply(
                apply(
                    apply(
                        method("scotch.data.num.(+)", asList(instance), fn(instance, fn(t, fn(t, t)))),
                        arg("$i0", instance),
                        fn(t, fn(t, t))
                    ),
                    arg("a", t),
                    fn(t, t)
                ),
                arg("b", t),
                t
            )))
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
        Type t = t(8, asList(num));
        InstanceType instance = instance(num, t(8));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", fn(
            "scotch.test.($0)",
            asList(arg("$i0", instance), arg("x", t), arg("y", t)),
            apply(
                apply(
                    apply(
                        method("scotch.data.num.(+)", asList(instance), fn(instance, fn(t, fn(t, t)))),
                        arg("$i0", instance),
                        fn(t, fn(t, t))
                    ),
                    arg("x", t),
                    fn(t, t)
                ),
                arg("y", t),
                t
            )
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
                    instance(instanceRef("scotch.data.num", "scotch.data.num.Num", asList(intType)), instance),
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
        shouldHaveCaptures(scopeRef("scotch.test.($1)"), asList("a", "b"));
        shouldHaveCaptures(scopeRef("scotch.test.($2)"), asList());
    }

    @Test
    public void shouldMarkLocalVariables() {
        parse(
            "module scotch.test",
            "fn a b = \\x y -> x a b y"
        );
        shouldNotHaveErrors();
        shouldHaveLocals(scopeRef("scotch.test.($1)"), asList("x", "y"));
        shouldHaveLocals(scopeRef("scotch.test.($2)"), asList("$0", "$1", "a", "b"));
    }

    private void shouldHaveLocals(DefinitionReference reference, List<String> locals) {
        assertThat(getScope(reference).getLocals(), is(locals));
    }

    private void shouldHaveCaptures(DefinitionReference reference, List<String> captures) {
        assertThat(getScope(reference).getCaptures(), is(captures));
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::analyzeTypes;
    }

    @Override
    protected void setUp() {
        intType = intType();
        doubleType = doubleType();
        stringType = sum("scotch.data.string.String");
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultInt())
            .define(defaultString())
            .define(defaultNum())
            .define(defaultNumOf(intType))
            .define(defaultNumOf(doubleType))
            .define(defaultEq())
            .define(defaultEqClass());
    }
}
