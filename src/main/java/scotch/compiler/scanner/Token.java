package scotch.compiler.scanner;

import static scotch.util.StringUtil.quote;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;
import scotch.compiler.text.NamedSourcePoint;
import scotch.compiler.text.SourceLocation;

public class Token {

    public static Token token(TokenKind kind, Object value, SourceLocation sourceLocation) {
        return new Token(sourceLocation, kind, value);
    }

    private final TokenKind      kind;
    private final Object         value;
    private final SourceLocation sourceLocation;

    private Token(SourceLocation sourceLocation, TokenKind kind, Object value) {
        this.kind = kind;
        this.value = value;
        this.sourceLocation = sourceLocation;
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
                .append(sourceLocation, other.sourceLocation)
                .isEquals();
        } else {
            return false;
        }
    }

    public int getColumn() {
        return sourceLocation.getStart().getColumn();
    }

    public NamedSourcePoint getEnd() {
        return sourceLocation.getEnd();
    }

    public TokenKind getKind() {
        return kind;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public NamedSourcePoint getStart() {
        return sourceLocation.getStart();
    }

    public int getStartOffset() {
        return sourceLocation.getStartOffset();
    }

    public int getEndOffset() {
        return sourceLocation.getEndOffset();
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
        return new Token(sourceLocation, kind, value);
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
