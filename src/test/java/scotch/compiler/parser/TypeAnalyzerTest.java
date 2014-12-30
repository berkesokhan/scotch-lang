package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.instance;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
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
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.method;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.syntax.DefinitionGraph;
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
        shouldHaveErrors(graph, typeError(
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
        shouldHaveErrors(graph, typeError(
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
        shouldHaveErrors(graph,
            typeError(mismatch(intType, stringType), source("shouldReportAllTypeErrors", point(110, 6, 1), point(125, 6, 16))),
            typeError(mismatch(stringType, intType), source("shouldReportAllTypeErrors", point(126, 7, 1), point(135, 7, 10)))
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
        shouldHaveErrors(graph, typeError(
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
            asList(arg("$0", t), arg("$1", t)),
            patterns(bool, pattern("scotch.test.($0)", asList(capture("$0", "a", t), capture("$1", "b", t)), apply(
                apply(
                    method("scotch.data.eq.(==)", asList(eqType), fn(eqType, fn(t, fn(t, bool)))),
                    apply(
                        apply(method("scotch.data.num.(+)", asList(numType), fn(numType, fn(t, fn(t, t)))), arg("a", t), fn(t, t)),
                        arg("b", t),
                        t
                    ),
                    fn(t, bool)
                ),
                apply(
                    apply(method("scotch.data.num.(+)", asList(numType), fn(numType, fn(t, fn(t, t)))), arg("b", t), fn(t, t)),
                    arg("a", t),
                    t
                ),
                bool
            )))
        ));
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
