package scotch.compiler.parser;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.Definition.classDef;
import static scotch.compiler.ast.Definition.module;
import static scotch.compiler.ast.Definition.operatorDef;
import static scotch.compiler.ast.Definition.root;
import static scotch.compiler.ast.Definition.signature;
import static scotch.compiler.ast.Definition.unshuffled;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionEntry.unscopedEntry;
import static scotch.compiler.ast.DefinitionReference.classRef;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.Import.moduleImport;
import static scotch.compiler.ast.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.ast.Operator.Fixity.PREFIX;
import static scotch.compiler.ast.Operator.Fixity.RIGHT_INFIX;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatch.equal;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.Identifier;
import static scotch.compiler.ast.Value.ValueVisitor;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.message;
import static scotch.compiler.parser.Token.TokenKind.ARROW;
import static scotch.compiler.parser.Token.TokenKind.ASSIGN;
import static scotch.compiler.parser.Token.TokenKind.BOOL;
import static scotch.compiler.parser.Token.TokenKind.CHAR;
import static scotch.compiler.parser.Token.TokenKind.COMMA;
import static scotch.compiler.parser.Token.TokenKind.DOT;
import static scotch.compiler.parser.Token.TokenKind.DOUBLE;
import static scotch.compiler.parser.Token.TokenKind.DOUBLE_COLON;
import static scotch.compiler.parser.Token.TokenKind.EOF;
import static scotch.compiler.parser.Token.TokenKind.INT;
import static scotch.compiler.parser.Token.TokenKind.LCURLY;
import static scotch.compiler.parser.Token.TokenKind.LPAREN;
import static scotch.compiler.parser.Token.TokenKind.RCURLY;
import static scotch.compiler.parser.Token.TokenKind.RPAREN;
import static scotch.compiler.parser.Token.TokenKind.SEMICOLON;
import static scotch.compiler.parser.Token.TokenKind.STRING;
import static scotch.compiler.parser.Token.TokenKind.WHERE;
import static scotch.compiler.parser.Token.TokenKind.WORD;
import static scotch.compiler.util.TextUtil.normalizeQualified;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.splitQualified;
import static scotch.lang.Symbol.qualified;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.sum;
import static scotch.lang.Type.t;
import static scotch.lang.Type.var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.Operator.Fixity;
import scotch.compiler.ast.PatternMatch;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.util.SourcePosition;
import scotch.compiler.util.TextUtil;
import scotch.lang.Symbol;
import scotch.lang.Type;

public class Parser {

    private static final List<TokenKind> literals = asList(STRING, INT, CHAR, DOUBLE, BOOL);
    private final LookAheadScanner      scanner;
    private final List<DefinitionEntry> definitions;
    private       String                currentModule;
    private       int                   sequence;

    public Parser(Scanner scanner) {
        this.scanner = new LookAheadScanner(scanner);
        this.definitions = new ArrayList<>();
    }

    public SymbolTable parse() {
        List<DefinitionReference> modules = new ArrayList<>();
        if (!expects(EOF)) {
            modules.add(parseModule());
            while (expectsModule()) {
                modules.add(parseModule());
            }
        }
        require(EOF);
        collectSymbol(unscopedEntry(rootRef(), root(modules)));
        return new SymbolTable(sequence, definitions);
    }

    private void collectSymbol(DefinitionEntry entry) {
        definitions.add(entry);
    }

    private boolean expects(TokenKind kind) {
        return expectsAt(0, kind);
    }

    private boolean expectsAt(int offset, TokenKind kind) {
        return scanner.peekAt(offset).is(kind);
    }

    private boolean expectsClassDefinition() {
        return expectsWord("class");
    }

    private boolean expectsDefinitions() {
        return !expectsModule() && !expects(EOF);
    }

    private boolean expectsImport() {
        return expectsWord("import");
    }

    private boolean expectsLiteral() {
        return literals.stream().anyMatch(this::expects);
    }

    private boolean expectsModule() {
        return expectsWord("module");
    }

    private boolean expectsOperatorDefinition() {
        return expectsWord("prefix")
            || ((expectsWord("left") || expectsWord("right")) && expectsWordAt(1, "infix"));
    }

    private boolean expectsValueSignatures() {
        int offset = 0;
        while (!expectsAt(offset, SEMICOLON) && !expectsAt(offset, LCURLY)) {
            if (expectsAt(offset, WORD)) {
                if (expectsAt(offset + 1, COMMA)) {
                    offset += 2;
                } else if (expectsAt(offset + 1, DOUBLE_COLON)) {
                    return true;
                } else {
                    break;
                }
            } else if (expectsAt(offset, LPAREN) && expectsAt(offset + 1, WORD) && expectsAt(offset + 2, RPAREN)) {
                if (expectsAt(offset + 3, COMMA)) {
                    offset += 4;
                } else if (expectsAt(offset + 3, DOUBLE_COLON)) {
                    return true;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return false;
    }

    private boolean expectsWord() {
        return expectsWordAt(0);
    }

    private boolean expectsWord(String value) {
        return expectsWordAt(0, value);
    }

    private boolean expectsWordAt(int offset) {
        return expectsAt(offset, WORD);
    }

    private boolean expectsWordAt(int offset, String value) {
        return expectsAt(offset, WORD) && Objects.equals(scanner.peekAt(offset).getValue(), value);
    }

    private void nextToken() {
        scanner.nextToken();
    }

    private List<Type> parseClassArguments() {
        List<Type> arguments = new ArrayList<>();
        do {
            arguments.add(var(requireWord()));
        } while (expectsWord());
        return arguments;
    }

    private DefinitionReference parseClassDefinition() {
        requireWord("class");
        String name = requireWord();
        DefinitionReference reference = classRef(currentModule, name);
        collectSymbol(unscopedEntry(reference, classDef(name, parseClassArguments(), parseClassMembers())));
        return reference;
    }

    private List<DefinitionReference> parseClassMembers() {
        require(WHERE);
        require(LCURLY);
        List<DefinitionReference> members = new ArrayList<>();
        while (!expects(RCURLY)) {
            if (expectsValueSignatures()) {
                parseValueSignatures().forEach(members::add);
            } else {
                members.add(parseValueDefinition());
            }
            requireTerminator();
        }
        require(RCURLY);
        return members;
    }

    private List<DefinitionReference> parseDefinitions() {
        List<DefinitionReference> definitions = new ArrayList<>();
        if (expectsClassDefinition()) {
            definitions.add(parseClassDefinition());
        } else if (expectsOperatorDefinition()) {
            definitions.addAll(parseOperatorDefinition());
        } else if (expectsValueSignatures()) {
            definitions.addAll(parseValueSignatures());
        } else {
            definitions.add(parseValueDefinition());
        }
        return definitions;
    }

    private Import parseImport() {
        requireWord("import");
        return moduleImport(parseQualifiedName());
    }

    private List<Import> parseImports() {
        List<Import> imports = new ArrayList<>();
        while (expectsImport()) {
            imports.add(parseImport());
            requireTerminator();
        }
        return imports;
    }

    private Value parseLiteral() {
        if (expects(STRING)) {
            return literal(requireString(), reserveType());
        } else if (expects(INT)) {
            return literal(requireInt(), reserveType());
        } else if (expects(CHAR)) {
            return literal(requireChar(), reserveType());
        } else if (expects(DOUBLE)) {
            return literal(requireDouble(), reserveType());
        } else if (expects(BOOL)) {
            return literal(requireBool(), reserveType());
        } else {
            throw unexpected(literals);
        }
    }

    private Optional<PatternMatch> parseMatch(boolean required) {
        PatternMatch match = null;
        if (expectsWord()) {
            match = parseWordReference().accept(new ValueVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Identifier identifier) {
                    return capture(identifier.getSymbol(), identifier.getType());
                }
            });
        } else if (expectsLiteral()) {
            match = equal(parseLiteral());
        } else if (required) {
            throw unexpected(WORD);
        }
        return ofNullable(match);
    }

    private Value parseMessage() {
        List<Value> values = new ArrayList<>();
        values.add(parseRequiredPrimary());
        Optional<Value> argument = parseOptionalPrimary();
        while (argument.isPresent()) {
            values.add(argument.get());
            argument = parseOptionalPrimary();
        }
        return message(values);
    }

    @SuppressWarnings("unchecked")
    private DefinitionReference parseModule() {
        requireWord("module");
        currentModule = parseQualifiedName();
        requireTerminator();
        List<Import> imports = parseImports();
        List<DefinitionReference> definitions = parseModuleDefinitions();
        return collect(module(currentModule, imports, definitions));
    }

    private DefinitionReference collect(Definition definition) {
        return collect(unscopedEntry(definition.getReference(), definition));
    }

    private DefinitionReference collect(DefinitionEntry definition) {
        definitions.add(definition);
        return definition.getReference();
    }

    private List<DefinitionReference> parseModuleDefinitions() {
        List<DefinitionReference> definitions = new ArrayList<>();
        while (expectsDefinitions()) {
            definitions.addAll(parseDefinitions());
            requireTerminator();
        }
        return definitions;
    }

    private List<DefinitionReference> parseOperatorDefinition() {
        Fixity fixity = parseOperatorFixity();
        int precedence = parseOperatorPrecedence();
        List<DefinitionReference> references = new ArrayList<>();
        parseSymbols().forEach(symbol -> {
            Definition operator = operatorDef(symbol, fixity, precedence);
            collectSymbol(unscopedEntry(operator.getReference(), operator));
            references.add(operator.getReference());
        });
        return references;
    }

    private Fixity parseOperatorFixity() {
        if (expectsWord("right") && expectsWordAt(1, "infix")) {
            nextToken();
            nextToken();
            return RIGHT_INFIX;
        } else if (expectsWord("left") && expectsWordAt(1, "infix")) {
            nextToken();
            nextToken();
            return LEFT_INFIX;
        } else if (expectsWord("prefix")) {
            nextToken();
            return PREFIX;
        } else {
            throw unexpected(WORD, "left infix", "right infix", "prefix");
        }
    }

    private int parseOperatorPrecedence() {
        return requireInt();
    }

    private Optional<PatternMatch> parseOptionalMatch() {
        return parseMatch(false);
    }

    private Optional<Value> parseOptionalPrimary() {
        return parsePrimary(false);
    }

    private List<PatternMatch> parsePatternMatches() {
        List<PatternMatch> matches = new ArrayList<>();
        matches.add(parseRequiredMatch());
        Optional<PatternMatch> match = parseOptionalMatch();
        while (match.isPresent()) {
            matches.add(match.get());
            match = parseOptionalMatch();
        }
        return matches;
    }

    private Optional<Value> parsePrimary(boolean required) {
        Value value = null;
        if (expectsWord()) {
            value = parseWordReference();
        } else if (expectsLiteral()) {
            value = parseLiteral();
        } else if (expects(LPAREN)) {
            nextToken();
            value = parseMessage();
            require(RPAREN);
        } else if (required) {
            throw unexpected(ImmutableList.<TokenKind>builder().add(WORD, LPAREN).addAll(literals).build());
        }
        return ofNullable(value);
    }

    private String parseQualifiedName() {
        List<String> parts = new ArrayList<>();
        parts.add(requireWord());
        while (expects(DOT)) {
            nextToken();
            parts.add(requireWord());
        }
        return join(".", parts);
    }

    private PatternMatch parseRequiredMatch() {
        return parseMatch(true).get();
    }

    private Value parseRequiredPrimary() {
        return parsePrimary(true).get();
    }

    private Symbol parseSymbol() {
        if (expects(WORD)) {
            return qualified(currentModule, requireWord());
        } else if (expects(LPAREN)) {
            nextToken();
            String symbol = requireWord();
            require(RPAREN);
            return qualified(currentModule, symbol);
        } else {
            throw unexpected(asList(WORD, LPAREN));
        }
    }

    private List<Symbol> parseSymbols() {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(parseSymbol());
        while (expects(COMMA)) {
            nextToken();
            symbols.add(parseSymbol());
        }
        return symbols;
    }

    private Type parseType() {
        return splitQualified(parseQualifiedName()).into((optionalModuleName, memberName) -> {
            if (isLowerCase(memberName.charAt(0))) {
                if (optionalModuleName.isPresent()) {
                    throw new ParseException("Type name must be uppercase"); // TODO better error reporting
                } else {
                    return var(memberName);
                }
            } else {
                return sum(normalizeQualified(optionalModuleName, memberName));
            }
        });
    }

    private DefinitionReference parseValueDefinition() {
        if (expectsAt(1, ASSIGN)) {
            String name = requireWord();
            nextToken();
            return collect(value(qualified(currentModule, name), reserveType(), parseMessage()));
        } else {
            List<PatternMatch> patternMatches = parsePatternMatches();
            require(ASSIGN);
            return collect(unshuffled(qualified(currentModule, "pattern#" + sequence++), pattern(patternMatches, parseMessage())));
        }
    }

    private Type parseValueSignature() {
        require(DOUBLE_COLON);
        return parseValueSignature_();
    }

    private Type parseValueSignature_() {
        Type type = parseType();
        if (expects(ARROW)) {
            require(ARROW);
            type = fn(type, parseValueSignature_());
        }
        return type;
    }

    private List<DefinitionReference> parseValueSignatures() {
        List<DefinitionReference> references = new ArrayList<>();
        List<Symbol> symbolList = parseSymbols();
        Type type = parseValueSignature();
        symbolList.forEach(symbol -> {
            Definition signature = signature(symbol, type);
            references.add(collect(signature));
        });
        return references;
    }

    private Value parseWordReference() {
        String word = requireWord();
        return id(word, reserveType());
    }

    private Token peekAt(int offset) {
        return scanner.peekAt(offset);
    }

    private Token require(TokenKind kind) {
        return requireAt(0, kind);
    }

    private Token requireAt(int offset, TokenKind kind) {
        if (expectsAt(offset, kind)) {
            Token token = peekAt(offset);
            nextToken();
            return token;
        } else {
            throw unexpected(kind);
        }
    }

    private Boolean requireBool() {
        return require(BOOL).getValueAs(Boolean.class);
    }

    private Character requireChar() {
        return require(CHAR).getValueAs(Character.class);
    }

    private Double requireDouble() {
        return require(DOUBLE).getValueAs(Double.class);
    }

    private int requireInt() {
        return require(INT).getValueAs(Integer.class);
    }

    private String requireString() {
        return require(STRING).getValueAs(String.class);
    }

    private void requireTerminator() {
        if (expects(SEMICOLON)) {
            while (expects(SEMICOLON)) {
                nextToken();
            }
        } else {
            throw unexpected(SEMICOLON);
        }
    }

    private void requireWord(String value) {
        requireWordAt(0, value);
    }

    private String requireWord() {
        return requireWordAt(0);
    }

    private String requireWordAt(int offset, String value) {
        if (expectsWordAt(offset, value)) {
            return requireWordAt(offset);
        } else {
            throw unexpected(WORD, value);
        }
    }

    private String requireWordAt(int offset) {
        if (expectsAt(offset, WORD)) {
            String word = peekAt(offset).getValueAs(String.class);
            nextToken();
            return word;
        } else {
            throw unexpected(WORD);
        }
    }

    private Type reserveType() {
        return t(sequence++);
    }

    private ParseException unexpected(TokenKind wantedKind) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted " + wantedKind + " at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object wantedValue) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with value " + quote(wantedValue) + " at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object... wantedValues) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with one value of"
                + " [" + join(", ", stream(wantedValues).map(TextUtil::quote).collect(toList())) + "] at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(List<TokenKind> wantedKinds) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted one of [" + join(", ", wantedKinds.stream().map(Object::toString).collect(toList())) + "]"
                + " at " + scanner.getPosition()
        );
    }

    private static final class LookAheadScanner {

        private final Scanner     delegate;
        private final List<Token> tokens;
        private       int         position;

        public LookAheadScanner(Scanner delegate) {
            this.delegate = delegate;
            this.tokens = new ArrayList<>();
        }

        public SourcePosition getPosition() {
            return new SourcePosition(delegate.getSource(), peekAt(0).getStart());
        }

        public void nextToken() {
            buffer();
            if (position < tokens.size()) {
                position++;
            }
        }

        public Token peekAt(int offset) {
            buffer();
            if (position + offset < tokens.size()) {
                return tokens.get(position + offset);
            } else {
                return tokens.get(tokens.size() - 1);
            }
        }

        private void buffer() {
            if (tokens.isEmpty()) {
                while (true) {
                    Token token = delegate.nextToken();
                    tokens.add(token);
                    if (token.is(EOF)) {
                        break;
                    }
                }
            }
        }
    }
}
