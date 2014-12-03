package scotch.compiler.parser;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.splitQualified;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.Value.Fixity.PREFIX;
import static scotch.compiler.symbol.Value.Fixity.RIGHT_INFIX;
import static scotch.compiler.syntax.DefinitionEntry.unscopedEntry;
import static scotch.compiler.syntax.Value.Identifier;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.util.StringUtil.quote;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.scanner.Token;
import scotch.compiler.scanner.Token.TokenKind;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.TypeGenerator;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;
import scotch.compiler.text.NamedSourcePoint;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;
import scotch.util.StringUtil;

public class InputParser {

    private static final List<TokenKind> literals = asList(TokenKind.STRING, TokenKind.INT, TokenKind.CHAR, TokenKind.DOUBLE, TokenKind.BOOL);
    private final LookAheadScanner        scanner;
    private final List<DefinitionEntry>   definitions;
    private final Deque<NamedSourcePoint> positions;
    private final SyntaxBuilderFactory    builderFactory;
    private final TypeGenerator           typeGenerator;
    private       String                  currentModule;

    public InputParser(Scanner scanner, SyntaxBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
        this.scanner = new LookAheadScanner(scanner);
        this.definitions = new ArrayList<>();
        this.positions = new ArrayDeque<>();
        this.typeGenerator = new TypeGenerator();
    }

    public DefinitionGraph parse() {
        parseRoot();
        return builderFactory.definitionGraphBuilder(definitions)
            .withSequence(typeGenerator)
            .build();
    }

    private DefinitionReference collect(Definition definition) {
        definitions.add(unscopedEntry(definition));
        return definition.getReference();
    }

    private <D extends Definition, B extends SyntaxBuilder<D>> DefinitionReference definition(Supplier<B> supplier, Consumer<B> consumer) {
        return collect(node(supplier, consumer));
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
        return !expectsModule() && !expects(TokenKind.EOF);
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
        while (!expectsAt(offset, TokenKind.SEMICOLON) && !expectsAt(offset, TokenKind.LCURLY)) {
            if (expectsAt(offset, TokenKind.WORD)) {
                if (expectsAt(offset + 1, TokenKind.COMMA)) {
                    offset += 2;
                } else if (expectsAt(offset + 1, TokenKind.DOUBLE_COLON)) {
                    return true;
                } else {
                    break;
                }
            } else if (expectsAt(offset, TokenKind.LPAREN) && expectsAt(offset + 1, TokenKind.WORD) && expectsAt(offset + 2, TokenKind.RPAREN)) {
                if (expectsAt(offset + 3, TokenKind.COMMA)) {
                    offset += 4;
                } else if (expectsAt(offset + 3, TokenKind.DOUBLE_COLON)) {
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
        return expectsAt(offset, TokenKind.WORD);
    }

    private boolean expectsWordAt(int offset, String value) {
        return expectsAt(offset, TokenKind.WORD) && Objects.equals(scanner.peekAt(offset).getValue(), value);
    }

    private SourceRange getSourceRange() {
        return positions.pop().to(scanner.getPreviousPosition());
    }

    private void markPosition() {
        positions.push(scanner.getPosition());
    }

    private void nextToken() {
        scanner.nextToken();
    }

    private <N, B extends SyntaxBuilder<N>> N node(Supplier<B> supplier, Consumer<B> consumer) {
        markPosition();
        B builder = supplier.get();
        try {
            consumer.accept(builder);
        } catch (RuntimeException exception) {
            positions.pop();
            throw exception;
        }
        return builder.withSourceRange(getSourceRange()).build();
    }

    private List<Type> parseClassArguments() {
        List<Type> arguments = new ArrayList<>();
        do {
            arguments.add(var(requireWord()));
        } while (expectsWord());
        return arguments;
    }

    private DefinitionReference parseClassDefinition() {
        return definition(builderFactory::classBuilder, builder -> {
            requireWord("class");
            builder.withSymbol(qualified(currentModule, requireWord()))
                .withArguments(parseClassArguments())
                .withMembers(parseClassMembers());
        });
    }

    private List<DefinitionReference> parseClassMembers() {
        require(TokenKind.WHERE);
        require(TokenKind.LCURLY);
        List<DefinitionReference> members = new ArrayList<>();
        while (!expects(TokenKind.RCURLY)) {
            if (expectsValueSignatures()) {
                parseValueSignatures().forEach(members::add);
            } else {
                members.add(parseValueDefinition());
            }
            requireTerminator();
        }
        require(TokenKind.RCURLY);
        return members;
    }

    private void parseConstraint(Map<String, List<Symbol>> constraints) {
        Symbol typeClass = fromString(parseQualifiedName());
        String variable = requireWord();
        constraints.computeIfAbsent(variable, k -> new ArrayList<>()).add(typeClass);
    }

    private Map<String, Type> parseConstraints() {
        Map<String, List<Symbol>> constraints = new HashMap<>();
        require(TokenKind.LPAREN);
        parseConstraint(constraints);
        while (expects(TokenKind.COMMA)) {
            nextToken();
            parseConstraint(constraints);
        }
        require(TokenKind.RPAREN);
        require(TokenKind.DOUBLE_ARROW);
        return constraints.entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> var(entry.getKey(), entry.getValue())));
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

    private ParseException parseException(String message) {
        throw new ParseException(message + "; in " + peekSourceRange().prettyPrint());
    }

    private Import parseImport() {
        return node(builderFactory::moduleImportBuilder, builder -> {
            requireWord("import");
            builder.withModuleName(parseQualifiedName());
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
        return node(builderFactory::literalBuilder, builder -> {
            if (expects(TokenKind.STRING)) {
                builder.withValue(requireString());
            } else if (expects(TokenKind.INT)) {
                builder.withValue(requireInt());
            } else if (expects(TokenKind.CHAR)) {
                builder.withValue(requireChar());
            } else if (expects(TokenKind.DOUBLE)) {
                builder.withValue(requireDouble());
            } else if (expects(TokenKind.BOOL)) {
                builder.withValue(requireBool());
            } else {
                throw unexpected(literals);
            }
            builder.withType(reserveType());
        });
    }

    private Optional<PatternMatch> parseMatch(boolean required) {
        PatternMatch match = null;
        if (expectsWord()) {
            match = node(builderFactory::captureMatchBuilder, builder -> builder.withIdentifier(parseWordReference()));
        } else if (expectsLiteral()) {
            match = node(builderFactory::equalMatchBuilder, builder -> builder.withValue(parseLiteral()));
        } else if (required) {
            throw unexpected(TokenKind.WORD);
        }
        return ofNullable(match);
    }

    private Value parseMessage() {
        return node(builderFactory::messageBuilder, builder -> {
            builder.withMember(parseRequiredPrimary());
            Optional<Value> argument = parseOptionalPrimary();
            while (argument.isPresent()) {
                builder.withMember(argument.get());
                argument = parseOptionalPrimary();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private DefinitionReference parseModule() {
        return definition(builderFactory::moduleBuilder, builder -> {
            requireWord("module");
            builder.withSymbol(currentModule = parseQualifiedName());
            requireTerminator();
            builder.withImports(parseImports())
                .withDefinitions(parseModuleDefinitions());
        });
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
        return parseSymbols().stream()
            .map(pair -> pair.into(
                (symbol, sourceRange) -> builderFactory.operatorBuilder()
                    .withSourceRange(sourceRange)
                    .withSymbol(symbol)
                    .withFixity(fixity)
                    .withPrecedence(precedence)
                    .build()
            ))
            .map(this::collect)
            .collect(toList());
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
            throw unexpected(TokenKind.WORD, "left infix", "right infix", "prefix");
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
        } else if (expects(TokenKind.LPAREN)) {
            nextToken();
            value = parseMessage();
            require(TokenKind.RPAREN);
        } else if (required) {
            throw unexpected(ImmutableList.<TokenKind>builder().add(TokenKind.WORD, TokenKind.LPAREN).addAll(literals).build());
        }
        return ofNullable(value);
    }

    private String parseQualifiedName() {
        List<String> parts = new ArrayList<>();
        parts.add(requireWord());
        while (expects(TokenKind.DOT)) {
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

    private void parseRoot() {
        definition(builderFactory::rootBuilder, builder -> {
            if (!expects(TokenKind.EOF)) {
                builder.withModule(parseModule());
                while (expectsModule()) {
                    builder.withModule(parseModule());
                }
            }
            require(TokenKind.EOF);
        });
    }

    private Map<String, Type> parseSignatureConstraints() {
        if (expects(TokenKind.LPAREN)) {
            int offset = 0;
            while (!peekAt(offset).is(TokenKind.SEMICOLON)) {
                if (peekAt(offset).is(TokenKind.RPAREN) && peekAt(offset + 1).is(TokenKind.DOUBLE_ARROW)) {
                    return parseConstraints();
                } else {
                    offset++;
                }
            }
        }
        return emptyMap();
    }

    private Symbol parseSymbol() {
        if (expects(TokenKind.WORD)) {
            return qualified(currentModule, requireWord());
        } else if (expects(TokenKind.LPAREN)) {
            nextToken();
            String symbol = requireWord();
            require(TokenKind.RPAREN);
            return qualified(currentModule, symbol);
        } else {
            throw unexpected(asList(TokenKind.WORD, TokenKind.LPAREN));
        }
    }

    private List<Tuple2<Symbol, SourceRange>> parseSymbols() {
        List<Tuple2<Symbol, SourceRange>> symbols = new ArrayList<>();
        markPosition();
        symbols.add(tuple2(parseSymbol(), getSourceRange()));
        while (expects(TokenKind.COMMA)) {
            nextToken();
            markPosition();
            symbols.add(tuple2(parseSymbol(), getSourceRange()));
        }
        return symbols;
    }

    private Type parseType(Map<String, Type> constraints) {
        return splitQualified(parseQualifiedName()).into((optionalModuleName, memberName) -> {
            try {
                markPosition();
                if (isLowerCase(memberName.charAt(0))) {
                    if (optionalModuleName.isPresent()) {
                        throw parseException("Type name must be uppercase; in " + peekSourceRange().prettyPrint());
                    } else {
                        Type type = var(memberName);
                        return constraints.getOrDefault(memberName, type);
                    }
                } else {
                    return optionalModuleName
                        .map(moduleName -> sum(qualified(moduleName, memberName)))
                        .orElseGet(() -> sum(unqualified(memberName)));
                }
            } finally {
                positions.pop();
            }
        });
    }

    private DefinitionReference parseValueDefinition() {
        if (expectsAt(1, TokenKind.ASSIGN)) {
            return definition(builderFactory::valueDefBuilder, builder -> {
                builder.withSymbol(qualified(currentModule, requireWord()));
                require(TokenKind.ASSIGN);
                builder.withType(reserveType())
                    .withBody(parseMessage());
            });
        } else {
            return definition(builderFactory::unshuffledBuilder, builder -> {
                builder.withMatches(parsePatternMatches());
                require(TokenKind.ASSIGN);
                builder.withSymbol(typeGenerator.reservePattern(currentModule))
                    .withBody(parseMessage());
            });
        }
    }

    private Type parseValueSignature() {
        require(TokenKind.DOUBLE_COLON);
        return parseValueSignature_(parseSignatureConstraints());
    }

    private Type parseValueSignature_(Map<String, Type> constraints) {
        Type type = parseType(constraints);
        if (expects(TokenKind.ARROW)) {
            require(TokenKind.ARROW);
            type = fn(type, parseValueSignature_(constraints));
        }
        return type;
    }

    private List<DefinitionReference> parseValueSignatures() {
        List<Tuple2<Symbol, SourceRange>> symbolList = parseSymbols();
        Type type = parseValueSignature();
        return symbolList.stream()
            .map(pair -> pair.into(
                (symbol, sourceRange) -> builderFactory.signatureBuilder()
                    .withSymbol(symbol)
                    .withType(type)
                    .withSourceRange(sourceRange)
                    .build()
            ))
            .map(this::collect)
            .collect(toList());
    }

    private Identifier parseWordReference() {
        return node(builderFactory::idBuilder, builder -> builder
                .withSymbol(unqualified(requireWord()))
                .withType(reserveType())
        );
    }

    private Token peekAt(int offset) {
        return scanner.peekAt(offset);
    }

    private SourceRange peekSourceRange() {
        return positions.peek().to(scanner.getPosition());
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
        return require(TokenKind.BOOL).getValueAs(Boolean.class);
    }

    private Character requireChar() {
        return require(TokenKind.CHAR).getValueAs(Character.class);
    }

    private Double requireDouble() {
        return require(TokenKind.DOUBLE).getValueAs(Double.class);
    }

    private int requireInt() {
        return require(TokenKind.INT).getValueAs(Integer.class);
    }

    private String requireString() {
        return require(TokenKind.STRING).getValueAs(String.class);
    }

    private void requireTerminator() {
        if (expects(TokenKind.SEMICOLON)) {
            while (expects(TokenKind.SEMICOLON)) {
                nextToken();
            }
        } else {
            throw unexpected(TokenKind.SEMICOLON);
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
            throw unexpected(TokenKind.WORD, value);
        }
    }

    private String requireWordAt(int offset) {
        if (expectsAt(offset, TokenKind.WORD)) {
            String word = peekAt(offset).getValueAs(String.class);
            nextToken();
            return word;
        } else {
            throw unexpected(TokenKind.WORD);
        }
    }

    private Type reserveType() {
        return typeGenerator.reserveType();
    }

    private ParseException unexpected(TokenKind wantedKind) {
        return new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted " + wantedKind + " in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object wantedValue) {
        return new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with value " + quote(wantedValue) + " in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object... wantedValues) {
        return new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with one value of"
                + " [" + join(", ", stream(wantedValues).map(StringUtil::quote).collect(toList())) + "] in " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(List<TokenKind> wantedKinds) {
        return new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted one of [" + join(", ", wantedKinds.stream().map(Object::toString).collect(toList())) + "]"
                + " at " + scanner.getPosition().prettyPrint()
        );
    }

    private static final class LookAheadScanner {

        private final Scanner          delegate;
        private final List<Token>      tokens;
        private       int              position;
        private       NamedSourcePoint previousPosition;

        public LookAheadScanner(Scanner delegate) {
            this.delegate = delegate;
            this.tokens = new ArrayList<>();
        }

        public NamedSourcePoint getPosition() {
            return peekAt(0).getStart();
        }

        public NamedSourcePoint getPreviousPosition() {
            return previousPosition;
        }

        public void nextToken() {
            buffer();
            if (position < tokens.size()) {
                previousPosition = peekAt(0).getEnd();
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
                    if (token.is(TokenKind.EOF)) {
                        break;
                    }
                }
            }
        }
    }
}
