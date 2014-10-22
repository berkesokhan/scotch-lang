package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.ast.Definition.classDef;
import static scotch.compiler.ast.Definition.operatorDef;
import static scotch.compiler.ast.Definition.root;
import static scotch.compiler.ast.Definition.signature;
import static scotch.compiler.ast.Definition.unshuffled;
import static scotch.compiler.ast.DefinitionReference.classRef;
import static scotch.compiler.ast.DefinitionReference.moduleRef;
import static scotch.compiler.ast.DefinitionReference.operatorRef;
import static scotch.compiler.ast.DefinitionReference.patternRef;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.DefinitionReference.signatureRef;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.ast.Operator.Fixity.PREFIX;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.message;
import static scotch.compiler.util.TestUtil.bodyOf;
import static scotch.compiler.util.TestUtil.parse;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.sum;
import static scotch.lang.Type.t;
import static scotch.lang.Type.var;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.ast.SymbolTable;

public class ParserTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldParseClassDefinition() {
        SymbolTable symbols = parse(
            "module scotch.data.eq",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not x /= y",
            "    x /= y = not x == y"
        );
        assertThat(symbols.getDefinition(classRef("scotch.data.eq", "Eq")), is(
            classDef("Eq", asList(var("a")), asList(
                signatureRef("scotch.data.eq", "=="),
                signatureRef("scotch.data.eq", "/="),
                patternRef("scotch.data.eq", "pattern#3"),
                patternRef("scotch.data.eq", "pattern#11")
            ))
        ));
    }

    @Test
    public void shouldParseMultiValueSignature() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "(==), (/=) :: a -> a -> Bool"
        );
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "==")), is(
            signature("scotch.test.(==)", fn(var("a"), fn(var("a"), sum("Bool"))))
        ));
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "/=")), is(
            signature("scotch.test.(/=)", fn(var("a"), fn(var("a"), sum("Bool"))))
        ));
    }

    @Test
    public void shouldParseMultipleModulesInSameSource() {
        SymbolTable symbols = parse(
            "module scotch.string",
            "length s = jStrlen s",
            "",
            "module scotch.math",
            "abs n = jAbs n"
        );
        assertThat(symbols.getDefinition(rootRef()), is(root(asList(
            moduleRef("scotch.string"),
            moduleRef("scotch.math")
        ))));
    }

    @Test
    public void shouldParseOperatorDefinitions() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "left infix 8 (*), (/), (%)",
            "left infix 7 (+), (-)",
            "prefix 4 not"
        );
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "*")), is(operatorDef("scotch.test.(*)", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "/")), is(operatorDef("scotch.test.(/)", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "%")), is(operatorDef("scotch.test.(%)", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "+")), is(operatorDef("scotch.test.(+)", LEFT_INFIX, 7)));
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "-")), is(operatorDef("scotch.test.(-)", LEFT_INFIX, 7)));
        assertThat(symbols.getDefinition(operatorRef("scotch.test", "not")), is(operatorDef("scotch.test.not", PREFIX, 4)));
    }

    @Test
    public void shouldParseParenthesesAsSeparateMessage() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "value = fn (a b)"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "value"))), is(
            message(
                id("fn", t(1)),
                message(id("a", t(2)), id("b", t(3)))
            )
        ));
    }

    @Test
    public void shouldParseSignature() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "length :: String -> Int"
        );
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "length")), is(
            signature("scotch.test.length", fn(sum("String"), sum("Int")))
        ));
    }

    @Test
    public void shouldParseValue() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "length :: String -> Int",
            "length s = jStrlen s"
        );
        assertThat(symbols.getDefinition(patternRef("scotch.test", "pattern#2")), is(unshuffled(
            "scotch.test.(pattern#2)",
            pattern(asList(capture("length", t(0)), capture("s", t(1))), message(id("jStrlen", t(3)), id("s", t(4))))
        )));
    }

    @Test
    public void shouldThrowException_whenModuleNameNotTerminatedWithSemicolonOrNewline() {
        expectParseException("Unexpected token COMMA; wanted SEMICOLON");
        parse("module scotch.test,");
    }

    @Test
    public void shouldThrowException_whenNotBeginningWithModule() {
        expectParseException("Unexpected token WORD with value 'length'; wanted WORD with value 'module'");
        parse("length s = jStrlen s");
    }

    @Test
    public void shouldThrowException_whenOperatorIsMissingPrecedence() {
        expectParseException("Unexpected token WORD; wanted INT");
        parse(
            "module scotch.test",
            "left infix =="
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolEnclosedByParensDoesNotContainWord() {
        expectParseException("Unexpected token INT; wanted WORD");
        parse(
            "module scotch.test",
            "left infix 7 (42)"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolIsNotWord() {
        expectParseException("Unexpected token BOOL; wanted one of [WORD, LPAREN]");
        parse(
            "module scotch.test",
            "left infix 7 True"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolsNotSeparatedByCommas() {
        expectParseException("Unexpected token WORD; wanted SEMICOLON");
        parse(
            "module scotch.test",
            "left infix 7 + -"
        );
    }

    @Test
    public void shouldThrowException_whenSignatureHasStuffBetweenNameAndDoubleColon() {
        expectParseException("Unexpected token SEMICOLON; wanted ASSIGN");
        parse(
            "module scotch.test",
            "length ; :: String -> Int"
        );
    }

    private void expectParseException(String message) {
        exception.expect(ParseException.class);
        exception.expectMessage(containsString(message));
    }
}
