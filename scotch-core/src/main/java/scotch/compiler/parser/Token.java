package scotch.compiler.parser;

import static scotch.compiler.util.SourceRange.NULL_RANGE;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;
import scotch.compiler.util.SourceCoordinate;
import scotch.compiler.util.SourceRange;

public class Token {

    public static Token token(TokenKind kind, Object value) {
        return new Token(kind, value);
    }

    public static Token token(TokenKind kind, Object value, SourceRange position) {
        return new Token(kind, value, position);
    }

    private final TokenKind   kind;
    private final Object      value;
    private final SourceRange range;

    private Token(TokenKind kind, Object value) {
        this.kind = kind;
        this.value = value;
        this.range = NULL_RANGE;
    }

    private Token(TokenKind kind, Object value, SourceRange range) {
        this.kind = kind;
        this.value = value;
        this.range = range;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Token) {
            Token other = (Token) o;
            return new EqualsBuilder()
                .append(kind, other.kind)
                .append(value, other.value)
                .append(range, other.range)
                .isEquals();
        } else {
            return false;
        }
    }

    public int getColumn() {
        return range.getStart().getColumn();
    }

    public TokenKind getKind() {
        return kind;
    }

    public SourceRange getRange() {
        return range;
    }

    public SourceCoordinate getStart() {
        return range.getStart();
    }

    public Object getValue() {
        return value;
    }

    public <T> T getValueAs(Class<T> type) {
        return type.cast(getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, value);
    }

    public boolean is(TokenKind kind) {
        return this.kind == kind;
    }

    @Override
    public String toString() {
        String valueString = getValue().toString();
        if (value instanceof String || value instanceof Character) {
            valueString = quote(valueString);
        }
        return stringify(this) + "(kind=" + kind + ", value=" + valueString + ", range=" + range + ")";
    }

    public enum TokenKind {
        ARROW,
        ASSIGN,
        BOOL,
        CHAR,
        DOUBLE,
        DOUBLE_COLON,
        SEMICOLON,
        EOF,
        WORD,
        OPERATOR,
        IN,
        INT,
        LAMBDA,
        LET,
        STRING,
        LPAREN,
        RPAREN,
        IF,
        ELSE,
        THEN,
        DOT,
        COMMA,
        NEWLINE,
        LCURLY,
        RCURLY,
        LSQUARE,
        RSQUARE,
        PIPE,
        WHERE,
        MATCH,
        ON,
        DOUBLE_ARROW,
    }
}
