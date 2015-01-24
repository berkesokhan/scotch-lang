package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.Value.Fixity.PREFIX;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.conditional;
import static scotch.compiler.util.TestUtil.constant;
import static scotch.compiler.util.TestUtil.construct;
import static scotch.compiler.util.TestUtil.ctorDef;
import static scotch.compiler.util.TestUtil.dataDef;
import static scotch.compiler.util.TestUtil.dataRef;
import static scotch.compiler.util.TestUtil.field;
import static scotch.compiler.util.TestUtil.fieldDef;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.initializer;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.operatorDef;
import static scotch.compiler.util.TestUtil.operatorRef;
import static scotch.compiler.util.TestUtil.root;
import static scotch.compiler.util.TestUtil.scopeRef;
import static scotch.compiler.util.TestUtil.signatureRef;
import static scotch.compiler.util.TestUtil.unshuffled;

import java.util.List;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.Compiler;
import scotch.compiler.ParserTest;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DataTypeDefinition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.Value;

public class InputParserTest extends ParserTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldParseClassDefinition() {
        parse(
            "module scotch.data.eq",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not x /= y",
            "    x /= y = not x == y"
        );
        shouldHaveClass("scotch.data.eq.Eq", asList(var("a")), asList(
            signatureRef("scotch.data.eq.(==)"),
            signatureRef("scotch.data.eq.(/=)"),
            scopeRef("scotch.data.eq.(#0)"),
            scopeRef("scotch.data.eq.(#1)")
        ));
    }

    @Test
    public void shouldParseMultiValueSignature() {
        parse(
            "module scotch.test",
            "(==), (/=) :: a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.(==)", fn(var("a"), fn(var("a"), sum("Bool"))));
        shouldHaveSignature("scotch.test.(/=)", fn(var("a"), fn(var("a"), sum("Bool"))));
    }

    @Test
    public void shouldParseMultipleModulesInSameSource() {
        parse(
            "module scotch.string",
            "length s = jStrlen s",
            "",
            "module scotch.math",
            "abs n = jAbs n"
        );
        shouldHaveModules(
            "scotch.string",
            "scotch.math"
        );
    }

    @Test
    public void shouldParseOperatorDefinitions() {
        parse(
            "module scotch.test",
            "left infix 8 (*), (/), (%)",
            "left infix 7 (+), (-)",
            "prefix 4 not"
        );
        shouldHaveOperator("scotch.test.(*)", LEFT_INFIX, 8);
        shouldHaveOperator("scotch.test.(/)", LEFT_INFIX, 8);
        shouldHaveOperator("scotch.test.(%)", LEFT_INFIX, 8);
        shouldHaveOperator("scotch.test.(+)", LEFT_INFIX, 7);
        shouldHaveOperator("scotch.test.(-)", LEFT_INFIX, 7);
        shouldHaveOperator("scotch.test.not", PREFIX, 4);
    }

    @Test
    public void shouldParseParenthesesAsSeparateMessage() {
        parse(
            "module scotch.test",
            "value = fn (a b)"
        );
        shouldHaveValue("scotch.test.value", unshuffled(
            id("fn", t(1)),
            unshuffled(id("a", t(2)), id("b", t(3)))
        ));
    }

    @Test
    public void shouldParseSignature() {
        parse(
            "module scotch.test",
            "length :: String -> Int"
        );
        shouldHaveSignature("scotch.test.length", fn(sum("String"), sum("Int")));
    }

    @Test
    public void shouldParseValue() {
        parse(
            "module scotch.test",
            "length :: String -> Int",
            "length s = jStrlen s"
        );
        shouldHavePattern(
            "scotch.test.(#0)",
            asList(capture("length", t(0)), capture("s", t(1))),
            unshuffled(id("jStrlen", t(2)), id("s", t(3)))
        );
    }

    @Test
    public void shouldThrowException_whenModuleNameNotTerminatedWithSemicolonOrNewline() {
        expectParseException("Unexpected COMMA; wanted SEMICOLON");
        parse("module scotch.test,");
    }

    @Test
    public void shouldThrowException_whenNotBeginningWithModule() {
        expectParseException("Unexpected IDENTIFIER with value 'length'; wanted IDENTIFIER with value 'module'");
        parse("length s = jStrlen s");
    }

    @Test
    public void shouldThrowException_whenOperatorIsMissingPrecedence() {
        expectParseException("Unexpected IDENTIFIER; wanted INT_LITERAL");
        parse(
            "module scotch.test",
            "left infix =="
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolEnclosedByParensDoesNotContainWord() {
        expectParseException("Unexpected INT_LITERAL; wanted IDENTIFIER");
        parse(
            "module scotch.test",
            "left infix 7 (42)"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolIsNotWord() {
        expectParseException("Unexpected BOOL_LITERAL; wanted one of [IDENTIFIER, LEFT_PARENTHESIS]");
        parse(
            "module scotch.test",
            "left infix 7 True"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolsNotSeparatedByCommas() {
        expectParseException("Unexpected IDENTIFIER; wanted SEMICOLON");
        parse(
            "module scotch.test",
            "left infix 7 + -"
        );
    }

    @Test
    public void shouldThrowException_whenSignatureHasStuffBetweenNameAndDoubleColon() {
        expectParseException("Unexpected SEMICOLON; wanted ASSIGN");
        parse(
            "module scotch.test",
            "length ; :: String -> Int"
        );
    }

    @Test
    public void shouldParseTypeConstraintInSignature() {
        parse(
            "module scotch.test",
            "fn :: (Eq a) => a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool"))));
    }

    @Test
    public void shouldParseMultipleConstraintsOnSameVariable() {
        parse(
            "module scotch.test",
            "fn :: (Eq a, Show a) => a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq", "Show")), fn(var("a", asList("Eq", "Show")), sum("Bool"))));
    }

    @Test
    public void shouldParseCompoundConstraints() {
        parse(
            "module scotch.test",
            "fn :: (Eq a, Show b) => a -> b -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq")), fn(var("b", asList("Show")), sum("Bool"))));
    }

    @Test
    public void shouldParseFunction1() {
        parse(
            "module scotch.test",
            "id = \\x -> x"
        );
        shouldHaveValue("scotch.test.id", t(0), fn("scotch.test.(id#0)", arg("x", t(1)), id("x", t(2))));
    }

    @Test
    public void shouldParseFunction2() {
        parse(
            "module scotch.test",
            "apply2 = \\x y z -> x y z"
        );
        shouldHaveValue("scotch.test.apply2", t(0), fn(
            "scotch.test.(apply2#0)",
            asList(arg("x", t(1)), arg("y", t(2)), arg("z", t(3))),
            unshuffled(id("x", t(4)), id("y", t(5)), id("z", t(6)))
        ));
    }

    @Test
    public void shouldParseLet() {
        parse(
            "module scotch.test",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldHavePattern("scotch.test.(main#1)", asList(capture("f", t(1)), capture("x", t(2))), unshuffled(id("a", t(3)), id("x", t(4))));
        shouldHavePattern("scotch.test.(main#2)", asList(capture("a", t(5)), capture("g", t(6))), unshuffled(id("g", t(7)), id("+", t(8)), id("g", t(9))));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(scopeRef("scotch.test.(main#1)"), scopeRef("scotch.test.(main#2)")),
            unshuffled(id("f", t(10)), literal(2))
        ));
    }

    @Test
    public void shouldParseLetWithSignature() {
        parse(
            "module scotch.test",
            "main = let",
            "    f :: Int -> Int",
            "    f x = x * x",
            "  f 2"
        );
        shouldHaveSignature("scotch.test.(main#f)", fn(sum("Int"), sum("Int")));
        shouldHavePattern("scotch.test.(main#1)", asList(capture("f", t(1)), capture("x", t(2))), unshuffled(id("x", t(3)), id("*", t(4)), id("x", t(5))));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(signatureRef("scotch.test.(main#f)"), scopeRef("scotch.test.(main#1)")),
            unshuffled(id("f", t(6)), literal(2))
        ));
    }

    @Test
    public void shouldParseNestedLet() {
        parse(
            "module scotch.test",
            "main = let",
            "    f = let",
            "        g = \\x y -> 2 + x + y",
            "      g 3",
            "  f 4"
        );
        shouldHaveValue("scotch.test.(main#f#g)");
    }

    @Test
    public void shouldParseConditional() {
        parse(
            "module scotch.test",
            "really? = if True",
            "          then \"Yes\"",
            "          else \"No\""
        );
        shouldHaveValue("scotch.test.(really?)", conditional(
            literal(true),
            literal("Yes"),
            literal("No"),
            t(1)
        ));
    }

    @Test
    public void shouldParseChainedConditional() {
        parse(
            "module scotch.test",
            "really? = if True then \"Yes\"",
            "          else if Maybe then \"Maybe\"",
            "          else \"Nope\""
        );
        shouldHaveValue("scotch.test.(really?)", conditional(
            literal(true),
            literal("Yes"),
            conditional(
                id("Maybe", t(1)),
                literal("Maybe"),
                literal("Nope"),
                t(2)
            ),
            t(3)
        ));
    }

    @Test
    public void shouldParseNestedConditional() {
        parse(
            "module scotch.test",
            "really? = if True then if False then \"Wat\" else \"Maybe?\"",
            "          else \"Nope\""
        );
        shouldHaveValue("scotch.test.(really?)", conditional(
            literal(true),
            conditional(
                literal(false),
                literal("Wat"),
                literal("Maybe?"),
                t(1)
            ),
            literal("Nope"),
            t(2)
        ));
    }

    @Test
    public void shouldParseInitializer() {
        parse(
            "module scotch.test",
            "toast = Toast {",
            "    type = Rye, butter = Yes, jam = No",
            "}"
        );
        shouldHaveValue("scotch.test.toast", initializer(t(2), id("Toast", t(1)), asList(
            field("type", id("Rye", t(3))),
            field("butter", id("Yes", t(4))),
            field("jam", id("No", t(5)))
        )));
    }

    @Test
    public void shouldParseUnaryDataDeclarationWithNamedFields() {
        parse(
            "module scotch.test",
            "data Toast {",
            "    type Bread,",
            "    butter Verbool,",
            "    jam Verbool",
            "}"
        );
        shouldHaveDataType("scotch.test.Toast", dataDef("scotch.test.Toast", emptyList(), asList(
            ctorDef(
                "scotch.test.Toast",
                "scotch.test.Toast",
                asList(
                    fieldDef("type", sum("Bread")),
                    fieldDef("butter", sum("Verbool")),
                    fieldDef("jam", sum("Verbool")))))));
    }

    @Test
    public void shouldParseDataDeclarationWithAnonymousField() {
        parse(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveDataType("scotch.test.Maybe", dataDef("scotch.test.Maybe", asList(var("a")), asList(
            ctorDef("scotch.test.Maybe", "scotch.test.Nothing"),
            ctorDef(
                "scotch.test.Maybe",
                "scotch.test.Just",
                asList(fieldDef("_0", var("a")))))));
    }

    @Test
    public void shouldParseDataDeclarationWithNamedField() {
        parse(
            "module scotch.test",
            "data Map a b = Empty | Entry { key a, value b }"
        );
        shouldHaveDataType(
            "scotch.test.Map",
            dataDef("scotch.test.Map",
                asList(var("a"), var("b")),
                asList(
                    ctorDef("scotch.test.Map", "scotch.test.Empty"),
                    ctorDef(
                        "scotch.test.Map",
                        "scotch.test.Entry",
                        asList(
                            fieldDef("key", var("a")),
                            fieldDef("value", var("b")))))));
    }

    @Test
    public void shouldCreateDataConstructor() {
        parse(
            "module scotch.test",
            "data Toast {",
            "    type Bread,",
            "    butter Verbool,",
            "    jam Verbool",
            "}"
        );
        shouldHaveValue("scotch.test.Toast", fn(
            "scotch.test.(#0)",
            asList(arg("type", t(0)), arg("butter", t(1)), arg("jam", t(2))),
            construct("scotch.test.Toast", t(3), asList(
                id("type", t(4)),
                id("butter", t(5)),
                id("jam", t(6))))));
    }

    @Test
    public void shouldCreateConstantForNiladicConstructor() {
        parse(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveValue("scotch.test.Nothing", constant("scotch.test.Nothing", "scotch.test.Maybe", t(0)));
    }

    @Test
    public void shouldCreateDataConstructorWithAnonymousFields() {
        parse(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveValue("scotch.test.Just", fn(
            "scotch.test.(#0)",
            asList(arg("_0", t(2))),
            construct("scotch.test.Just", t(3), asList(
                id("_0", t(4))))));
    }

    @Test
    public void shouldParseParenthesizedSignature() {
        parse(
            "module scotch.test",
            "($) :: (a -> b) -> a -> b"
        );
        shouldHaveSignature("scotch.test.($)", fn(fn(var("a"), var("b")), fn(var("a"), var("b"))));
    }

    @Test
    public void shouldParseDataConstructorWithTypeConstraints() {
        parse(
            "module scotch.test",
            "data (Eq a) => List a = Empty | Node a (List a)"
        );
        VariableType var = var("a", asList("Eq"));
        shouldHaveDataType("scotch.test.List", dataDef(
            "scotch.test.List",
            asList(var),
            asList(
                ctorDef("scotch.test.List", "scotch.test.Empty"),
                ctorDef(
                    "scotch.test.List",
                    "scotch.test.Node",
                    asList(
                        fieldDef("_0", var),
                        fieldDef("_1", sum("List", asList(var))))))));
    }

    @Test
    public void shouldParseDataDeclarationWithNamedFieldAndTypeConstraints() {
        parse(
            "module scotch.test",
            "data (Eq a, Eq b) => Map a b = Empty | Entry { key a, value b }"
        );
        shouldHaveDataType(
            "scotch.test.Map",
            dataDef("scotch.test.Map",
                asList(var("a", asList("Eq")), var("b", asList("Eq"))),
                asList(
                    ctorDef("scotch.test.Map", "scotch.test.Empty"),
                    ctorDef(
                        "scotch.test.Map",
                        "scotch.test.Entry",
                        asList(
                            fieldDef("key", var("a", asList("Eq"))),
                            fieldDef("value", var("b", asList("Eq"))))))));
    }

    private void shouldHaveDataType(String name, DataTypeDefinition value) {
        assertThat(graph.getDefinition(dataRef(name)).get(), is(value));
    }

    private void expectParseException(String message) {
        exception.expect(ParseException.class);
        exception.expectMessage(containsString(message));
    }

    private void shouldHaveModules(String... moduleNames) {
        assertThat(graph.getDefinition(rootRef()).get(), is(root(
            stream(moduleNames, 0, moduleNames.length)
                .map(DefinitionReference::moduleRef)
                .collect(toList())
        )));
    }

    private void shouldHaveOperator(String name, Fixity fixity, int precedence) {
        assertThat(graph.getDefinition(operatorRef(name)).get(), is(operatorDef(name, fixity, precedence)));
    }

    private void shouldHavePattern(String name, List<PatternMatch> matches, Value body) {
        assertThat(graph.getDefinition(scopeRef(name)).get(), is(unshuffled(name, matches, body)));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::parseInput;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
