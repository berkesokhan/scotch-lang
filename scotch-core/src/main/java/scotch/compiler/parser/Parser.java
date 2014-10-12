package scotch.compiler.parser;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.Definition.DefinitionVisitor;
import static scotch.compiler.ast.Definition.ModuleDefinition;
import static scotch.compiler.ast.Definition.ValueDefinition;
import static scotch.compiler.ast.Definition.ValueSignature;
import static scotch.compiler.ast.Definition.classDef;
import static scotch.compiler.ast.Definition.module;
import static scotch.compiler.ast.Definition.operatorDef;
import static scotch.compiler.ast.Definition.root;
import static scotch.compiler.ast.Definition.signature;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionEntry.scopedEntry;
import static scotch.compiler.ast.DefinitionEntry.unscopedEntry;
import static scotch.compiler.ast.DefinitionReference.classRef;
import static scotch.compiler.ast.DefinitionReference.moduleRef;
import static scotch.compiler.ast.DefinitionReference.opRef;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.DefinitionReference.signatureRef;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.Import.moduleImport;
import static scotch.compiler.ast.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.ast.Operator.Fixity.PREFIX;
import static scotch.compiler.ast.Operator.Fixity.RIGHT_INFIX;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatch.equal;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.Apply;
import static scotch.compiler.ast.Value.Identifier;
import static scotch.compiler.ast.Value.Message;
import static scotch.compiler.ast.Value.ValueVisitor;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.message;
import static scotch.compiler.ast.Value.patterns;
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
import static scotch.lang.Either.EitherVisitor;
import static scotch.lang.Either.left;
import static scotch.lang.Either.right;
import static scotch.lang.Type.boolType;
import static scotch.lang.Type.charType;
import static scotch.lang.Type.doubleType;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.intType;
import static scotch.lang.Type.stringType;
import static scotch.lang.Type.var;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.Operator.Fixity;
import scotch.compiler.ast.PatternMatch;
import scotch.compiler.ast.PatternMatch.CaptureMatch;
import scotch.compiler.ast.PatternMatch.PatternMatchVisitor;
import scotch.compiler.ast.PatternMatcher;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.Symbol;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.ast.Value.LiteralValue;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.util.SourcePosition;
import scotch.compiler.util.TextUtil;
import scotch.lang.Either;
import scotch.lang.Type;
import scotch.lang.Type.FunctionType;
import scotch.lang.Type.TypeVisitor;
import scotch.lang.Type.UnionLookup;

public class Parser {

    private static final List<TokenKind> literals = asList(STRING, INT, CHAR, DOUBLE, BOOL);
    private final LookAheadScanner                       scanner;
    private final ScopeBuilder                           scopeBuilder;
    private final List<DefinitionEntry>                  definitions;
    private final Map<DefinitionReference, PatternEntry> patterns;
    private       String                                 currentModule;

    public Parser(Scanner scanner) {
        this.scanner = new LookAheadScanner(scanner);
        this.scopeBuilder = new ScopeBuilder();
        this.definitions = new ArrayList<>();
        this.patterns = new HashMap<>();
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
        collectSymbol(scopedEntry(rootRef(), root(modules), scopeBuilder.getScope()));
        patterns.forEach((reference, entry) -> createScope(
            reference,
            value(entry.name, entry.type, patterns(entry.patterns)),
            entry.scope
        ));
        return new SymbolTable(definitions);
    }

    private void addPattern(PatternEntry patternEntry) {
        patterns.put(patternEntry.getReference(), patternEntry);
    }

    private Value bindType(String name, Value body) {
        return body.accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Apply apply) {
                return apply
                    .withFunction(bindType(name, apply.getFunction()))
                    .withArgument(bindType(name, apply.getArgument()));
            }

            @Override
            public Value visit(Identifier identifier) {
                if (name.equals(identifier.getName())) {
                    return identifier.withType(getValueType(name));
                } else {
                    return identifier;
                }
            }

            @Override
            public Value visit(LiteralValue value) {
                return value;
            }

            @Override
            public Value visit(Message message) {
                return message.withMembers(
                    message.getMembers().stream()
                        .map(member -> bindType(name, member))
                        .collect(toList())
                );
            }

            @Override
            public Value visitOtherwise(Value value) {
                throw new UnsupportedOperationException("Can't bind pattern type to " + value.getClass().getSimpleName());
            }
        });
    }

    private void collectSymbol(DefinitionEntry entry) {
        definitions.add(entry);
    }

    private DefinitionReference createScope(Definition definition) {
        Scope scope = scopeBuilder.getScope();
        return definition.accept(new DefinitionVisitor<DefinitionReference>() {
            @Override
            public DefinitionReference visit(ValueSignature signature) {
                return createScope(signatureRef(currentModule, signature.getName()), signature, scope);
            }

            @Override
            public DefinitionReference visit(ModuleDefinition definition) {
                scope.setImports(definition.getImports());
                return createScope(moduleRef(definition.getName()), definition, scope);
            }

            @Override
            public DefinitionReference visit(ValueDefinition definition) {
                return createScope(getValueRef(definition.getName()), definition, scope);
            }
        });
    }

    private DefinitionReference createScope(DefinitionReference reference, Definition definition, Scope scope) {
        scope.setReference(reference);
        scope.pruneReferences();
        collectSymbol(scopedEntry(reference, definition, scope));
        return reference;
    }

    private void define(Definition operator) {
        scopeBuilder.define(operator);
    }

    private void define(String symbol, Type type) {
        scopeBuilder.define(symbol, type);
    }

    private Type define(String name) {
        Type type = reserveType();
        define(name, type);
        return type;
    }

    private boolean expects(TokenKind kind) {
        return expectsAt(0, kind);
    }

    private boolean expectsArgument(Deque input) {
        return !input.isEmpty() && expectsArgument_(input);
    }

    private boolean expectsArgument_(Deque input) {
        if (input.peek() instanceof PatternMatch) {
            return !isOperator((PatternMatch) input.peek());
        } else {
            return !isOperator((Value) input.peek());
        }
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

    private Symbol forwardReference(String name) {
        return scopeBuilder.forwardReference(name);
    }

    private OperatorPair<CaptureMatch> getOperator(PatternMatch match, boolean expectsPrefix) {
        return match.accept(new PatternMatchVisitor<OperatorPair<CaptureMatch>>() {
            @Override
            public OperatorPair<CaptureMatch> visit(CaptureMatch match) {
                Operator operator = scopeBuilder.getOperator(match.getName());
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + quote(match.getName()));
                }
                return new OperatorPair<>(operator, match);
            }
        });
    }

    private OperatorPair<Identifier> getOperator(Value value, boolean expectsPrefix) {
        return value.accept(new ValueVisitor<OperatorPair<Identifier>>() {
            @Override
            public OperatorPair<Identifier> visit(Identifier identifier) {
                Operator operator = scopeBuilder.getOperator(identifier.getName());
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + quote(identifier.getName()));
                }
                return new OperatorPair<>(scopeBuilder.getOperator(identifier.getName()), identifier);
            }
        });
    }

    private PatternEntry getPattern(DefinitionReference reference) {
        return patterns.get(reference);
    }

    private Type getType(String name) {
        return scopeBuilder.getType(name);
    }

    private DefinitionReference getValueRef(String name) {
        return valueRef(currentModule, name);
    }

    private Type getValueType(String name) {
        return scopeBuilder.getValueType(name);
    }

    private void importValueType(String name) {
        getValueType(name).accept(new TypeVisitor<Void>() {
            @Override
            public Void visit(UnionLookup lookup) {
                getValueType(lookup.getName());
                return null;
            }

            @Override
            public Void visit(FunctionType functionType) {
                functionType.getArgument().accept(this);
                functionType.getResult().accept(this);
                return null;
            }

            @Override
            public Void visitOtherwise(Type type) {
                return null;
            }
        });
    }

    private boolean isOperator(PatternMatch match) {
        return match.accept(new PatternMatchVisitor<Boolean>() {
            @Override
            public Boolean visit(CaptureMatch match) {
                return scopeBuilder.isOperator(match.getName());
            }

            @Override
            public Boolean visitOtherwise(PatternMatch match) {
                return false;
            }
        });
    }

    private boolean isOperator(Value value) {
        return value.accept(new ValueVisitor<Boolean>() {
            @Override
            public Boolean visit(Identifier identifier) {
                return scopeBuilder.isOperator(identifier.getName());
            }

            @Override
            public Boolean visitOtherwise(Value value) {
                return false;
            }
        });
    }

    private boolean isPattern(DefinitionReference reference) {
        return patterns.containsKey(reference);
    }

    private <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, Deque<OperatorPair<T>> stack) {
        return !stack.isEmpty() && nextOperatorHasGreaterPrecedence(current, stack.peek());
    }

    private <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, OperatorPair<T> next) {
        return current.isLeftAssociative() && current.hasSamePrecedenceAs(next)
            || current.hasLessPrecedenceThan(next);
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
                parseValueDefinition().ifPresent(members::add);
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
            parseValueDefinition().ifPresent(definitions::add);
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
            return literal(requireString(), stringType);
        } else if (expects(INT)) {
            return literal(requireInt(), intType);
        } else if (expects(CHAR)) {
            return literal(requireChar(), charType);
        } else if (expects(DOUBLE)) {
            return literal(requireDouble(), doubleType);
        } else if (expects(BOOL)) {
            return literal(requireBool(), boolType);
        } else {
            throw unexpected(literals);
        }
    }

    private Optional<PatternMatch> parseMatch(boolean required) {
        PatternMatch match = null;
        if (expectsWord()) {
            match = parseWordDeclaration().accept(new ValueVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Identifier identifier) {
                    return capture(identifier.getName(), identifier.getType());
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
        List<Value> message = new ArrayList<>();
        message.add(parseRequiredPrimary());
        Optional<Value> argument = parseOptionalPrimary();
        while (argument.isPresent()) {
            message.add(argument.get());
            argument = parseOptionalPrimary();
        }
        return shuffleMessage(message);
    }

    @SuppressWarnings("unchecked")
    private DefinitionReference parseModule() {
        return scoped(() -> {
            requireWord("module");
            currentModule = parseQualifiedName();
            requireTerminator();
            List<Import> imports = parseImports();
            List<DefinitionReference> definitions = parseModuleDefinitions();
            return createScope(module(currentModule, imports, definitions));
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
        List<DefinitionReference> references = new ArrayList<>();
        parseSymbols().forEach(name -> {
            Definition operator = operatorDef(name, fixity, precedence);
            define(operator);
            DefinitionReference reference = opRef(currentModule, name);
            collectSymbol(unscopedEntry(reference, operator));
            references.add(reference);
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

    private Optional<DefinitionReference> parsePatternDefinition() {
        List<PatternMatch> patternMatches = shufflePattern(parsePatternMatches());
        CaptureMatch head = patternMatches.get(0).accept(new PatternMatchVisitor<CaptureMatch>() {
            @Override
            public CaptureMatch visit(CaptureMatch match) {
                patternMatches.remove(0);
                return match;
            }

            @Override
            public CaptureMatch visitOtherwise(PatternMatch match) {
                throw new ParseException("Bad pattern match"); // TODO better message
            }
        });
        String name = head.getName();
        DefinitionReference patternReference = getValueRef(name);
        Type type = pullUp(name, () -> {
            if (scopeBuilder.isLocal(name)) {
                return getValueType(name);
            } else {
                define(name, head.getType());
                return head.getType();
            }
        });
        importValueType(name);
        if (!isPattern(patternReference)) {
            addPattern(new PatternEntry(currentModule, name, type, scopeBuilder.getScope()));
        }
        require(ASSIGN);
        getPattern(patternReference).patterns.add(pattern(patternMatches, bindType(name, unwrap(parseMessage()))));
        if (getPattern(patternReference).isReferenced()) {
            return Optional.empty();
        } else {
            getPattern(patternReference).setReferenced(true);
            return Optional.of(valueRef(currentModule, name));
        }
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
            throw unexpected(asList(WORD, STRING, INT, CHAR, DOUBLE, BOOL));
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

    private String parseSymbol() {
        if (expects(WORD)) {
            return requireWord();
        } else if (expects(LPAREN)) {
            nextToken();
            String symbol = requireWord();
            require(RPAREN);
            return symbol;
        } else {
            throw unexpected(asList(WORD, LPAREN));
        }
    }

    private List<String> parseSymbols() {
        List<String> symbols = new ArrayList<>();
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
                return getType(normalizeQualified(optionalModuleName, memberName));
            }
        });
    }

    private Optional<DefinitionReference> parseValueDefinition() {
        if (expectsAt(1, ASSIGN)) {
            String name = requireWord();
            Type type = define(name);
            nextToken();
            return scoped(() -> Optional.of(createScope(value(name, type, bindType(name, unwrap(parseMessage()))))));
        } else {
            return scoped(this::parsePatternDefinition);
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
        List<String> symbolList = parseSymbols();
        Type type = parseValueSignature();
        symbolList.forEach(symbol -> {
            define(symbol, type);
            Definition signature = signature(symbol, type);
            references.add(createScope(signature));
        });
        return references;
    }

    private Value parseWordDeclaration() {
        String word = requireWord();
        return id(word, define(word));
    }

    private Value parseWordReference() {
        String word = requireWord();
        return id(word, getValueType(word));
    }

    private Token peekAt(int offset) {
        return scanner.peekAt(offset);
    }

    private <T> T pullUp(String name, Supplier<T> supplier) {
        return scopeBuilder.pullUp(name, supplier);
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
        return scopeBuilder.reserveType();
    }

    private <T> T scoped(Supplier<T> supplier) {
        scopeBuilder.enterScope();
        try {
            return supplier.get();
        } finally {
            scopeBuilder.leaveScope();
        }
    }

    private Value shuffleMessage(List<Value> message) {
        if (message.size() == 1) {
            return message(message);
        } else {
            return shuffleMessage_(message);
        }
    }

    private Value shuffleMessageApply(Deque<Either<OperatorPair<Identifier>, Value>> message) {
        Deque<Value> stack = new ArrayDeque<>();
        while (!message.isEmpty()) {
            stack.push(message.pollLast().getRightOr(pair -> {
                if (pair.isPrefix()) {
                    return apply(pair.getValue(), stack.pop(), reserveType());
                } else {
                    Value right = stack.pop();
                    Value left = stack.pop();
                    return apply(apply(pair.getValue(), left, reserveType()), right, reserveType());
                }
            }));
        }
        if (stack.size() > 1) {
            stack.push(apply(stack.pollLast(), stack.pollLast(), reserveType()));
        }
        return stack.pop();
    }

    private Value shuffleMessage_(List<Value> message) {
        Deque<Value> input = new ArrayDeque<>(message);
        Deque<Either<OperatorPair<Identifier>, Value>> output = new ArrayDeque<>();
        Deque<OperatorPair<Identifier>> stack = new ArrayDeque<>();
        boolean expectsPrefix = isOperator(input.peek());
        while (!input.isEmpty()) {
            if (isOperator(input.peek())) {
                OperatorPair<Identifier> o1 = getOperator(input.poll(), expectsPrefix);
                while (nextOperatorHasGreaterPrecedence(o1, stack)) {
                    output.push(left(stack.pop()));
                }
                stack.push(o1);
                expectsPrefix = isOperator(input.peek());
            } else {
                output.push(right(shuffleNext(input)));
                while (expectsArgument(input)) {
                    output.push(right(apply(
                        output.pop().getRightOr(OperatorPair::getValue),
                        shuffleNext(input),
                        reserveType()
                    )));
                }
            }
        }
        while (!stack.isEmpty()) {
            output.push(left(stack.pop()));
        }
        return shuffleMessageApply(output);
    }

    private Value shuffleNext(Deque<Value> input) {
        return input.poll().accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Message message) {
                return shuffleMessage(message.getMembers());
            }

            @Override
            public Value visitOtherwise(Value value) {
                return value;
            }
        });
    }

    private List<PatternMatch> shufflePattern(List<PatternMatch> matches) {
        Deque<PatternMatch> input = new ArrayDeque<>(matches);
        Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> output = new ArrayDeque<>();
        Deque<OperatorPair<CaptureMatch>> stack = new ArrayDeque<>();
        boolean expectsPrefix = isOperator(input.peek());
        while (!input.isEmpty()) {
            if (isOperator(input.peek())) {
                OperatorPair<CaptureMatch> o1 = getOperator(input.poll(), expectsPrefix);
                while (nextOperatorHasGreaterPrecedence(o1, stack)) {
                    output.push(left(stack.pop()));
                }
                stack.push(o1);
                expectsPrefix = isOperator(input.peek());
            } else {
                output.push(right(input.poll()));
                while (expectsArgument(input)) {
                    output.push(right(input.poll()));
                }
            }
        }
        while (!stack.isEmpty()) {
            output.push(left(stack.pop()));
        }
        return shufflePatternApply(output);
    }

    private List<PatternMatch> shufflePatternApply(Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> input) {
        Deque<PatternMatch> output = new ArrayDeque<>();
        while (!input.isEmpty()) {
            input.pollLast().accept(new EitherVisitor<OperatorPair<CaptureMatch>, PatternMatch, Void>() {
                @Override
                public Void visitLeft(OperatorPair<CaptureMatch> left) {
                    if (left.isPrefix()) {
                        PatternMatch head = output.pop();
                        output.push(left.getValue());
                        output.push(head);
                    } else {
                        PatternMatch l = output.pop();
                        PatternMatch r = output.pop();
                        output.push(left.getValue());
                        output.push(r);
                        output.push(l);
                    }
                    return null;
                }

                @Override
                public Void visitRight(PatternMatch right) {
                    output.push(right);
                    return null;
                }
            });
        }
        List<PatternMatch> matches = new ArrayList<>(output);
        reverse(matches);
        return matches;
    }

    private ParseException unexpected(TokenKind kind) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted " + kind + " at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(TokenKind kind, Object value) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + kind + " with value " + quote(value) + " at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(TokenKind kind, Object... values) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + kind + " with one value of"
                + " [" + join(", ", stream(values).map(TextUtil::quote).collect(toList())) + "] at " + scanner.getPosition()
        );
    }

    private ParseException unexpected(List<TokenKind> kinds) {
        throw new ParseException(
            "Unexpected token " + scanner.peekAt(0).getKind()
                + "; wanted one of [" + join(", ", kinds.stream().map(Object::toString).collect(toList())) + "]"
                + " at " + scanner.getPosition()
        );
    }

    private Value unwrap(Value value) {
        return value.accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Message message) {
                if (message.size() == 1) {
                    return message.getMembers().get(0);
                } else {
                    return message;
                }
            }

            @Override
            public Value visit(Apply apply) {
                return apply.withFunction(unwrap(apply.getFunction()))
                    .withArgument(unwrap(apply.getArgument()));
            }

            @Override
            public Value visitOtherwise(Value value) {
                return value;
            }
        });
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

    private static final class OperatorPair<T> {

        private final Operator operator;
        private final T        value;

        private OperatorPair(Operator operator, T value) {
            this.operator = operator;
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public boolean hasLessPrecedenceThan(OperatorPair other) {
            return operator.hasLessPrecedenceThan(other.operator);
        }

        public boolean hasSamePrecedenceAs(OperatorPair other) {
            return operator.hasSamePrecedenceAs(other.operator);
        }

        public boolean isLeftAssociative() {
            return operator.isLeftAssociative();
        }

        public boolean isPrefix() {
            return operator.isPrefix();
        }
    }

    private static final class PatternEntry {

        public final String               moduleName;
        public final String               name;
        public final Type                 type;
        public final Scope                scope;
        public final List<PatternMatcher> patterns;
        private      boolean              referenced;

        private PatternEntry(String moduleName, String name, Type type, Scope scope) {
            this.moduleName = moduleName;
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.patterns = new ArrayList<>();
        }

        public DefinitionReference getReference() {
            return valueRef(moduleName, name);
        }

        public boolean isReferenced() {
            return referenced;
        }

        public void setReferenced(boolean referenced) {
            this.referenced = referenced;
        }
    }
}
