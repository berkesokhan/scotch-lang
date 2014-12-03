package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.Value.Fixity.PREFIX;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.DefinitionReference.operatorRef;
import static scotch.compiler.syntax.DefinitionReference.patternRef;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.util.TestUtil.bodyOf;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.classDef;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.message;
import static scotch.compiler.util.TestUtil.operatorDef;
import static scotch.compiler.util.TestUtil.parseInput;
import static scotch.compiler.util.TestUtil.root;
import static scotch.compiler.util.TestUtil.signature;
import static scotch.compiler.util.TestUtil.unshuffled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.syntax.DefinitionGraph;

public class InputParserTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldParseClassDefinition() {
        DefinitionGraph graph = parseInput(
            "module scotch.data.eq",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not x /= y",
            "    x /= y = not x == y"
        );
        assertThat(graph.getDefinition(classRef("scotch.data.eq", "Eq")).get(), is(
            classDef("scotch.data.eq.Eq", asList(var("a")), asList(
                signatureRef("scotch.data.eq", "=="),
                signatureRef("scotch.data.eq", "/="),
                patternRef("scotch.data.eq", "pattern#3"),
                patternRef("scotch.data.eq", "pattern#11")
            ))
        ));
    }

    @Test
    public void shouldParseMultiValueSignature() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "(==), (/=) :: a -> a -> Bool"
        );
        assertThat(graph.getDefinition(signatureRef("scotch.test", "==")).get(), is(
            signature("scotch.test.(==)", fn(var("a"), fn(var("a"), sum("Bool"))))
        ));
        assertThat(graph.getDefinition(signatureRef("scotch.test", "/=")).get(), is(
            signature("scotch.test.(/=)", fn(var("a"), fn(var("a"), sum("Bool"))))
        ));
    }

    @Test
    public void shouldParseMultipleModulesInSameSource() {
        DefinitionGraph graph = parseInput(
            "module scotch.string",
            "length s = jStrlen s",
            "",
            "module scotch.math",
            "abs n = jAbs n"
        );
        assertThat(graph.getDefinition(rootRef()).get(), is(root(asList(
            moduleRef("scotch.string"),
            moduleRef("scotch.math")
        ))));
    }

    @Test
    public void shouldParseOperatorDefinitions() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "left infix 8 (*), (/), (%)",
            "left infix 7 (+), (-)",
            "prefix 4 not"
        );
        assertThat(graph.getDefinition(operatorRef("scotch.test", "*")).get(), is(operatorDef("scotch.test.(*)", LEFT_INFIX, 8)));
        assertThat(graph.getDefinition(operatorRef("scotch.test", "/")).get(), is(operatorDef("scotch.test.(/)", LEFT_INFIX, 8)));
        assertThat(graph.getDefinition(operatorRef("scotch.test", "%")).get(), is(operatorDef("scotch.test.(%)", LEFT_INFIX, 8)));
        assertThat(graph.getDefinition(operatorRef("scotch.test", "+")).get(), is(operatorDef("scotch.test.(+)", LEFT_INFIX, 7)));
        assertThat(graph.getDefinition(operatorRef("scotch.test", "-")).get(), is(operatorDef("scotch.test.(-)", LEFT_INFIX, 7)));
        assertThat(graph.getDefinition(operatorRef("scotch.test", "not")).get(), is(operatorDef("scotch.test.not", PREFIX, 4)));
    }

    @Test
    public void shouldParseParenthesesAsSeparateMessage() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "value = fn (a b)"
        );
        assertThat(bodyOf(graph.getDefinition(valueRef("scotch.test", "value"))), is(
            message(
                id("fn", t(1)),
                message(id("a", t(2)), id("b", t(3)))
            )
        ));
    }

    @Test
    public void shouldParseSignature() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "length :: String -> Int"
        );
        assertThat(graph.getDefinition(signatureRef("scotch.test", "length")).get(), is(
            signature("scotch.test.length", fn(sum("String"), sum("Int")))
        ));
    }

    @Test
    public void shouldParseValue() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "length :: String -> Int",
            "length s = jStrlen s"
        );
        assertThat(graph.getDefinition(patternRef("scotch.test", "pattern#2")).get(), is(unshuffled(
            "scotch.test.(pattern#2)",
            asList(capture("length", t(0)), capture("s", t(1))),
            message(id("jStrlen", t(3)), id("s", t(4)))
        )));
    }

    @Test
    public void shouldThrowException_whenModuleNameNotTerminatedWithSemicolonOrNewline() {
        expectParseException("Unexpected token COMMA; wanted SEMICOLON");
        parseInput("module scotch.test,");
    }

    @Test
    public void shouldThrowException_whenNotBeginningWithModule() {
        expectParseException("Unexpected token WORD with value 'length'; wanted WORD with value 'module'");
        parseInput("length s = jStrlen s");
    }

    @Test
    public void shouldThrowException_whenOperatorIsMissingPrecedence() {
        expectParseException("Unexpected token WORD; wanted INT");
        parseInput(
            "module scotch.test",
            "left infix =="
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolEnclosedByParensDoesNotContainWord() {
        expectParseException("Unexpected token INT; wanted WORD");
        parseInput(
            "module scotch.test",
            "left infix 7 (42)"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolIsNotWord() {
        expectParseException("Unexpected token BOOL; wanted one of [WORD, LPAREN]");
        parseInput(
            "module scotch.test",
            "left infix 7 True"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolsNotSeparatedByCommas() {
        expectParseException("Unexpected token WORD; wanted SEMICOLON");
        parseInput(
            "module scotch.test",
            "left infix 7 + -"
        );
    }

    @Test
    public void shouldThrowException_whenSignatureHasStuffBetweenNameAndDoubleColon() {
        expectParseException("Unexpected token SEMICOLON; wanted ASSIGN");
        parseInput(
            "module scotch.test",
            "length ; :: String -> Int"
        );
    }

    @Test
    public void shouldParseTypeConstraintInSignature() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "fn :: (Eq a) => a -> a -> Bool"
        );
        assertThat(graph.getValue(signatureRef("scotch.test", "fn")).get(),
            is(fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool")))));
    }

    @Test
    public void shouldParseMultipleConstraintsOnSameVariable() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "fn :: (Eq a, Show a) => a -> a -> Bool"
        );
        assertThat(graph.getValue(signatureRef("scotch.test", "fn")).get(),
            is(fn(var("a", asList("Eq", "Show")), fn(var("a", asList("Eq", "Show")), sum("Bool")))));
    }

    @Test
    public void shouldParseCompoundConstraints() {
        DefinitionGraph graph = parseInput(
            "module scotch.test",
            "fn :: (Eq a, Show b) => a -> b -> Bool"
        );
        assertThat(graph.getValue(signatureRef("scotch.test", "fn")).get(),
            is(fn(var("a", asList("Eq")), fn(var("b", asList("Show")), sum("Bool")))));
    }

    private void expectParseException(String message) {
        exception.expect(ParseException.class);
        exception.expectMessage(containsString(message));
    }
}
