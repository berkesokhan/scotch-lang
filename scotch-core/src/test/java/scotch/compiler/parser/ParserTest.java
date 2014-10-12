package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.ast.Definition.classDef;
import static scotch.compiler.ast.Definition.operatorDef;
import static scotch.compiler.ast.Definition.root;
import static scotch.compiler.ast.Definition.signature;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionReference.classRef;
import static scotch.compiler.ast.DefinitionReference.moduleRef;
import static scotch.compiler.ast.DefinitionReference.opRef;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.DefinitionReference.signatureRef;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.Import.moduleImport;
import static scotch.compiler.ast.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.ast.Operator.Fixity.PREFIX;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatch.equal;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.matcher.Matchers.hasForwardReferences;
import static scotch.compiler.matcher.Matchers.hasImports;
import static scotch.compiler.matcher.Matchers.hasReferences;
import static scotch.compiler.util.TestUtil.bodyOf;
import static scotch.compiler.util.TestUtil.parse;
import static scotch.lang.Type.boolType;
import static scotch.lang.Type.charType;
import static scotch.lang.Type.doubleType;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.intType;
import static scotch.lang.Type.lookup;
import static scotch.lang.Type.stringType;
import static scotch.lang.Type.t;
import static scotch.lang.Type.var;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.SymbolTable;

public class ParserTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void moduleScopeShouldContainForwardReferences() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(moduleRef("scotch.test")), hasForwardReferences("not"));
    }

    @Test
    public void moduleScopeShouldContainReferencedSymbols() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(moduleRef("scotch.test")), hasReferences("not", "/=", "x", "y", "=="));
    }

    @Test
    public void moduleScopeShouldHaveRootAsParent() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(moduleRef("scotch.test")).getParent(), is(symbols.getScope(rootRef())));
    }

    @Test
    public void shouldAssignDifferentTypesToSameSymbolInDifferentScopes() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "left infix 7 (+), (++)",
            "fn0 x = x + 1",
            "fn1 x = x ++ \"something stringy\""
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "fn0"))), equalTo(patterns(
            pattern(asList(capture("x", t(1))), apply(
                apply(id("+", t(2)), id("x", t(1)), t(3)),
                literal(1, intType),
                t(4)
            ))
        )));
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "fn1"))), equalTo(patterns(
            pattern(asList(capture("x", t(6))), apply(
                apply(id("++", t(7)), id("x", t(6)), t(8)),
                literal("something stringy", stringType),
                t(9)
            ))
        )));
    }

    @Test
    public void shouldAssignTypeToForwardReferences() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );

        Scope rootScope = symbols.getScope(rootRef());
        assertThat(rootScope.getValueType("not"), is(t(3)));
        assertThat(rootScope.getValueType("/="), is(t(4)));

        Scope eqScope = symbols.getScope(valueRef("scotch.test", "=="));
        assertThat(eqScope.getValueType("not"), is(t(3)));
        assertThat(eqScope.getValueType("/="), is(t(4)));

        Scope notEqScope = symbols.getScope(valueRef("scotch.test", "/="));
        assertThat(notEqScope.getValueType("not"), is(t(3)));
    }

    @Test
    public void shouldAssignTypesToLiterals() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "string = \"string value\"",
            "int = 42",
            "char = 'a'",
            "double = 3.14",
            "bool = True"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "string")), equalTo(
            value("string", t(0), literal("string value", stringType))
        ));
        assertThat(symbols.getDefinition(valueRef("scotch.test", "int")), equalTo(
            value("int", t(1), literal(42, intType))
        ));
        assertThat(symbols.getDefinition(valueRef("scotch.test", "char")), equalTo(
            value("char", t(2), literal('a', charType))
        ));
        assertThat(symbols.getDefinition(valueRef("scotch.test", "double")), equalTo(
            value("double", t(3), literal(3.14, doubleType))
        ));
        assertThat(symbols.getDefinition(valueRef("scotch.test", "bool")), equalTo(
            value("bool", t(4), literal(true, boolType))
        ));
    }

    @Test
    public void shouldConsolidatePatterns() {
        SymbolTable symbols = parse(
            "module scotch.fn",
            "fib :: Int -> Int",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.fn", "fib")), equalTo(
            value("fib", fn(lookup("Int"), lookup("Int")), patterns(
                pattern(asList(equal(literal(0, intType))), literal(0, intType)),
                pattern(asList(equal(literal(1, intType))), literal(1, intType)),
                pattern(asList(capture("n", t(4))), apply(
                    apply(
                        id("+", t(8)),
                        apply(
                            id("fib", fn(lookup("Int"), lookup("Int"))),
                            apply(apply(id("-", t(5)), id("n", t(4)), t(6)), literal(1, intType), t(7)),
                            t(11)
                        ),
                        t(13)
                    ),
                    apply(
                        id("fib", fn(lookup("Int"), lookup("Int"))),
                        apply(apply(id("-", t(5)), id("n", t(4)), t(9)), literal(2, intType), t(10)),
                        t(12)
                    ),
                    t(14)
                ))
            ))
        ));
    }

    @Test
    public void shouldGivePrecedenceToParentheses() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "value = fn (a b)"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "value"))), equalTo(
            apply(
                id("fn", t(1)),
                apply(id("a", t(2)), id("b", t(3)), t(4)),
                t(5)
            )
        ));
    }

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
        assertThat(symbols.getDefinition(classRef("scotch.data.eq", "Eq")), equalTo(
            classDef("Eq", asList(var("a")), asList(
                signatureRef("scotch.data.eq", "=="),
                signatureRef("scotch.data.eq", "/="),
                valueRef("scotch.data.eq", "=="),
                valueRef("scotch.data.eq", "/=")
            ))
        ));
    }

    @Test
    public void shouldParseImports() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "import scotch.data.eq"
        );
        assertThat(symbols.getScope(moduleRef("scotch.test")), hasImports(moduleImport("scotch.data.eq")));
    }

    @Test
    public void shouldParseLeftInfixOperatorPrecedence() {
        SymbolTable symbols = parse(
            "module scotch.data.eq",
            "left infix 5 (==)",
            "eq x y = x == y"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.data.eq", "eq"))), equalTo(patterns(
            pattern(
                asList(capture("x", t(1)), capture("y", t(2))),
                apply(apply(id("==", t(3)), id("x", t(1)), t(4)), id("y", t(2)), t(5))
            )
        )));
    }

    @Test
    public void shouldParseMultiValueSignature() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "(==), (/=) :: a -> a -> Bool"
        );
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "==")), equalTo(
            signature("==", fn(var("a"), fn(var("a"), lookup("Bool"))))
        ));
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "/=")), equalTo(
            signature("/=", fn(var("a"), fn(var("a"), lookup("Bool"))))
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
        assertThat(symbols.getDefinition(rootRef()), equalTo(root(asList(
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
        assertThat(symbols.getDefinition(opRef("scotch.test", "*")), equalTo(operatorDef("*", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(opRef("scotch.test", "/")), equalTo(operatorDef("/", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(opRef("scotch.test", "%")), equalTo(operatorDef("%", LEFT_INFIX, 8)));
        assertThat(symbols.getDefinition(opRef("scotch.test", "+")), equalTo(operatorDef("+", LEFT_INFIX, 7)));
        assertThat(symbols.getDefinition(opRef("scotch.test", "-")), equalTo(operatorDef("-", LEFT_INFIX, 7)));
        assertThat(symbols.getDefinition(opRef("scotch.test", "not")), equalTo(operatorDef("not", PREFIX, 4)));
    }

    @Test
    public void shouldParseOperatorInParensAsRegularWord() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "left infix 5 (==)",
            "fn x y = (==) x y"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "fn"))), equalTo(patterns(
            pattern(
                asList(capture("x", t(1)), capture("y", t(2))),
                apply(apply(id("==", t(3)), id("x", t(1)), t(4)), id("y", t(2)), t(5))
            )
        )));
    }

    @Test
    public void shouldParsePrefixOperatorPrecedence() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 3 (&&)",
            "ne x y = x && not y"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "ne"))), equalTo(patterns(
            pattern(
                asList(capture("x", t(1)), capture("y", t(2))),
                apply(
                    apply(id("&&", t(3)), id("x", t(1)), t(6)),
                    apply(id("not", t(4)), id("y", t(2)), t(5)),
                    t(7)
                )
            )
        )));
    }

    @Test
    public void shouldParseRightInfixOperatorPrecedence() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "right infix 5 (<<)",
            "fn x y = 3 << x << y"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "fn"))), equalTo(patterns(
            pattern(
                asList(capture("x", t(1)), capture("y", t(2))),
                apply(
                    apply(
                        id("<<", t(3)),
                        literal(3, intType),
                        t(6)
                    ),
                    apply(
                        apply(id("<<", t(3)), id("x", t(1)), t(4)),
                        id("y", t(2)),
                        t(5)
                    ),
                    t(7)
                )
            )
        )));
    }

    @Test
    public void shouldParseSignature() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "length :: String -> Int"
        );
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "length")), equalTo(
            signature("length", fn(lookup("String"), lookup("Int")))
        ));
    }

    @Test
    public void shouldParseValue() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "length :: String -> Int",
            "length s = jStrlen s"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "length")), equalTo(value("length", fn(lookup("String"), lookup("Int")), patterns(
            pattern(asList(capture("s", t(1))), apply(id("jStrlen", t(4)), id("s", t(1)), t(5)))
        ))));
    }

    @Test
    public void shouldShuffleWordsContainingSymbolCharactersAsOperators() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "expression = 2 forward> fn <backward 3"
        );
        assertThat(bodyOf(symbols.getDefinition(valueRef("scotch.test", "expression"))), equalTo(apply(
            apply(
                id("<backward", t(3)),
                apply(
                    apply(id("forward>", t(1)), literal(2, intType), t(4)),
                    id("fn", t(2)),
                    t(5)
                ),
                t(6)
            ),
            literal(3, intType),
            t(7)
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

    @Test
    public void valueScopeShouldContainForwardReferences() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(valueRef("scotch.test", "==")), hasForwardReferences("not", "/="));
    }

    @Test
    public void valueScopeShouldContainReferencedSymbols() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(valueRef("scotch.test", "==")), hasReferences("not", "/=", "x", "y"));
    }

    @Test
    public void valueScopeShouldHaveModuleAsParent() {
        SymbolTable symbols = parse(
            "module scotch.test",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getScope(valueRef("scotch.test", "==")).getParent(), is(symbols.getScope(moduleRef("scotch.test"))));
    }

    private void expectParseException(String message) {
        exception.expect(ParseException.class);
        exception.expectMessage(containsString(message));
    }
}
