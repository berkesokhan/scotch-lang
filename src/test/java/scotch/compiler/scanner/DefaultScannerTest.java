package scotch.compiler.scanner;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static scotch.compiler.scanner.Token.TokenKind.ARROW;
import static scotch.compiler.scanner.Token.TokenKind.BACKSLASH;
import static scotch.compiler.scanner.Token.TokenKind.CHAR_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.COMMA;
import static scotch.compiler.scanner.Token.TokenKind.DEFAULT_OPERATOR;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_ARROW;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_COLON;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.IDENTIFIER;
import static scotch.compiler.scanner.Token.TokenKind.INT_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_IN;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_LET;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_MATCH;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_ON;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_WHERE;
import static scotch.compiler.scanner.Token.TokenKind.LEFT_CURLY_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.LEFT_SQUARE_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.NEWLINE;
import static scotch.compiler.scanner.Token.TokenKind.PIPE;
import static scotch.compiler.scanner.Token.TokenKind.RIGHT_CURLY_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.RIGHT_SQUARE_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.STRING_LITERAL;
import static scotch.compiler.scanner.Token.token;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.token;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultScannerTest {

    @Rule
    public final ExpectedException exception = none();

    @Test
    public void dotInParensIsIdentifier() {
        assertThat(secondFrom("(.)"), equalTo(token(IDENTIFIER, ".")));
    }

    @Test
    public void dotPrecededAndFollowedByWhitespaceIsIdentifier() {
        assertThat(secondFrom("first . second"), equalTo(token(IDENTIFIER, ".")));
    }

    @Test
    public void shouldGetArgumentAfterLambdaPrefix() {
        assertThat(secondFrom("\\x -> y"), equalTo(token(IDENTIFIER, "x", source("test", point(1, 1, 2), point(2, 1, 3)))));
    }

    @Test
    public void shouldGetArrow() {
        assertThat(firstFrom("-> arrow?"), equalTo(token(ARROW, "->", source("test", point(0, 1, 1), point(2, 1, 3)))));
    }

    @Test
    public void shouldGetAt() {
        assertThat(firstFrom("@atom"), equalTo(token(IDENTIFIER, "@")));
    }

    @Test
    public void shouldGetColon() {
        assertThat(firstFrom(": test"), equalTo(token(IDENTIFIER, ":")));
    }

    @Test
    public void shouldGetComma() {
        assertThat(firstFrom(", comma"), equalTo(token(COMMA, ",")));
    }

    @Test
    public void shouldGetDouble() {
        assertThat(firstFrom("123.4"), equalTo(token(DOUBLE_LITERAL, 123.4)));
    }

    @Test
    public void shouldGetDoubleArrow() {
        assertThat(firstFrom("=> (Eq a)"), equalTo(token(DOUBLE_ARROW, "=>")));
    }

    @Test
    public void shouldGetDoubleColon() {
        assertThat(firstFrom(":: signature"), equalTo(token(DOUBLE_COLON, "::")));
    }

    @Test
    public void shouldGetIdentifier() {
        assertThat(firstFrom("asteroids yo"), equalTo(token(IDENTIFIER, "asteroids", source("test", point(0, 1, 1), point(9, 1, 10)))));
    }

    @Test
    public void shouldGetIdentifierForSquare() {
        assertThat(firstFrom("[]list"), equalTo(token(IDENTIFIER, "[]")));
    }

    @Test
    public void shouldGetIdentifierSuffixedWithQuote() {
        assertThat(firstFrom("asteroids' again"), equalTo(token(IDENTIFIER, "asteroids'", source("test", point(0, 1, 1), point(10, 1, 11)))));
    }

    @Test
    public void shouldGetIdentifier_whenNumbersFollowedByName() {
        assertThat(firstFrom("123four"), equalTo(token(IDENTIFIER, "123four")));
    }

    @Test
    public void shouldGetIn() {
        assertThat(firstFrom("in there"), equalTo(token(KEYWORD_IN, "in")));
    }

    @Test
    public void shouldGetInt_whenFollowedByDotName() {
        assertThat(firstFrom("123.four"), equalTo(token(INT_LITERAL, 123)));
    }

    @Test
    public void shouldGetInt_whenFollowedByDotNumberName() {
        assertThat(firstFrom("123.4five"), equalTo(token(INT_LITERAL, 123)));
    }

    @Test
    public void shouldGetLambdaPrefix() {
        assertThat(secondFrom("asteroids \\x -> boom!"), equalTo(token(BACKSLASH, "\\", source("test", point(10, 1, 11), point(11, 1, 12)))));
    }

    @Test
    public void shouldGetLeftCurly() {
        assertThat(firstFrom("{ left"), equalTo(token(LEFT_CURLY_BRACE, "{")));
    }

    @Test
    public void shouldGetLeftSquare() {
        assertThat(firstFrom("[lsquare"), equalTo(token(LEFT_SQUARE_BRACE, "[")));
    }

    @Test
    public void shouldGetMatch() {
        assertThat(firstFrom("match this"), equalTo(token(KEYWORD_MATCH, "match")));
    }

    @Test
    public void shouldGetNewLine() {
        assertThat(secondFrom("line1", "line2"), equalTo(token(NEWLINE, "\n")));
    }

    @Test
    public void shouldGetOn() {
        assertThat(firstFrom("on that"), equalTo(token(KEYWORD_ON, "on")));
    }

    @Test
    public void shouldGetPipe() {
        assertThat(firstFrom("| BreakfastItem"), equalTo(token(PIPE, "|")));
    }

    @Test
    public void shouldGetQuotedIdentifier() {
        assertThat(firstFrom("`comet`"), equalTo(token(DEFAULT_OPERATOR, "comet")));
    }

    @Test
    public void shouldGetRightCurly() {
        assertThat(firstFrom("} right"), equalTo(token(RIGHT_CURLY_BRACE, "}")));
    }

    @Test
    public void shouldGetRightSquare() {
        assertThat(firstFrom("]rsquare"), equalTo(token(RIGHT_SQUARE_BRACE, "]")));
    }

    @Test
    public void shouldGetTuple3() {
        assertThat(firstFrom("(,,)"), equalTo(token(IDENTIFIER, "(,,)")));
    }

    @Test
    public void shouldGetUnit() {
        assertThat(firstFrom("()"), equalTo(token(IDENTIFIER, "()")));
    }

    @Test
    public void shouldGetWhere() {
        assertThat(firstFrom("where stuff"), equalTo(token(KEYWORD_WHERE, "where")));
    }

    @Test
    public void shouldNestMultiLineComment() {
        Token token = secondFrom(
            "first /*",
            "  this is ignored",
            "  and /* this is ignored */ too",
            "*/ second"
        );
        assertThat(token, equalTo(token(IDENTIFIER, "second")));
    }

    @Test
    public void shouldReportBadHexEscape() {
        exception.expect(ScanException.class);
        exception.expectMessage(containsString("Invalid hex escape character 'V' ['test' (1, 10)]"));
        firstFrom("\"oops \\u0V12 busted\"");
    }

    @Test
    public void shouldScanCharLiteral() {
        assertThat(firstFrom("'a'"), equalTo(token(CHAR_LITERAL, 'a')));
    }

    @Test
    public void shouldScanCharLiteralWithEscape() {
        assertThat(firstFrom("'\\t'"), equalTo(token(CHAR_LITERAL, '\t')));
    }

    @Test
    public void shouldScanString() {
        Scanner scanner = scan("\"this is a string\"");
        assertThat(scanner.nextToken(), equalTo(token(STRING_LITERAL, "this is a string")));
    }

    @Test
    public void shouldScanStringWithEscape() {
        Scanner scanner = scan("\"this\\tis\\ta\\nstring\"");
        assertThat(scanner.nextToken(), equalTo(token(STRING_LITERAL, "this\tis\ta\nstring")));
    }

    @Test
    public void shouldScanStringWithHexEscape() {
        assertThat(firstFrom("\"this should be '\\u0040'\""), equalTo(token(STRING_LITERAL, "this should be '@'")));
    }

    @Test
    public void shouldScanStringWithOctalEscape() {
        assertThat(firstFrom("\"octal: '\\100'\""), equalTo(token(STRING_LITERAL, "octal: '@'")));
    }

    @Test
    public void shouldSkipLineComment() {
        Token token = firstFrom(
            " // this is a comment",
            "rocks"
        );
        assertThat(token, equalTo(token(IDENTIFIER, "rocks")));
    }

    @Test
    public void shouldSkipMultiLineComment() {
        Token token = secondFrom(
            "first /*",
            " ignore all this stuff",
            "*/ second"
        );
        assertThat(token, equalTo(token(IDENTIFIER, "second")));
    }

    @Test
    public void shouldThrowException_whenCharLiteralIsEmpty() {
        exception.expect(ScanException.class);
        exception.expectMessage("Empty char literal ['test' (1, 2)]");
        firstFrom("''");
    }

    @Test
    public void shouldThrowException_whenCharLiteralIsTooLong() {
        exception.expect(ScanException.class);
        exception.expectMessage("Unterminated character literal: unexpected <LATIN SMALL LETTER B> 'b' ['test' (1, 3)]");
        firstFrom("'ab'");
    }

    @Test
    public void shouldThrowException_whenQuotingReservedWord() {
        exception.expect(ScanException.class);
        exception.expectMessage(containsString("Cannot quote reserved word 'else' ['test' (1, 2)]"));
        firstFrom(" `else`");
    }

    @Test
    public void shouldGetLet() {
        assertThat(firstFrom("let"), is(token(KEYWORD_LET, "let")));
    }

    private Token firstFrom(String... data) {
        return nthFrom(0, data);
    }

    private Token nthFrom(int offset, String... data) {
        Scanner scanner = scan(data);
        for (int i = 0; i < offset; i++) {
            scanner.nextToken();
        }
        return scanner.nextToken();
    }

    private Scanner scan(String... data) {
        return new DefaultScanner("test", (join(lineSeparator(), data) + lineSeparator()).toCharArray());
    }

    private Token secondFrom(String... data) {
        return nthFrom(1, data);
    }
}
