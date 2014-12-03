package scotch.compiler.parser;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.analyzeTypes;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class TypeAnalyzerTest {

    private Type         intType;
    private Type         stringType;
    private StubResolver resolver;

    @Before
    public void setUp() {
        intType = sum("scotch.data.int.Int");
        stringType = sum("scotch.data.string.String");
        resolver = new StubResolver()
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultInt())
            .define(defaultString());
    }

    @Test
    public void identityOfIntShouldBeInt() {
        DefinitionGraph graph = analyzeTypes(
            resolver,
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        assertThat(graph.getValue(valueRef("scotch.test", "test")).get(), is(intType));
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
        assertThat(graph.getValue(valueRef("scotch.test", "fib")).get(), is(fn(intType, intType)));
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
}
