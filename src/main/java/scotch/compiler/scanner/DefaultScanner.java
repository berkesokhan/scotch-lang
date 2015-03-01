package scotch.compiler.scanner;

import static java.lang.Character.getName;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang.StringEscapeUtils.unescapeJava;
import static scotch.compiler.scanner.DefaultScanner.Action.ACCEPT;
import static scotch.compiler.scanner.DefaultScanner.Action.ERROR;
import static scotch.compiler.scanner.DefaultScanner.Action.KEEP_GOING;
import static scotch.compiler.scanner.DefaultScanner.State.SCAN_COMMENT;
import static scotch.compiler.scanner.DefaultScanner.State.SCAN_DEFAULT;
import static scotch.compiler.scanner.DefaultScanner.State.SCAN_STRING;
import static scotch.compiler.scanner.Token.TokenKind.ARROW;
import static scotch.compiler.scanner.Token.TokenKind.ASSIGN;
import static scotch.compiler.scanner.Token.TokenKind.BACKSLASH;
import static scotch.compiler.scanner.Token.TokenKind.BACKWARD_ARROW;
import static scotch.compiler.scanner.Token.TokenKind.BOOL_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.CHAR_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.COMMA;
import static scotch.compiler.scanner.Token.TokenKind.DEFAULT_OPERATOR;
import static scotch.compiler.scanner.Token.TokenKind.DOT;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_ARROW;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_COLON;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.END_OF_FILE;
import static scotch.compiler.scanner.Token.TokenKind.IDENTIFIER;
import static scotch.compiler.scanner.Token.TokenKind.INT_LITERAL;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_DO;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_ELSE;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_IF;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_IN;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_LET;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_MATCH;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_ON;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_THEN;
import static scotch.compiler.scanner.Token.TokenKind.KEYWORD_WHERE;
import static scotch.compiler.scanner.Token.TokenKind.LEFT_CURLY_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.LEFT_PARENTHESIS;
import static scotch.compiler.scanner.Token.TokenKind.LEFT_SQUARE_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.NEWLINE;
import static scotch.compiler.scanner.Token.TokenKind.PIPE;
import static scotch.compiler.scanner.Token.TokenKind.RIGHT_CURLY_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.RIGHT_PARENTHESIS;
import static scotch.compiler.scanner.Token.TokenKind.RIGHT_SQUARE_BRACE;
import static scotch.compiler.scanner.Token.TokenKind.SEMICOLON;
import static scotch.compiler.scanner.Token.TokenKind.STRING_LITERAL;
import static scotch.compiler.scanner.Token.token;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.TextUtil.isAsciiEscape;
import static scotch.compiler.text.TextUtil.isBackslash;
import static scotch.compiler.text.TextUtil.isBacktick;
import static scotch.compiler.text.TextUtil.isDigit;
import static scotch.compiler.text.TextUtil.isDot;
import static scotch.compiler.text.TextUtil.isDoubleQuote;
import static scotch.compiler.text.TextUtil.isHex;
import static scotch.compiler.text.TextUtil.isHorizontalWhitespace;
import static scotch.compiler.text.TextUtil.isIdentifier;
import static scotch.compiler.text.TextUtil.isLetter;
import static scotch.compiler.text.TextUtil.isNewLineOrEOF;
import static scotch.compiler.text.TextUtil.isOctal;
import static scotch.compiler.text.TextUtil.isSingleQuote;
import static scotch.compiler.text.TextUtil.isSymbol;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.collect.ImmutableMap;
import scotch.compiler.scanner.Token.TokenKind;
import scotch.compiler.text.NamedSourcePoint;
import scotch.compiler.text.SourcePoint;

public final class DefaultScanner implements Scanner {

    private static final Map<String, Acceptor> dictionary = ImmutableMap.<String, Acceptor>builder()
        .put("->", take(ARROW))
        .put("<-", take(BACKWARD_ARROW))
        .put("=>", take(DOUBLE_ARROW))
        .put("=", take(ASSIGN))
        .put("let", take(KEYWORD_LET))
        .put("in", take(KEYWORD_IN))
        .put("False", takeBool())
        .put("True", takeBool())
        .put("if", take(KEYWORD_IF))
        .put("else", take(KEYWORD_ELSE))
        .put("then", take(KEYWORD_THEN))
        .put("where", take(KEYWORD_WHERE))
        .put("match", take(KEYWORD_MATCH))
        .put("on", take(KEYWORD_ON))
        .put("do", take(KEYWORD_DO))
        .build();

    private static Acceptor take(TokenKind kind) {
        return new Acceptor(kind);
    }

    private static Acceptor takeBool() {
        return new Acceptor(BOOL_LITERAL, Boolean::valueOf);
    }

    private final URI                source;
    private final char[]             data;
    private final Deque<SaveState>   saves;
    private final Deque<State>       states;
    private final Deque<SourcePoint> marks;
    private       Action             action;
    private       Optional<Token>    token;
    private       Optional<String>   text;
    private       SourcePoint        location;

    public DefaultScanner(URI source, char[] data) {
        this.source = source;
        this.data = data;
        this.saves = new ArrayDeque<>();
        this.states = new ArrayDeque<>(asList(SCAN_DEFAULT));
        this.marks = new ArrayDeque<>(asList(point(0, 1, 1)));
        this.action = KEEP_GOING;
        this.token = empty();
        this.text = empty();
        this.location = point(0, 1, 1);
    }

    @Override
    public NamedSourcePoint getPosition() {
        return location.withSource(source);
    }

    public URI getSource() {
        return source;
    }

    @Override
    public Token nextToken() {
        token = empty();
        setText(s -> "");
        setAction(KEEP_GOING);
        mark();
        while (isKeepGoing()) {
            nextToken_();
        }
        if (isError()) {
            unexpected();
        } else {
            unMark();
        }
        return token.get();
    }

    @Override
    public String toString() {
        return stringify(this) + "("
            + "source=" + quote(source)
            + ", coord=" + location
            + ")";
    }

    private void accept() {
        action = ACCEPT;
    }

    private void accept(TokenKind kind) {
        accept(kind, markedText());
    }

    private void accept(TokenKind kind, Function<String, Object> modifier) {
        accept(kind, modifier.apply(markedText()));
    }

    private void accept(Acceptor acceptor) {
        accept(acceptor.getKind(), acceptor.getModifier());
    }

    private void accept(TokenKind kind, Object value) {
        accept();
        token = of(token(kind, value, marks.peek().withSource(source).to(getPosition())));
    }

    private void acceptChar() {
        accept(CHAR_LITERAL, unescapeJava(getText()).charAt(0));
    }

    private void acceptInt() {
        accept(INT_LITERAL, Integer::valueOf);
    }

    private void begin() {
        saves.push(new SaveState(this));
    }

    private void beginComment() {
        skip();
        skip();
        enterState(SCAN_COMMENT);
        keepGoing();
    }

    private void beginString() {
        read();
        enterState(SCAN_STRING);
        keepGoing();
    }

    private DefaultScanner emptyChar() {
        throw new ScanException("Empty char literal " + getPosition().prettyPrint());
    }

    private void end() {
        saves.pop();
    }

    private void enterState(State state) {
        states.push(state);
    }

    private void error() {
        setAction(ERROR);
    }

    private void gatherWord() {
        setText(t -> t + peekChar());
        read();
    }

    private NamedSourcePoint getMarkedPosition() {
        return marks.peek().withSource(source);
    }

    private String getText() {
        return text.orElse("");
    }

    private DefaultScanner invalidHexEscape() {
        throw new ScanException("Invalid hex escape character " + quote(peekChar()) + " " + getPosition().prettyPrint());
    }

    private boolean isEOF() {
        return peek() == -1;
    }

    private boolean isError() {
        return action == ERROR;
    }

    private boolean isKeepGoing() {
        return action == KEEP_GOING;
    }

    private boolean isPrefix() {
        int c0 = peek();
        int c1 = peekAt(1);
        return (c0 == '-' || c0 == '!') && (isLetter(c1) || isDigit(c1));
    }

    private void keepGoing() {
        setAction(KEEP_GOING);
    }

    private void leaveComment() {
        skip();
        skip();
        leaveState();
        keepGoing();
    }

    private void leaveState() {
        states.pop();
    }

    private void mark() {
        marks.push(location);
    }

    private int markedLength() {
        return location.getOffset() - marks.peek().getOffset();
    }

    private String markedText() {
        return new String(data, marks.peek().getOffset(), markedLength());
    }

    private String nameOf(int c) {
        return isEOF() ? "<END_OF_FILE>" : "<" + getName(c) + "> " + quote((char) c);
    }

    private void nextToken_() {
        setAction(ERROR);
        if (isEOF()) {
            accept(END_OF_FILE);
        } else if (state() == SCAN_DEFAULT) {
            skipIgnored();
            if (isDoubleQuote(peek())) {
                beginString();
            } else if (isSingleQuote(peek())) {
                scanChar();
            } else if (isBacktick(peek())) {
                scanQuotedWord();
            } else if (isBackslash(peek())) {
                read();
                accept(BACKSLASH);
            } else if (peek() == '|' && !isLetterOrDigit(peekAt(1)) && !isSymbol(peekAt(1))) {
                read();
                accept(PIPE);
            } else if (peek() == -1) {
                read();
                accept(END_OF_FILE);
            } else if (peek() == '@') {
                read();
                accept(IDENTIFIER);
            } else if (peek() == '(') {
                scanParentheses();
            } else if (peek() == ')') {
                read();
                accept(RIGHT_PARENTHESIS);
            } else if (peek() == '[') {
                read();
                if (peek() == ']') {
                    read();
                    accept(IDENTIFIER);
                } else {
                    accept(LEFT_SQUARE_BRACE);
                }
            } else if (peek() == ']') {
                read();
                accept(RIGHT_SQUARE_BRACE);
            } else if (peek() == '\n') {
                read();
                accept(NEWLINE);
            } else if (peek() == '.') {
                if (!isIdentifier(peekAt(-1)) && !isIdentifier(peekAt(1))) {
                    read();
                    accept(IDENTIFIER);
                } else {
                    read();
                    accept(DOT);
                }
            } else if (peek() == ',') {
                read();
                accept(COMMA);
            } else if (peek() == ';') {
                read();
                accept(SEMICOLON);
            } else if (peek() == ':') {
                read();
                if (peek() == ':') {
                    read();
                    accept(DOUBLE_COLON);
                } else {
                    accept(IDENTIFIER);
                }
            } else {
                scanDefault();
            }
        } else if (state() == SCAN_COMMENT) {
            scanComment();
        } else if (state() == SCAN_STRING) {
            scanString();
        } else {
            error();
        }
    }

    private int peek() {
        return peekAt(0);
    }

    private int peekAt(int offset) {
        if (location.getOffset() + offset >= data.length || data[location.getOffset() + offset] == '\0') {
            return -1;
        } else {
            return data[location.getOffset() + offset];
        }
    }

    private char peekChar() {
        return (char) peek();
    }

    private void read() {
        if (!isEOF()) {
            if (peek() == '\n') {
                location = location.nextLine();
            } else if (peek() == '\t') {
                location = location.nextTab();
            } else {
                location = location.nextChar();
            }
        }
    }

    private void readWord() {
        setText(t -> "");
        while (isIdentifier(peek())) {
            gatherWord();
        }
        while (peek() == '\'') {
            gatherWord();
        }
    }

    private void rollback() {
        saves.pop().restore(this);
    }

    private void scanChar() {
        if (isSingleQuote(peek())) {
            read();
            if (isSingleQuote(peek())) {
                emptyChar();
            } else if (isNewLineOrEOF(peek())) {
                unterminatedChar();
            } else if (isBackslash(peek())) {
                scanEscape();
                terminateChar();
            } else {
                setText(t -> "" + peekChar());
                read();
                terminateChar();
            }
        } else {
            error();
        }
    }

    private void scanComment() {
        if (peek() == '*' && peekAt(1) == '/') {
            leaveComment();
        } else if (peek() == '/' && peekAt(1) == '*') {
            beginComment();
        } else {
            skip();
            keepGoing();
        }
    }

    private void scanDefault() {
        if (peek() == '/' && peekAt(1) == '*') {
            beginComment();
        } else if (isDigit(peek())) {
            scanNumber();
        } else if (peek() == '{') {
            read();
            accept(LEFT_CURLY_BRACE);
        } else if (peek() == '}') {
            read();
            accept(RIGHT_CURLY_BRACE);
        } else if (isIdentifier(peek())) {
            if (isPrefix()) {
                read();
                accept(IDENTIFIER);
            } else {
                scanWord();
            }
        } else {
            error();
        }
    }

    private void scanEscape() {
        if (isBackslash(peek())) {
            setText(t -> t + peekChar());
            read();
            if (isAsciiEscape(peek())) {
                setText(t -> t + peekChar());
                read();
                keepGoing();
            } else if (peek() == 'u') {
                setText(t -> t + 'u');
                while (peek() == 'u') {
                    read();
                }
                for (int i = 0; i < 4; i++) {
                    if (isHex(peek())) {
                        setText(t -> t + peekChar());
                        read();
                        keepGoing();
                    } else {
                        invalidHexEscape();
                    }
                }
            } else if (isOctal(peek())) {
                mark();
                if (peek() >= '0' && peek() <= '3') {
                    read();
                }
                for (int i = 0; i < 2; i++) {
                    if (isOctal(peek())) {
                        read();
                    }
                }
                setText(t -> t + format("u%04x", parseInt(markedText(), 8)));
                unMark();
                keepGoing();
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void scanNumber() {
        while (isDigit(peek())) {
            read();
        }
        if (isDot(peek())) {
            if (isDigit(peekAt(1))) {
                begin();
                read();
                while (isDigit(peek())) {
                    read();
                }
                if (isIdentifier(peek())) {
                    rollback();
                    acceptInt();
                } else {
                    end();
                    accept(DOUBLE_LITERAL, Double::valueOf);
                }
            } else {
                acceptInt();
            }
        } else if (isIdentifier(peek())) {
            scanWord();
        } else {
            acceptInt();
        }
    }

    private void scanParentheses() {
        if (peek() == '(') {
            read();
            if (peek() == ',' || peek() == ')') {
                scanParentheses_();
            } else {
                accept(LEFT_PARENTHESIS);
            }
        } else {
            unexpected();
        }
    }

    private void scanParentheses_() {
        if (peek() == ',') {
            read();
            scanParentheses_();
        } else if (peek() == ')') {
            read();
            accept(IDENTIFIER);
        } else {
            unexpected();
        }
    }

    private void scanQuotedWord() {
        if (isBacktick(peek())) {
            read();
            readWord();
            if (isBacktick(peek())) {
                String word = markedText().substring(1, markedLength());
                if (dictionary.containsKey(word)) {
                    throw new ScanException("Cannot quote reserved word " + quote(word) + " " + getMarkedPosition().prettyPrint());
                } else {
                    read();
                    accept(DEFAULT_OPERATOR, word);
                }
            } else {
                error();
            }
        } else {
            error();
        }
    }

    private void scanString() {
        if (isDoubleQuote(peek())) {
            read();
            leaveState();
            accept(STRING_LITERAL, unescapeJava(getText()));
        } else if (isNewLineOrEOF(peek())) {
            unterminatedString();
        } else if (isBackslash(peek())) {
            scanEscape();
            keepGoing();
        } else {
            setText(t -> t + peekChar());
            read();
            keepGoing();
        }
    }

    private void scanWord() {
        readWord();
        accept(dictionary.getOrDefault(getText(), take(IDENTIFIER)));
    }

    private void setAction(Action action) {
        this.action = action;
    }

    private void setText(Function<String, String> modifier) {
        text = of(modifier.apply(text.orElse("")));
    }

    private void skip() {
        unMark();
        read();
        mark();
    }

    private void skipIgnored() {
        if (peek() == '\r') {
            skip();
            skipIgnored();
        } else if (isHorizontalWhitespace(peek())) {
            while (isHorizontalWhitespace(peek())) {
                skip();
            }
            skipIgnored();
        } else if (peek() == '/' && peekAt(1) == '/') {
            while (peek() != '\n') {
                skip();
            }
            skip();
            skipIgnored();
        }
    }

    private State state() {
        return states.peek();
    }

    private void terminateChar() {
        if (isSingleQuote(peek())) {
            read();
            acceptChar();
        } else {
            throw new ScanException("Unterminated character literal: unexpected " + nameOf(peek()) + " " + getPosition().prettyPrint());
        }
    }

    private void unMark() {
        marks.pop();
    }

    private void unexpected() {
        throw new ScanException("Unexpected " + nameOf(peek()) + " " + getPosition().prettyPrint());
    }

    private DefaultScanner unterminatedChar() {
        throw new ScanException("Unterminated char literal " + getPosition().prettyPrint());
    }

    private DefaultScanner unterminatedString() {
        throw new ScanException("Unterminated string " + getPosition().prettyPrint());
    }

    enum Action {
        KEEP_GOING,
        ACCEPT,
        ERROR,
    }

    enum State {
        SCAN_DEFAULT,
        SCAN_COMMENT,
        SCAN_STRING,
    }

    private static final class Acceptor {

        private final TokenKind                kind;
        private final Function<String, Object> modifier;

        public Acceptor(TokenKind kind) {
            this.kind = kind;
            this.modifier = text -> text;
        }

        public Acceptor(TokenKind kind, Function<String, Object> modifier) {
            this.kind = kind;
            this.modifier = modifier;
        }

        public TokenKind getKind() {
            return kind;
        }

        public Function<String, Object> getModifier() {
            return modifier;
        }
    }

    private static final class SaveState {

        private final ArrayDeque<SaveState>   saves;
        private final ArrayDeque<State>       states;
        private final ArrayDeque<SourcePoint> marks;
        private final Action                  action;
        private final Optional<Token>         token;
        private final Optional<String>        text;
        private final SourcePoint             coordinate;

        public SaveState(DefaultScanner scanner) {
            saves = new ArrayDeque<>(scanner.saves);
            states = new ArrayDeque<>(scanner.states);
            marks = new ArrayDeque<>(scanner.marks);
            action = scanner.action;
            token = scanner.token;
            text = scanner.text;
            coordinate = scanner.location;
        }

        public void restore(DefaultScanner scanner) {
            scanner.saves.clear();
            scanner.saves.addAll(saves);
            scanner.states.clear();
            scanner.states.addAll(states);
            scanner.marks.clear();
            scanner.marks.addAll(marks);
            scanner.action = action;
            scanner.token = token;
            scanner.text = text;
            scanner.location = coordinate;
        }
    }
}
