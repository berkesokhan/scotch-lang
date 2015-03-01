package scotch.compiler.scanner;

import static scotch.util.StringUtil.quote;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;
import scotch.compiler.text.NamedSourcePoint;
import scotch.compiler.text.SourceRange;

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

    public NamedSourcePoint getEnd() {
        return sourceRange.getEnd();
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

    public int getStartOffset() {
        return sourceRange.getStartOffset();
    }

    public int getEndOffset() {
        return sourceRange.getEndOffset();
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
        return kind + "(" + quote(value) + ")";
    }

    public Token withKind(TokenKind kind) {
        return new Token(sourceRange, kind, value);
    }

    public enum TokenKind {
        ARROW,
        ASSIGN,
        BOOL_LITERAL,
        CHAR_LITERAL,
        DOUBLE_LITERAL,
        DOUBLE_COLON,
        SEMICOLON,
        END_OF_FILE,
        IDENTIFIER,
        DEFAULT_OPERATOR,
        KEYWORD_IN,
        INT_LITERAL,
        BACKSLASH,
        KEYWORD_LET,
        STRING_LITERAL,
        LEFT_PARENTHESIS,
        RIGHT_PARENTHESIS,
        KEYWORD_IF,
        KEYWORD_ELSE,
        KEYWORD_THEN,
        DOT,
        COMMA,
        NEWLINE,
        LEFT_CURLY_BRACE,
        RIGHT_CURLY_BRACE,
        LEFT_SQUARE_BRACE,
        RIGHT_SQUARE_BRACE,
        PIPE,
        KEYWORD_WHERE,
        KEYWORD_MATCH,
        KEYWORD_ON,
        DOUBLE_ARROW,
        KEYWORD_DO,
        BACKWARD_ARROW,
    }
}
