package scotch.compiler.parser;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
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
import static scotch.compiler.syntax.Definition.classDef;
import static scotch.compiler.syntax.Definition.module;
import static scotch.compiler.syntax.Definition.operatorDef;
import static scotch.compiler.syntax.Definition.root;
import static scotch.compiler.syntax.Definition.signature;
import static scotch.compiler.syntax.Definition.unshuffled;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.DefinitionEntry.unscopedEntry;
import static scotch.compiler.syntax.Import.moduleImport;
import static scotch.compiler.syntax.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.syntax.Operator.Fixity.PREFIX;
import static scotch.compiler.syntax.Operator.Fixity.RIGHT_INFIX;
import static scotch.compiler.syntax.PatternMatch.capture;
import static scotch.compiler.syntax.PatternMatch.equal;
import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.syntax.SymbolTable.symbols;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.syntax.Type.t;
import static scotch.compiler.syntax.Type.var;
import static scotch.compiler.syntax.Value.Identifier;
import static scotch.compiler.syntax.Value.ValueVisitor;
import static scotch.compiler.syntax.Value.id;
import static scotch.compiler.syntax.Value.literal;
import static scotch.compiler.syntax.Value.message;
import static scotch.compiler.util.TextUtil.normalizeQualified;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.splitQualified;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ParseException;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.NamedSourcePoint;
import scotch.compiler.syntax.Operator.Fixity;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.SourceAware;
import scotch.compiler.syntax.SourceRange;
import scotch.compiler.syntax.Symbol;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.util.TextUtil;
import scotch.data.tuple.Tuple2;

public class InputParser {

    private static final List<TokenKind> literals = asList(STRING, INT, CHAR, DOUBLE, BOOL);
    private final LookAheadScanner        scanner;
    private final List<DefinitionEntry>   definitions;
    private final Deque<NamedSourcePoint> positions;
    private       String                  currentModule;
    private       int                     sequence;

    public InputParser(Scanner scanner) {
        this.scanner = new LookAheadScanner(scanner);
        this.definitions = new ArrayList<>();
        this.positions = new ArrayDeque<>();
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
        collect(root(modules));
        return symbols(definitions)
            .withSequence(sequence)
            .build();
    }

    private DefinitionReference collect(Definition definition) {
        definitions.add(unscopedEntry(definition));
        return definition.getReference();
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

    private void markPosition() {
        positions.push(scanner.getPosition());
    }

    private SourceRange getSourceRange() {
        return positions.pop().to(scanner.getPosition());
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
        return collect(withSourceRange(() -> {
            requireWord("class");
            String name = requireWord();
            return classDef(qualified(currentModule, name), parseClassArguments(), parseClassMembers());
        }));
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
        return withSourceRange(() -> {
            requireWord("import");
            return moduleImport(parseQualifiedName());
        });
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
        return withSourceRange(() -> {
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
        });
    }

    private Optional<PatternMatch> parseMatch(boolean required) {
        PatternMatch match = null;
        if (expectsWord()) {
            match = withSourceRange(() -> parseWordReference().accept(new ValueVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Identifier identifier) {
                    return capture(identifier.getSymbol(), identifier.getType());
                }

                @Override
                public PatternMatch visitOtherwise(Value value) {
                    throw new UnsupportedOperationException(); // TODO
                }
            }));
        } else if (expectsLiteral()) {
            match = withSourceRange(() -> equal(parseLiteral()));
        } else if (required) {
            throw unexpected(WORD);
        }
        return ofNullable(match);
    }

    private Value parseMessage() {
        return withSourceRange(() -> {
            List<Value> values = new ArrayList<>();
            values.add(parseRequiredPrimary());
            Optional<Value> argument = parseOptionalPrimary();
            while (argument.isPresent()) {
                values.add(argument.get());
                argument = parseOptionalPrimary();
            }
            return message(values);
        });
    }

    @SuppressWarnings("unchecked")
    private DefinitionReference parseModule() {
        return collect(withSourceRange(() -> {
            requireWord("module");
            currentModule = parseQualifiedName();
            requireTerminator();
            List<Import> imports = parseImports();
            List<DefinitionReference> definitions = parseModuleDefinitions();
            return module(currentModule, imports, definitions);
        }));
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
        parseSymbols().forEach(pair -> pair.into((symbol, sourceRange) -> {
            Definition operator = operatorDef(symbol, fixity, precedence).withSourceRange(sourceRange);
            references.add(collect(operator));
            return null;
        }));
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

    private List<Tuple2<Symbol, SourceRange>> parseSymbols() {
        List<Tuple2<Symbol, SourceRange>> symbols = new ArrayList<>();
        markPosition();
        symbols.add(tuple2(parseSymbol(), getSourceRange()));
        while (expects(COMMA)) {
            nextToken();
            markPosition();
            symbols.add(tuple2(parseSymbol(), getSourceRange()));
        }
        return symbols;
    }

    private Type parseType() {
        return splitQualified(parseQualifiedName()).into((optionalModuleName, memberName) -> {
            try {
                markPosition();
                if (isLowerCase(memberName.charAt(0))) {
                    if (optionalModuleName.isPresent()) {
                        throw parseException("Type name must be uppercase; in " + peekSourceRange().prettyPrint());
                    } else {
                        return var(memberName);
                    }
                } else {
                    return sum(normalizeQualified(optionalModuleName, memberName));
                }
            } finally {
                positions.pop();
            }
        });
    }

    private SourceRange peekSourceRange() {
        return positions.peek().to(scanner.getPosition());
    }

    private ParseException parseException(String message) {
        throw new ParseException(message + "; in " + peekSourceRange().prettyPrint());
    }

    private DefinitionReference parseValueDefinition() {
        return collect(withSourceRange(() -> {
            if (expectsAt(1, ASSIGN)) {
                String name = requireWord();
                nextToken();
                return value(qualified(currentModule, name), reserveType(), parseMessage());
            } else {
                List<PatternMatch> patternMatches = parsePatternMatches();
                require(ASSIGN);
                return unshuffled(qualified(currentModule, "pattern#" + sequence++), patternMatches, parseMessage());
            }
        }));
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
        List<Tuple2<Symbol, SourceRange>> symbolList = parseSymbols();
        Type type = parseValueSignature();
        symbolList.forEach(pair -> pair.into((symbol, sourceRange) -> {
            Definition signature = signature(symbol, type).withSourceRange(sourceRange);
            references.add(collect(signature));
            return null;
        }));
        return references;
    }

    private Value parseWordReference() {
        return withSourceRange(() -> {
            String word = requireWord();
            return id(word, reserveType());
        });
    }

    private <T extends SourceAware<T>> T withSourceRange(Supplier<T> supplier) {
        markPosition();
        T result;
        try {
            result = supplier.get();
        } catch (RuntimeException exception) {
            positions.pop();
            throw exception;
        }
        return result.withSourceRange(getSourceRange());
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
                + "; wanted " + wantedKind + " in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object wantedValue) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with value " + quote(wantedValue) + " in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object... wantedValues) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with one value of"
                + " [" + join(", ", stream(wantedValues).map(TextUtil::quote).collect(toList())) + "] in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(List<TokenKind> wantedKinds) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted one of [" + join(", ", wantedKinds.stream().map(Object::toString).collect(toList())) + "]"
                + " at " + scanner.getPosition().prettyPrint()
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

        public NamedSourcePoint getPosition() {
            return peekAt(0).getStart();
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
