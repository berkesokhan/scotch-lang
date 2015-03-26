package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.CodeBlock.ACC_STATIC;
import static me.qmx.jitescript.util.CodegenUtils.ci;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.conditional;
import static scotch.compiler.util.TestUtil.constantRef;
import static scotch.compiler.util.TestUtil.constantValue;
import static scotch.compiler.util.TestUtil.construct;
import static scotch.compiler.util.TestUtil.ctorDef;
import static scotch.compiler.util.TestUtil.dataDef;
import static scotch.compiler.util.TestUtil.dataRef;
import static scotch.compiler.util.TestUtil.field;
import static scotch.compiler.util.TestUtil.fieldDef;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.ignore;
import static scotch.compiler.util.TestUtil.initializer;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.matcher;
import static scotch.compiler.util.TestUtil.operatorDef;
import static scotch.compiler.util.TestUtil.operatorRef;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.root;
import static scotch.compiler.util.TestUtil.scopeRef;
import static scotch.compiler.util.TestUtil.signatureRef;
import static scotch.compiler.util.TestUtil.unshuffled;
import static scotch.compiler.util.TestUtil.unshuffledMatch;
import static scotch.control.monad.Monad.fail;
import static scotch.symbol.FieldSignature.fieldSignature;
import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.symbol.Value.Fixity.PREFIX;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.t;
import static scotch.symbol.type.Types.var;

import java.util.List;
import java.util.function.Function;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DataTypeDefinition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.pattern.PatternMatch;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.util.TestUtil;
import scotch.runtime.Callable;
import scotch.symbol.Value.Fixity;
import scotch.symbol.type.VariableType;

public class InputParserTest extends IsolatedCompilerTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldParseClassDefinition() {
        compile(
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
        compile(
            "module scotch.test",
            "(==), (/=) :: a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.(==)", fn(var("a"), fn(var("a"), sum("Bool"))));
        shouldHaveSignature("scotch.test.(/=)", fn(var("a"), fn(var("a"), sum("Bool"))));
    }

    @Test
    public void shouldParseMultipleModulesInSameSource() {
        compile(
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
        compile(
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
        compile(
            "module scotch.test",
            "value = fn (a b)"
        );
        shouldHaveValue("scotch.test.value", unshuffled(
            id("fn", t(0)),
            unshuffled(id("a", t(1)), id("b", t(2)))
        ));
    }

    @Test
    public void shouldParseSignature() {
        compile(
            "module scotch.test",
            "length :: String -> Int"
        );
        shouldHaveSignature("scotch.test.length", fn(sum("String"), sum("Int")));
    }

    @Test
    public void shouldParseValue() {
        compile(
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
        compile("module scotch.test,");
    }

    @Test
    public void shouldThrowException_whenNotBeginningWithModule() {
        expectParseException("Unexpected IDENTIFIER with value 'length'; wanted IDENTIFIER with value 'module'");
        compile("length s = jStrlen s");
    }

    @Test
    public void shouldThrowException_whenOperatorIsMissingPrecedence() {
        expectParseException("Unexpected IDENTIFIER; wanted INT_LITERAL");
        compile(
            "module scotch.test",
            "left infix =="
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolEnclosedByParensDoesNotContainWord() {
        expectParseException("Unexpected INT_LITERAL; wanted IDENTIFIER");
        compile(
            "module scotch.test",
            "left infix 7 (42)"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolIsNotWord() {
        expectParseException("Unexpected BOOL_LITERAL; wanted one of [IDENTIFIER, LEFT_PARENTHESIS]");
        compile(
            "module scotch.test",
            "left infix 7 True"
        );
    }

    @Test
    public void shouldThrowException_whenOperatorSymbolsNotSeparatedByCommas() {
        expectParseException("Unexpected IDENTIFIER; wanted SEMICOLON");
        compile(
            "module scotch.test",
            "left infix 7 + -"
        );
    }

    @Test
    public void shouldThrowException_whenSignatureHasStuffBetweenNameAndDoubleColon() {
        expectParseException("Unexpected SEMICOLON; wanted ASSIGN");
        compile(
            "module scotch.test",
            "length ; :: String -> Int"
        );
    }

    @Test
    public void shouldParseTypeConstraintInSignature() {
        compile(
            "module scotch.test",
            "fn :: (Eq a) => a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool"))));
    }

    @Test
    public void shouldParseMultipleConstraintsOnSameVariable() {
        compile(
            "module scotch.test",
            "fn :: (Eq a, Show a) => a -> a -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq", "Show")), fn(var("a", asList("Eq", "Show")), sum("Bool"))));
    }

    @Test
    public void shouldParseCompoundConstraints() {
        compile(
            "module scotch.test",
            "fn :: (Eq a, Show b) => a -> b -> Bool"
        );
        shouldHaveSignature("scotch.test.fn", fn(var("a", asList("Eq")), fn(var("b", asList("Show")), sum("Bool"))));
    }

    @Test
    public void shouldParseCapturingPatternLiteral1() {
        compile(
            "module scotch.test",
            "id = \\x -> x"
        );
        shouldHaveValue("scotch.test.id", matcher("scotch.test.(id#0)", t(0), arg("#0", t(2)),
            pattern("scotch.test.(id#0#0)", asList(capture("x", t(1))), unshuffled(id("x", t(3))))));
    }

    @Test
    public void shouldParseCapturingPatternLiteral2() {
        compile(
            "module scotch.test",
            "apply2 = \\x y z -> x y z"
        );
        shouldHaveValue("scotch.test.apply2", matcher("scotch.test.(apply2#0)", t(0), asList(arg("#0", t(4)), arg("#1", t(5)), arg("#2", t(6))), pattern(
            "scotch.test.(apply2#0#0)",
            asList(capture("x", t(1)), capture("y", t(2)), capture("z", t(3))),
            unshuffled(id("x", t(7)), id("y", t(8)), id("z", t(9)))
        )));
    }

    @Test
    public void shouldNotParseEqualsPatternLiteral() {
        exception.expect(ParseException.class);
        exception.expectMessage(containsString("wanted IDENTIFIER [test://shouldNotParseEqualsPatternLiteral (2, 11), (2, 12)]"));
        compile(
            "module scotch.test",
            "apply2 = \\1 y z = y z"
        );
    }

    @Test
    public void shouldParseIgnoredPatternLiteral() {
        compile(
            "module scotch.test",
            "fn = \\_ -> ignored"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", matcher("scotch.test.(fn#0)", t(0), arg("#0", t(2)), pattern(
            "scotch.test.(fn#0#0)", asList(ignore(t(1))), unshuffled(id("ignored", t(3)))
        )));
    }

    @Test
    public void shouldParseLet() {
        compile(
            "module scotch.test",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldHavePattern("scotch.test.(main#1)", asList(capture("f", t(0)), capture("x", t(1))), unshuffled(id("a", t(2)), id("x", t(3))));
        shouldHavePattern("scotch.test.(main#2)", asList(capture("a", t(4)), capture("g", t(5))), unshuffled(id("g", t(6)), id("+", t(7)), id("g", t(8))));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(scopeRef("scotch.test.(main#1)"), scopeRef("scotch.test.(main#2)")),
            unshuffled(id("f", t(9)), literal(2))
        ));
    }

    @Test
    public void shouldParseLetWithSignature() {
        compile(
            "module scotch.test",
            "main = let",
            "    f :: Int -> Int",
            "    f x = x * x",
            "  f 2"
        );
        shouldHaveSignature("scotch.test.(main#f)", fn(sum("Int"), sum("Int")));
        shouldHavePattern("scotch.test.(main#1)", asList(capture("f", t(0)), capture("x", t(1))), unshuffled(id("x", t(2)), id("*", t(3)), id("x", t(4))));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(signatureRef("scotch.test.(main#f)"), scopeRef("scotch.test.(main#1)")),
            unshuffled(id("f", t(5)), literal(2))
        ));
    }

    @Test
    public void shouldParseNestedLet() {
        compile(
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
        compile(
            "module scotch.test",
            "really? = if True",
            "          then \"Yes\"",
            "          else \"No\""
        );
        shouldHaveValue("scotch.test.(really?)", conditional(
            literal(true),
            literal("Yes"),
            literal("No"),
            t(0)
        ));
    }

    @Test
    public void shouldParseChainedConditional() {
        compile(
            "module scotch.test",
            "really? = if True then \"Yes\"",
            "          else if Maybe then \"Maybe\"",
            "          else \"Nope\""
        );
        shouldHaveValue("scotch.test.(really?)", conditional(
            literal(true),
            literal("Yes"),
            conditional(
                id("Maybe", t(0)),
                literal("Maybe"),
                literal("Nope"),
                t(1)
            ),
            t(2)
        ));
    }

    @Test
    public void shouldParseNestedConditional() {
        compile(
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
                t(0)
            ),
            literal("Nope"),
            t(1)
        ));
    }

    @Test
    public void shouldParseInitializer() {
        compile(
            "module scotch.test",
            "toast = Toast {",
            "    type = Rye, butter = Yes, jam = No",
            "}"
        );
        shouldHaveValue("scotch.test.toast", initializer(t(1), id("Toast", t(0)), asList(
            field("type", id("Rye", t(2))),
            field("butter", id("Yes", t(3))),
            field("jam", id("No", t(4)))
        )));
    }

    @Test
    public void shouldParseUnaryDataDeclarationWithNamedFields() {
        compile(
            "module scotch.test",
            "data Toast {",
            "    type :: Bread,",
            "    butter :: Verbool,",
            "    jam :: Verbool",
            "}"
        );
        shouldHaveDataType("scotch.test.Toast", dataDef("scotch.test.Toast", emptyList(), asList(
            ctorDef(
                0, "scotch.test.Toast",
                "scotch.test.Toast",
                asList(
                    fieldDef(0, "type", sum("Bread")),
                    fieldDef(1, "butter", sum("Verbool")),
                    fieldDef(2, "jam", sum("Verbool")))))));
    }

    @Test
    public void shouldParseDataDeclarationWithAnonymousField() {
        compile(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveDataType("scotch.test.Maybe", dataDef("scotch.test.Maybe", asList(var("a")), asList(
            ctorDef(0, "scotch.test.Maybe", "scotch.test.Nothing"),
            ctorDef(
                1, "scotch.test.Maybe",
                "scotch.test.Just",
                asList(fieldDef(0, "_0", var("a")))))));
    }

    @Test
    public void shouldParseDataDeclarationWithNamedField() {
        compile(
            "module scotch.test",
            "data Map a b = Empty | Entry { key :: a, value :: b }"
        );
        shouldHaveDataType(
            "scotch.test.Map",
            dataDef("scotch.test.Map",
                asList(var("a"), var("b")),
                asList(
                    ctorDef(0, "scotch.test.Map", "scotch.test.Empty"),
                    ctorDef(
                        1, "scotch.test.Map",
                        "scotch.test.Entry",
                        asList(
                            fieldDef(0, "key", var("a")),
                            fieldDef(1, "value", var("b")))))));
    }

    @Test
    public void shouldCreateDataConstructor() {
        compile(
            "module scotch.test",
            "data Toast {",
            "    type :: Bread,",
            "    butter :: Verbool,",
            "    jam :: Verbool",
            "}"
        );
        shouldHaveValue("scotch.test.Toast", fn(
            "scotch.test.(#0)",
            asList(arg("type", sum("Bread")), arg("butter", sum("Verbool")), arg("jam", sum("Verbool"))),
            construct("scotch.test.Toast", sum("scotch.test.Toast"), asList(
                id("type", sum("Bread")),
                id("butter", sum("Verbool")),
                id("jam", sum("Verbool"))))));
    }

    @Test
    public void shouldCreateConstantForNiladicConstructor() {
        compile(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveValue("scotch.test.Nothing", constantRef(
            "scotch.test.Nothing",
            "scotch.test.Maybe",
            fieldSignature("scotch/test/Maybe$Nothing", ACC_STATIC | ACC_PUBLIC | ACC_FINAL, "INSTANCE", ci(Callable.class)),
            sum("scotch.test.Maybe", var("a"))
        ));
    }

    @Test
    public void shouldCreateDataConstructorWithAnonymousFields() {
        compile(
            "module scotch.test",
            "data Maybe a = Nothing | Just a"
        );
        shouldHaveValue("scotch.test.Just", fn(
            "scotch.test.(#0)",
            asList(arg("_0", var("a"))),
            construct("scotch.test.Just", sum("scotch.test.Maybe", var("a")), asList(
                id("_0", var("a"))))));
    }

    @Test
    public void shouldParseParenthesizedSignature() {
        compile(
            "module scotch.test",
            "($) :: (a -> b) -> a -> b"
        );
        shouldHaveSignature("scotch.test.($)", fn(fn(var("a"), var("b")), fn(var("a"), var("b"))));
    }

    @Test
    public void shouldParseDataConstructorWithTypeConstraints() {
        compile(
            "module scotch.test",
            "data (Eq a) => List a = Empty | Node a (List a)"
        );
        VariableType var = var("a", asList("Eq"));
        shouldHaveDataType("scotch.test.List", dataDef(
            "scotch.test.List",
            asList(var),
            asList(
                ctorDef(0, "scotch.test.List", "scotch.test.Empty"),
                ctorDef(
                    1, "scotch.test.List",
                    "scotch.test.Node",
                    asList(
                        fieldDef(0, "_0", var),
                        fieldDef(1, "_1", sum("List", asList(var))))))));
    }

    @Test
    public void shouldParseDataDeclarationWithNamedFieldAndTypeConstraints() {
        compile(
            "module scotch.test",
            "data (Eq a, Eq b) => Map a b = Empty | Entry { key :: a, value :: b }"
        );
        shouldHaveDataType(
            "scotch.test.Map",
            dataDef("scotch.test.Map",
                asList(var("a", asList("Eq")), var("b", asList("Eq"))),
                asList(
                    ctorDef(0, "scotch.test.Map", "scotch.test.Empty"),
                    ctorDef(
                        1, "scotch.test.Map",
                        "scotch.test.Entry",
                        asList(
                            fieldDef(0, "key", var("a", asList("Eq"))),
                            fieldDef(1, "value", var("b", asList("Eq"))))))));
    }

    @Test
    public void shouldParseDot() {
        compile(
            "module scotch.test",
            "val = (f . g) x"
        );
        shouldHaveValue("scotch.test.val", unshuffled(
            unshuffled(
                id("f", t(0)),
                id(".", t(1)),
                id("g", t(2))
            ),
            id("x", t(5))
        ));
    }

    @Test
    public void shouldParseDoNotationWithThen() {
        compile(
            "module scotch.test",
            "messaged = do",
            "    println \"Hello World!\"",
            "    println \"Debilitating coffee addiction\""
        );
        shouldHaveValue("scotch.test.messaged", unshuffled(
            unshuffled(id("println", t(0)), literal("Hello World!")),
            id("scotch.control.monad.(>>)", t(2)),
            unshuffled(id("println", t(1)), literal("Debilitating coffee addiction"))
        ));
    }

    @Test
    public void shouldParseDoNotationWithBind() {
        compile(
            "module scotch.test",
            "pingpong = do",
            "    ping <- readln",
            "    println (\"ponging back! \" ++ ping)"
        );
        shouldHaveValue("scotch.test.pingpong", unshuffled(
            unshuffled(id("readln", t(1))),
            id("scotch.control.monad.(>>=)", t(7)),
            fn("scotch.test.(pingpong#0)", arg("ping", t(0)), unshuffled(
                id("println", t(2)),
                unshuffled(literal("ponging back! "), id("++", t(3)), id("ping", t(4)))
            ))
        ));
    }

    @Test
    public void shouldThrow_whenBindIsLastLineInDoNotation() {
        exception.expectMessage(containsString("Unexpected bind in do-notation"));
        compile(
            "module scotch.test",
            "pingpong = do",
            "    ping <- readln",
            "    println (\"ponging back! \" ++ ping)",
            "    pong <- readln"
        );
        fail();
    }

    @Test
    public void shouldParseTupleLiteral() {
        compile(
            "module scotch.test",
            "tuple = (1, 2, 3)"
        );
        shouldHaveValue("scotch.test.tuple", initializer(t(0), id("scotch.data.tuple.(,,)", t(1)), asList(
            field("_0", unshuffled(literal(1))),
            field("_1", unshuffled(literal(2))),
            field("_2", unshuffled(literal(3)))
        )));
    }

    @Test
    public void shouldThrow_whenTupleHasTooManyMembers() {
        exception.expectMessage(containsString("Tuple can't have more than 24 members"));
        compile(
            "module scotch.test",
            "tuple = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25)"
        );
    }

    @Test
    public void shouldParseListLiteral() {
        compile(
            "module scotch.test",
            "list = [1, 2]"
        );
        shouldHaveValue("scotch.test.list", initializer(t(3), id("scotch.data.list.(:)", t(4)), asList(
            field("_0", unshuffled(literal(1))),
            field("_1", initializer(t(1), id("scotch.data.list.(:)", t(2)), asList(
                field("_0", unshuffled(literal(2))),
                field("_1", constantValue("scotch.data.list.[]", "scotch.data.list.[]", t(0)))
            )))
        )));
    }

    @Test
    public void shouldParseListLiteralWithTrailingComma() {
        compile(
            "module scotch.test",
            "list = [1, 2,]"
        );
        shouldHaveValue("scotch.test.list", initializer(t(3), id("scotch.data.list.(:)", t(4)), asList(
            field("_0", unshuffled(literal(1))),
            field("_1", initializer(t(1), id("scotch.data.list.(:)", t(2)), asList(
                field("_0", unshuffled(literal(2))),
                field("_1", constantValue("scotch.data.list.[]", "scotch.data.list.[]", t(0)))
            )))
        )));
    }

    @Test
    public void shouldParseTupleDestructuringPattern() {
        compile(
            "module scotch.test",
            "second (_, b) = b"
        );
        shouldHavePattern("scotch.test.(#0)",
            asList(capture("second", t(0)), TestUtil.tuple("scotch.data.tuple.(,)", t(5), asList(
                TestUtil.field(t(6), ignore(t(2))),
                TestUtil.field(t(7), capture("b", t(4)))))),
            unshuffled(id("b", t(8)))
        );
    }

    @Test
    public void shouldParseListDestructuringPattern() {
        compile(
            "module scotch.test",
            "tail (_:xs) = xs"
        );
        shouldHavePattern("scotch.test.(#0)",
            asList(capture("tail", t(0)), unshuffledMatch(t(1), ignore(t(2)), capture(":", t(3)), capture("xs", t(4)))),
            unshuffled(id("xs", t(6))));
    }

    @Test
    public void shouldParseParenthesizedCapturePattern() {
        compile(
            "module scotch.test",
            "second (_, (b)) = b"
        );
        shouldHavePattern("scotch.test.(#0)",
            asList(capture("second", t(0)), TestUtil.tuple("scotch.data.tuple.(,)", t(7), asList(
                TestUtil.field(t(8), ignore(t(2))),
                TestUtil.field(t(9), capture("b", t(5)))))),
            unshuffled(id("b", t(10)))
        );
    }

    @Test
    public void shouldParseSecondSecond() {
        compile(
            "module scotch.test",
            "secondSecond (_, (_, b)) = b"
        );
        shouldHavePattern("scotch.test.(#0)",
            asList(capture("secondSecond", t(0)), TestUtil.tuple("scotch.data.tuple.(,)", t(11), asList(
                TestUtil.field(t(12), ignore(t(2))),
                TestUtil.field(t(13), TestUtil.tuple("scotch.data.tuple.(,)", t(8), asList(
                    TestUtil.field(t(9), ignore(t(5))),
                    TestUtil.field(t(10), capture("b", t(7))))))))),
            unshuffled(id("b", t(14)))
        );
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
    protected Function<Compiler, DefinitionGraph> compile() {
        return Compiler::parseInput;
    }
}
