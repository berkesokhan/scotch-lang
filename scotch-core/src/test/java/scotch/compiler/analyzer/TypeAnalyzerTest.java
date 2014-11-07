package scotch.compiler.analyzer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.SourcePoint.point;
import static scotch.compiler.syntax.SourceRange.source;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.syntax.Unification.mismatch;
import static scotch.compiler.util.TestUtil.analyzeTypes;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.Type;

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
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        assertThat(symbols.getValue(valueRef("scotch.test", "test")), is(intType));
    }

    @Test
    public void fibShouldBeIntOfInt() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getValue(valueRef("scotch.test", "fib")), is(fn(intType, intType)));
    }

    @Test
    public void mismatchedSignatureAndValueShouldReportTypeError() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn n = \"Hello, World! I'm a type error\""
        );
        assertThat(symbols.hasErrors(), is(true));
        assertThat(symbols.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(59, 4, 1), point(98, 4, 40)))
        ));
    }

    @Test
    public void mismatchedPatternCaseShouldReportTypeError() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn 0 = 0",
            "fn 1 = \"Oops, I broke it :)\""
        );
        assertThat(symbols.hasErrors(), is(true));
        assertThat(symbols.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(68, 5, 1), point(96, 5, 29)))
        ));
    }

    @Test
    public void shouldReportAllTypeErrors() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn1 :: Int -> Int",
            "fn2 :: String -> String",
            "fn1 n = \"Oops!\"",
            "fn2 n = 9"
        );
        assertThat(symbols.getErrors(), hasSize(2));
        assertThat(symbols.getErrors(), contains(
            typeError(mismatch(intType, stringType), source("$test", point(110, 6, 1), point(125, 6, 16))),
            typeError(mismatch(stringType, intType), source("$test", point(126, 7, 1), point(135, 7, 10)))
        ));
    }

    @Test
    public void shouldReportMismatchedFunctionAndArgumentTypes() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.string",
            "fn :: String -> Int",
            "fn n = 2",
            "val = fn 3"
        );
        assertThat(symbols.hasErrors(), is(true));
        assertThat(symbols.getErrors(), contains(
            typeError(mismatch(stringType, intType), source("$test", point(103, 6, 7), point(107, 6, 11)))
        ));
    }
}
