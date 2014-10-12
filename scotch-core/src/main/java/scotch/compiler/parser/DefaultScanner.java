package scotch.compiler.parser;

import static java.lang.Character.getName;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang.StringEscapeUtils.unescapeJava;
import static scotch.compiler.parser.DefaultScanner.Action.ACCEPT;
import static scotch.compiler.parser.DefaultScanner.Action.ERROR;
import static scotch.compiler.parser.DefaultScanner.Action.KEEP_GOING;
import static scotch.compiler.parser.DefaultScanner.State.SCAN_COMMENT;
import static scotch.compiler.parser.DefaultScanner.State.SCAN_DEFAULT;
import static scotch.compiler.parser.DefaultScanner.State.SCAN_STRING;
import static scotch.compiler.parser.Token.TokenKind.ARROW;
import static scotch.compiler.parser.Token.TokenKind.ASSIGN;
import static scotch.compiler.parser.Token.TokenKind.BOOL;
import static scotch.compiler.parser.Token.TokenKind.CHAR;
import static scotch.compiler.parser.Token.TokenKind.COMMA;
import static scotch.compiler.parser.Token.TokenKind.DOT;
import static scotch.compiler.parser.Token.TokenKind.DOUBLE;
import static scotch.compiler.parser.Token.TokenKind.DOUBLE_ARROW;
import static scotch.compiler.parser.Token.TokenKind.DOUBLE_COLON;
import static scotch.compiler.parser.Token.TokenKind.ELSE;
import static scotch.compiler.parser.Token.TokenKind.EOF;
import static scotch.compiler.parser.Token.TokenKind.IF;
import static scotch.compiler.parser.Token.TokenKind.IN;
import static scotch.compiler.parser.Token.TokenKind.INT;
import static scotch.compiler.parser.Token.TokenKind.LAMBDA;
import static scotch.compiler.parser.Token.TokenKind.LCURLY;
import static scotch.compiler.parser.Token.TokenKind.LET;
import static scotch.compiler.parser.Token.TokenKind.LPAREN;
import static scotch.compiler.parser.Token.TokenKind.LSQUARE;
import static scotch.compiler.parser.Token.TokenKind.MATCH;
import static scotch.compiler.parser.Token.TokenKind.NEWLINE;
import static scotch.compiler.parser.Token.TokenKind.ON;
import static scotch.compiler.parser.Token.TokenKind.OPERATOR;
import static scotch.compiler.parser.Token.TokenKind.PIPE;
import static scotch.compiler.parser.Token.TokenKind.RCURLY;
import static scotch.compiler.parser.Token.TokenKind.RPAREN;
import static scotch.compiler.parser.Token.TokenKind.RSQUARE;
import static scotch.compiler.parser.Token.TokenKind.SEMICOLON;
import static scotch.compiler.parser.Token.TokenKind.STRING;
import static scotch.compiler.parser.Token.TokenKind.THEN;
import static scotch.compiler.parser.Token.TokenKind.WHERE;
import static scotch.compiler.parser.Token.TokenKind.WORD;
import static scotch.compiler.util.SourceCoordinate.coordinate;
import static scotch.compiler.util.SourcePosition.position;
import static scotch.compiler.util.TextUtil.isAsciiEscape;
import static scotch.compiler.util.TextUtil.isBackslash;
import static scotch.compiler.util.TextUtil.isBacktick;
import static scotch.compiler.util.TextUtil.isDigit;
import static scotch.compiler.util.TextUtil.isDot;
import static scotch.compiler.util.TextUtil.isDoubleQuote;
import static scotch.compiler.util.TextUtil.isHex;
import static scotch.compiler.util.TextUtil.isHorizontalWhitespace;
import static scotch.compiler.util.TextUtil.isIdentifier;
import static scotch.compiler.util.TextUtil.isLetter;
import static scotch.compiler.util.TextUtil.isNewLineOrEOF;
import static scotch.compiler.util.TextUtil.isOctal;
import static scotch.compiler.util.TextUtil.isSingleQuote;
import static scotch.compiler.util.TextUtil.isSymbol;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.collect.ImmutableMap;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.util.SourceCoordinate;
import scotch.compiler.util.SourcePosition;
import scotch.compiler.util.SourceRange.RangeBuilder;

public final class DefaultScanner implements Scanner {

    private static final Map<String, Acceptor> dictionary = ImmutableMap.<String, Acceptor>builder()
        .put("->", take(ARROW))
        .put("=>", take(DOUBLE_ARROW))
        .put("=", take(ASSIGN))
        .put("let", take(LET))
        .put("in", take(IN))
        .put("False", takeBool())
        .put("True", takeBool())
        .put("if", take(IF))
        .put("else", take(ELSE))
        .put("then", take(THEN))
        .put("where", take(WHERE))
        .put("match", take(MATCH))
        .put("on", take(ON))
        .build();

    private static Acceptor take(TokenKind kind) {
        return new Acceptor(kind);
    }

    private static Acceptor takeBool() {
        return new Acceptor(BOOL, Boolean::valueOf);
    }

    private final String                       source;
    private final char[]                       data;
    private final ArrayDeque<SaveState>        saves;
    private final ArrayDeque<State>            states;
    private final ArrayDeque<SourceCoordinate> marks;
    private       Action                       action;
    private       Optional<Token>              token;
    private       Optional<String>             text;
    private       SourceCoordinate             coordinate;

    public DefaultScanner(String source, char[] data) {
        this.source = source;
        this.data = data;
        this.saves = new ArrayDeque<>();
        this.states = new ArrayDeque<>(asList(SCAN_DEFAULT));
        this.marks = new ArrayDeque<>(asList(coordinate(0, 1, 1)));
        this.action = KEEP_GOING;
        this.token = empty();
        this.text = empty();
        this.coordinate = coordinate(0, 1, 1);
    }

    @Override
    public SourcePosition getPosition() {
        return position(getSource(), coordinate);
    }

    public String getSource() {
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
            + ", coordinate=" + coordinate
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
        token = of(Token.token(kind, value, new RangeBuilder()
                .setSource(source)
                .setStart(marks.peek())
                .setEnd(coordinate)
                .toPosition()
        ));
    }

    private void acceptChar() {
        accept(CHAR, unescapeJava(getText()).charAt(0));
    }

    private void acceptInt() {
        accept(INT, Integer::valueOf);
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
        throw new ScanException("Empty char literal " + getPosition());
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

    private SourcePosition getMarkedPosition() {
        return position(source, marks.peek());
    }

    private String getText() {
        return text.orElse("");
    }

    private DefaultScanner invalidHexEscape() {
        throw new ScanException("Invalid hex escape character " + quote(peekChar()) + " " + getPosition());
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
        marks.push(coordinate);
    }

    private int markedLength() {
        return coordinate.getOffset() - marks.peek().getOffset();
    }

    private String markedText() {
        return new String(data, marks.peek().getOffset(), markedLength());
    }

    private String nameOf(int c) {
        return isEOF() ? "<EOF>" : "<" + getName(c) + "> " + quote((char) c);
    }

    private void nextToken_() {
        setAction(ERROR);
        if (isEOF()) {
            accept(EOF);
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
                accept(LAMBDA);
            } else if (peek() == '|' && !isLetterOrDigit(peekAt(1)) && !isSymbol(peekAt(1))) {
                read();
                accept(PIPE);
            } else if (peek() == -1) {
                read();
                accept(EOF);
            } else if (peek() == '@') {
                read();
                accept(WORD);
            } else if (peek() == '(') {
                scanParentheses();
            } else if (peek() == ')') {
                read();
                accept(RPAREN);
            } else if (peek() == '[') {
                read();
                if (peek() == ']') {
                    read();
                    accept(WORD);
                } else {
                    accept(LSQUARE);
                }
            } else if (peek() == ']') {
                read();
                accept(RSQUARE);
            } else if (peek() == '\n') {
                read();
                accept(NEWLINE);
            } else if (peek() == '.') {
                if (!isIdentifier(peekAt(-1)) && !isIdentifier(peekAt(1))) {
                    read();
                    accept(WORD);
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
                    accept(WORD);
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
        if (coordinate.getOffset() + offset >= data.length || data[coordinate.getOffset() + offset] == '\0') {
            return -1;
        } else {
            return data[coordinate.getOffset() + offset];
        }
    }

    private char peekChar() {
        return (char) peek();
    }

    private void read() {
        if (!isEOF()) {
            if (peek() == '\n') {
                coordinate = coordinate.nextLine();
            } else if (peek() == '\t') {
                coordinate = coordinate.nextTab();
            } else {
                coordinate = coordinate.nextChar();
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
            accept(LCURLY);
        } else if (peek() == '}') {
            read();
            accept(RCURLY);
        } else if (isIdentifier(peek())) {
            if (isPrefix()) {
                read();
                accept(WORD);
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
                    accept(DOUBLE, Double::valueOf);
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
                accept(LPAREN);
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
            accept(WORD);
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
                    throw new ScanException("Cannot quote reserved word " + quote(word) + " " + getMarkedPosition());
                } else {
                    read();
                    accept(OPERATOR, word);
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
            accept(STRING, unescapeJava(getText()));
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
        accept(dictionary.getOrDefault(getText(), take(WORD)));
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
            throw new ScanException("Unterminated character literal: unexpected " + nameOf(peek()) + " " + getPosition());
        }
    }

    private void unMark() {
        marks.pop();
    }

    private void unexpected() {
        throw new ScanException("Unexpected " + nameOf(peek()) + " " + getPosition());
    }

    private DefaultScanner unterminatedChar() {
        throw new ScanException("Unterminated char literal " + getPosition());
    }

    private DefaultScanner unterminatedString() {
        throw new ScanException("Unterminated string " + getPosition());
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

        private final ArrayDeque<SaveState>        saves;
        private final ArrayDeque<State>            states;
        private final ArrayDeque<SourceCoordinate> marks;
        private final Action                       action;
        private final Optional<Token>              token;
        private final Optional<String>             text;
        private final SourceCoordinate             coordinate;

        public SaveState(DefaultScanner scanner) {
            saves = new ArrayDeque<>(scanner.saves);
            states = new ArrayDeque<>(scanner.states);
            marks = new ArrayDeque<>(scanner.marks);
            action = scanner.action;
            token = scanner.token;
            text = scanner.text;
            coordinate = scanner.coordinate;
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
            scanner.coordinate = coordinate;
        }
    }
}
