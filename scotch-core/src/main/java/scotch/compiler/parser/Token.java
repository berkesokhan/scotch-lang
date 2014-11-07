package scotch.compiler.parser;

import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;
import scotch.compiler.syntax.NamedSourcePoint;
import scotch.compiler.syntax.SourceRange;

public class Token {

    public static Token token(TokenKind kind, Object value, SourceRange sourceRange) {
        return new Token(sourceRange, kind, value);
    }

    private final TokenKind   kind;
    private final Object      value;
    private final SourceRange sourceRange;

    private Token(SourceRange sourceRange, TokenKind kind, Object value) {
        this.kind = kind;
        this.value = value;
        this.sourceRange = sourceRange;
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
                .append(sourceRange, other.sourceRange)
                .isEquals();
        } else {
            return false;
        }
    }

    public int getColumn() {
        return sourceRange.getStart().getColumn();
    }

    public TokenKind getKind() {
        return kind;
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public NamedSourcePoint getStart() {
        return sourceRange.getStart();
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
        return stringify(this) + "(kind=" + kind + ", value=" + valueString + ", range=" + sourceRange + ")";
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
