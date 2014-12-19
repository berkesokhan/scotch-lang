package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultNum;
import static scotch.compiler.syntax.StubResolver.defaultNumOf;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.analyzeTypes;
import static scotch.compiler.util.TestUtil.doubleType;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.valueRef;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class TypeAnalyzerTest {

    private Type         intType;
    private Type         doubleType;
    private Type         stringType;
    private StubResolver resolver;

    @Before
    public void setUp() {
        intType = intType();
        doubleType = doubleType();
        stringType = sum("scotch.data.string.String");
        resolver = new StubResolver()
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultInt())
            .define(defaultString())
            .define(defaultNum())
            .define(defaultNumOf(intType))
            .define(defaultNumOf(doubleType));
    }

    @Test
    public void identityOfIntShouldBeInt() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        assertThat(graph.getValue(valueRef("scotch.test.id")).get(), is(fn(t(1), t(1))));
        assertThat(graph.getValue(valueRef("scotch.test.test")).get(), is(intType));
    }

    @Test
    public void fibShouldBeIntOfInt() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(graph.getValue(valueRef("scotch.test.fib")).get(), is(fn(intType, intType)));
    }

    @Test
    public void mismatchedSignatureAndValueShouldReportTypeError() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn n = \"Hello, World! I'm a type error\""
        );
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(59, 4, 1), point(98, 4, 40)))
        ));
    }

    @Test
    public void mismatchedPatternCaseShouldReportTypeError() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn 0 = 0",
            "fn 1 = \"Oops, I broke it :)\""
        );
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(68, 5, 1), point(96, 5, 29)))
        ));
    }

    @Test
    public void shouldReportAllTypeErrors() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn1 :: Int -> Int",
            "fn2 :: String -> String",
            "fn1 n = \"Oops!\"",
            "fn2 n = 9"
        );
        assertThat(graph.getErrors(), hasSize(2));
        assertThat(graph.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(110, 6, 1), point(125, 6, 16))),
            typeError(mismatch(stringType, intType), source("$test", point(126, 7, 1), point(135, 7, 10)))
        ));
    }

    @Test
    public void shouldReportMismatchedFunctionAndArgumentTypes() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn :: String -> Int",
            "fn n = 2",
            "val = fn 3"
        );
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(
            typeError(mismatch(stringType, intType), source("$test", point(103, 6, 7), point(107, 6, 11)))
        ));
    }

    @Test
    public void shouldAnalyzeTypeOf2PlusAsIntegerIfOperandsAreIntegers() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "val = 2 + 2"
        );
        assertThat(graph.getErrors(), empty());
        assertThat(graph.getValue(valueRef("scotch.test.val")).get(), is(intType));
    }

    @Test
    public void shouldAnalyzeTypeOf2PlusAsDoubleIfOperandsAreDoubles() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "val = 2.1 + 2.2"
        );
        assertThat(graph.getErrors(), empty());
        assertThat(graph.getValue(valueRef("scotch.test.val")).get(), is(doubleType));
    }

    @Test
    public void genericFunctionWithPlusShouldInheritContext() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "add a b = a + b"
        );
        assertThat(graph.getErrors(), empty());
        VariableType a = t(12, asList("scotch.data.num.Num"));
        assertThat(graph.getValue(valueRef("scotch.test.add")).get(), is(fn(a, fn(a, a))));
    }

    @Test
    public void shouldAnalyzeFunction() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "apply = \\x y -> x y"
        );
        System.out.println(graph.getValue(valueRef("scotch.test.apply")).get().prettyPrint());
        assertThat(graph.getValue(valueRef("scotch.test.apply")).get(), is(fn(fn(t(2), t(6)), fn(t(2), t(6)))));
    }
}
