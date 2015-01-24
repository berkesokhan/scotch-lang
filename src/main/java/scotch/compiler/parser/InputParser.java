package scotch.compiler.parser;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static scotch.compiler.scanner.Token.TokenKind.ARROW;
import static scotch.compiler.scanner.Token.TokenKind.ASSIGN;
import static scotch.compiler.scanner.Token.TokenKind.BOOL;
import static scotch.compiler.scanner.Token.TokenKind.CHAR;
import static scotch.compiler.scanner.Token.TokenKind.COMMA;
import static scotch.compiler.scanner.Token.TokenKind.DOT;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_ARROW;
import static scotch.compiler.scanner.Token.TokenKind.DOUBLE_COLON;
import static scotch.compiler.scanner.Token.TokenKind.ELSE;
import static scotch.compiler.scanner.Token.TokenKind.EOF;
import static scotch.compiler.scanner.Token.TokenKind.IF;
import static scotch.compiler.scanner.Token.TokenKind.IN;
import static scotch.compiler.scanner.Token.TokenKind.INT;
import static scotch.compiler.scanner.Token.TokenKind.LAMBDA;
import static scotch.compiler.scanner.Token.TokenKind.LCURLY;
import static scotch.compiler.scanner.Token.TokenKind.LET;
import static scotch.compiler.scanner.Token.TokenKind.LPAREN;
import static scotch.compiler.scanner.Token.TokenKind.PIPE;
import static scotch.compiler.scanner.Token.TokenKind.RCURLY;
import static scotch.compiler.scanner.Token.TokenKind.RPAREN;
import static scotch.compiler.scanner.Token.TokenKind.SEMICOLON;
import static scotch.compiler.scanner.Token.TokenKind.STRING;
import static scotch.compiler.scanner.Token.TokenKind.THEN;
import static scotch.compiler.scanner.Token.TokenKind.WHERE;
import static scotch.compiler.scanner.Token.TokenKind.WORD;
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
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.definition.DefinitionGraph.createGraph;
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
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.DataConstructorDefinition;
import scotch.compiler.syntax.definition.DataConstructorDefinition.Builder;
import scotch.compiler.syntax.definition.DataFieldDefinition;
import scotch.compiler.syntax.definition.DataTypeDefinition;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.definition.ModuleDefinition;
import scotch.compiler.syntax.definition.ModuleImport;
import scotch.compiler.syntax.definition.OperatorDefinition;
import scotch.compiler.syntax.definition.RootDefinition;
import scotch.compiler.syntax.definition.ScopeDefinition;
import scotch.compiler.syntax.definition.UnshuffledDefinition;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.definition.ValueSignature;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.CaptureMatch.CaptureMatchBuilder;
import scotch.compiler.syntax.value.CaptureMatch.ClassDefinitionBuilder;
import scotch.compiler.syntax.value.Conditional;
import scotch.compiler.syntax.value.Constant;
import scotch.compiler.syntax.value.DataConstructor;
import scotch.compiler.syntax.value.EqualMatch.EqualMatchBuilder;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Initializer;
import scotch.compiler.syntax.value.InitializerField;
import scotch.compiler.syntax.value.Let;
import scotch.compiler.syntax.value.Literal;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.NamedSourcePoint;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;
import scotch.util.StringUtil;

public class InputParser {

    private static final List<TokenKind> literals = asList(STRING, INT, CHAR, DOUBLE, BOOL);
    private final LookAheadScanner        scanner;
    private final List<DefinitionEntry>   definitions;
    private final Deque<NamedSourcePoint> positions;
    private final SymbolGenerator         symbolGenerator;
    private final Deque<Scope>            scopes;
    private final Deque<List<String>>     memberNames;
    private       String                  currentModule;

    public InputParser(SymbolResolver resolver, Scanner scanner) {
        this.scanner = new LookAheadScanner(scanner);
        this.definitions = new ArrayList<>();
        this.positions = new ArrayDeque<>();
        this.symbolGenerator = new SymbolGenerator();
        this.scopes = new ArrayDeque<>(asList(Scope.scope(symbolGenerator, resolver)));
        this.memberNames = new ArrayDeque<>(asList(ImmutableList.of()));
    }

    public DefinitionGraph parse() {
        parseRoot();
        return createGraph(definitions)
            .withSequence(symbolGenerator)
            .build();
    }

    private DefinitionReference collect(Definition definition) {
        definitions.add(entry(scope(), definition));
        return definition.getReference();
    }

    private DefinitionReference createConstructor(DataConstructorDefinition constructor) {
        return scoped(() -> definition(ValueDefinition.builder(),
            value -> value
                .withSymbol(constructor.getSymbol())
                .withBody(createConstructorBody(constructor))
                .withType(reserveType())
        ));
    }

    private Value createConstructorBody(DataConstructorDefinition constructor) {
        if (constructor.isNiladic()) {
            return node(Constant.builder(),
                constant -> constant
                    .withDataType(constructor.getDataType())
                    .withSymbol(constructor.getSymbol())
                    .withType(reserveType()));
        } else {
            return scoped(() -> node(
                FunctionValue.builder(),
                function -> definition(
                    ScopeDefinition.builder(),
                    scope -> {
                        Symbol symbol = reserveSymbol();
                        scope.withSymbol(symbol);
                        function
                            .withSymbol(symbol)
                            .withArguments(constructor.getFields().stream()
                                .map(field -> field.toArgument(scope()))
                                .collect(toList()))
                            .withBody(node(DataConstructor.builder(),
                                ctor -> ctor
                                    .withSymbol(constructor.getSymbol())
                                    .withType(reserveType())
                                    .withArguments(constructor.getFields().stream()
                                        .map(field -> field.toValue(scope()))
                                        .collect(toList()))));
                    })));
        }
    }

    private <D extends Definition, B extends SyntaxBuilder<D>> DefinitionReference definition(B supplier, Consumer<B> consumer) {
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

    private boolean expectsDataDefinition() {
        return expectsWord("data");
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

    private SourceRange getSourceRange() {
        return positions.pop().to(scanner.getPreviousPosition());
    }

    private void markPosition() {
        positions.push(scanner.getPosition());
    }

    private void nextToken() {
        scanner.nextToken();
    }

    private <N, B extends SyntaxBuilder<N>> N node(B builder, Consumer<B> consumer) {
        markPosition();
        try {
            consumer.accept(builder);
        } catch (RuntimeException exception) {
            positions.pop();
            throw exception;
        }
        return builder.withSourceRange(getSourceRange()).build();
    }

    private DataFieldDefinition parseAnonymousField(int offset) {
        return node(DataFieldDefinition.builder(),
            builder -> builder.withName("_" + offset).withType(parseType()));
    }

    private Argument parseArgument() {
        return node(Argument.builder(), builder -> builder
            .withName(requireWord())
            .withType(reserveType()));
    }

    private List<Argument> parseArguments() {
        List<Argument> arguments = new ArrayList<>();
        arguments.add(parseArgument());
        while (expectsWord()) {
            arguments.add(parseArgument());
        }
        return arguments;
    }

    private List<Type> parseClassArguments() {
        List<Type> arguments = new ArrayList<>();
        do {
            arguments.add(var(requireWord()));
        } while (expectsWord());
        return arguments;
    }

    private DefinitionReference parseClassDefinition() {
        return definition(new ClassDefinitionBuilder(), builder -> {
            requireWord("class");
            builder.withSymbol(qualify(requireWord()))
                .withArguments(parseClassArguments())
                .withMembers(parseClassMembers());
        });
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

    private Value parseConditional() {
        return node(Conditional.builder(), conditional -> {
            require(IF);
            conditional.withCondition(parseExpression().collapse());
            require(THEN);
            conditional.withWhenTrue(parseExpression().collapse());
            require(ELSE);
            conditional.withWhenFalse(parseExpression().collapse());
            conditional.withType(reserveType());
        });
    }

    private void parseConstraint(Map<String, List<Symbol>> constraints) {
        Symbol typeClass = fromString(parseQualifiedName());
        String variable = requireWord();
        constraints.computeIfAbsent(variable, k -> new ArrayList<>()).add(typeClass);
    }

    private Map<String, Type> parseConstraints() {
        Map<String, List<Symbol>> constraints = new HashMap<>();
        require(LPAREN);
        parseConstraint(constraints);
        while (expects(COMMA)) {
            nextToken();
            parseConstraint(constraints);
        }
        require(RPAREN);
        require(DOUBLE_ARROW);
        return constraints.entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> var(entry.getKey(), entry.getValue())));
    }

    private DataConstructorDefinition parseDataConstructor(Symbol dataType) {
        return node(DataConstructorDefinition.builder(), builder -> {
            if (expects(LCURLY)) {
                builder
                    .withSymbol(dataType)
                    .withDataType(dataType);
                parseDataFields(builder);
            } else {
                builder
                    .withSymbol(qualify(requireWord()))
                    .withDataType(dataType);
                if (expects(LCURLY)) {
                    parseDataFields(builder);
                } else {
                    int offset = 0;
                    while (expectsWord()) {
                        builder.addField(parseAnonymousField(offset++));
                    }
                }
            }
        });
    }

    private void parseDataFields(Builder builder) {
        require(LCURLY);
        builder.addField(parseNamedField());
        while (expects(COMMA) && expectsWordAt(1)) {
            require(COMMA);
            builder.addField(parseNamedField());
        }
        if (expects(COMMA)) {
            require(COMMA);
        }
        require(RCURLY);
    }

    private List<DefinitionReference> parseDataType() {
        List<DataConstructorDefinition> constructors = new ArrayList<>();
        DefinitionReference definition = definition(DataTypeDefinition.builder(), builder -> {
            requireWord("data");
            Symbol symbol = qualify(requireWord());
            builder.withSymbol(symbol);
            while (!expects(ASSIGN) && !expects(LCURLY)) {
                builder.addParameter(parseType());
            }
            if (expects(LCURLY)) {
                constructors.add(parseDataConstructor(symbol));
            } else {
                require(ASSIGN);
                constructors.add(parseDataConstructor(symbol));
                while (expects(PIPE)) {
                    require(PIPE);
                    constructors.add(parseDataConstructor(symbol));
                }
            }
            constructors.forEach(builder::addConstructor);
        });
        return new ArrayList<DefinitionReference>() {{
            add(definition);
            constructors.stream()
                .map(InputParser.this::createConstructor)
                .forEach(this::add);
        }};
    }

    private List<DefinitionReference> parseDefinitions() {
        List<DefinitionReference> definitions = new ArrayList<>();
        if (expectsClassDefinition()) {
            definitions.add(parseClassDefinition());
        } else if (expectsOperatorDefinition()) {
            definitions.addAll(parseOperatorDefinition());
        } else if (expectsDataDefinition()) {
            definitions.addAll(parseDataType());
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

    private Value parseExpression() {
        return node(UnshuffledValue.builder(), builder -> {
            builder.withMember(parseRequiredPrimary());
            Optional<Value> argument = parseOptionalPrimary();
            while (argument.isPresent()) {
                builder.withMember(argument.get());
                argument = parseOptionalPrimary();
            }
        });
    }

    private InitializerField parseField() {
        return node(InitializerField.builder(), builder -> {
            builder.withName(requireWord());
            require(ASSIGN);
            builder.withValue(parseExpression().collapse());
        });
    }

    private List<InitializerField> parseFields() {
        List<InitializerField> fields = new ArrayList<>();
        require(LCURLY);
        if (expectsWord()) {
            fields.add(parseField());
            while (expects(COMMA) && expectsWordAt(1)) {
                require(COMMA);
                fields.add(parseField());
            }
            if (expects(COMMA)) {
                require(COMMA);
            }
        }
        require(RCURLY);
        return fields;
    }

    private FunctionValue parseFunction() {
        return scoped(() -> node(FunctionValue.builder(),
            function -> definition(ScopeDefinition.builder(), scope -> {
                Symbol symbol = reserveSymbol();
                require(LAMBDA);
                function.withSymbol(symbol);
                function.withArguments(parseArguments());
                require(ARROW);
                function.withBody(parseExpression());
                scope.withSymbol(symbol);
            })
        ));
    }

    private Import parseImport() {
        return node(ModuleImport.builder(), builder -> {
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

    private Value parseInitializer_(Value value) {
        if (expects(LCURLY)) {
            return node(Initializer.builder(), builder -> {
                builder.withType(reserveType());
                builder.withValue(value);
                parseFields().forEach(builder::addField);
            });
        } else {
            return value;
        }
    }

    private Value parseLet() {
        return scoped(() -> node(Let.builder(),
            let -> definition(ScopeDefinition.builder(), scope -> {
                Symbol symbol = reserveSymbol();
                List<DefinitionReference> definitions = new ArrayList<>();
                require(LET);
                require(LCURLY);
                while (!expects(RCURLY)) {
                    if (expectsValueSignatures()) {
                        definitions.addAll(parseValueSignatures());
                    } else {
                        definitions.add(parseValueDefinition());
                    }
                    require(SEMICOLON);
                }
                require(RCURLY);
                require(IN);
                scope.withSymbol(symbol);
                let.withSymbol(symbol)
                    .withDefinitions(definitions)
                    .withBody(parseExpression());
            })
        ));
    }

    private Value parseLiteral() {
        return node(Literal.builder(), builder -> {
            if (expects(STRING)) {
                builder.withValue(requireString());
            } else if (expects(INT)) {
                builder.withValue(requireInt());
            } else if (expects(CHAR)) {
                builder.withValue(requireChar());
            } else if (expects(DOUBLE)) {
                builder.withValue(requireDouble());
            } else if (expects(BOOL)) {
                builder.withValue(requireBool());
            } else {
                throw unexpected(literals);
            }
        });
    }

    private Optional<PatternMatch> parseMatch(boolean required) {
        PatternMatch match = null;
        if (expectsWord()) {
            match = node(new CaptureMatchBuilder(), builder -> builder.withIdentifier(parseWordReference()));
        } else if (expectsLiteral()) {
            match = node(new EqualMatchBuilder(), builder -> builder.withValue(parseLiteral()));
        } else if (required) {
            throw unexpected(WORD);
        }
        return ofNullable(match);
    }

    @SuppressWarnings("unchecked")
    private DefinitionReference parseModule() {
        requireWord("module");
        currentModule = parseQualifiedName();
        requireTerminator();
        List<Import> imports = parseImports();
        return scoped(imports, () -> definition(
            ModuleDefinition.builder(),
            builder -> builder
                .withSymbol(currentModule)
                .withImports(imports)
                .withDefinitions(parseModuleDefinitions())
        ));
    }

    private List<DefinitionReference> parseModuleDefinitions() {
        List<DefinitionReference> definitions = new ArrayList<>();
        while (expectsDefinitions()) {
            definitions.addAll(parseDefinitions());
            requireTerminator();
        }
        return definitions;
    }

    private DataFieldDefinition parseNamedField() {
        return node(DataFieldDefinition.builder(), builder -> {
            builder.withName(requireWord());
            builder.withType(parseType());
        });
    }

    private List<DefinitionReference> parseOperatorDefinition() {
        Fixity fixity = parseOperatorFixity();
        int precedence = parseOperatorPrecedence();
        return parseSymbols().stream()
            .map(pair -> pair.into(
                (symbol, sourceRange) -> OperatorDefinition.builder()
                    .withSourceRange(sourceRange)
                    .withSymbol(symbol)
                    .withFixity(fixity)
                    .withPrecedence(precedence)
                    .build()))
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
            value = parseExpression();
            require(RPAREN);
        } else if (expects(LAMBDA)) {
            value = parseFunction();
        } else if (expects(LET)) {
            value = parseLet();
        } else if (expects(IF)) {
            value = parseConditional();
        } else if (required) {
            throw unexpected(ImmutableList.<TokenKind>builder().add(WORD, LPAREN).addAll(literals).build());
        }
        return ofNullable(value).map(this::parseInitializer_);
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

    private void parseRoot() {
        definition(RootDefinition.builder(), builder -> {
            if (!expects(EOF)) {
                builder.withModule(parseModule());
                while (expectsModule()) {
                    builder.withModule(parseModule());
                }
            }
            require(EOF);
        });
    }

    private Map<String, Type> parseSignatureConstraints() {
        if (expects(LPAREN)) {
            int offset = 0;
            while (!peekAt(offset).is(SEMICOLON)) {
                if (peekAt(offset).is(RPAREN) && peekAt(offset + 1).is(DOUBLE_ARROW)) {
                    return parseConstraints();
                } else {
                    offset++;
                }
            }
        }
        return emptyMap();
    }

    private Symbol parseSymbol() {
        if (expects(WORD)) {
            return qualify(requireMemberName());
        } else if (expects(LPAREN)) {
            nextToken();
            List<String> memberName = requireMemberName();
            require(RPAREN);
            return qualify(memberName);
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
        return parseType(emptyMap());
    }

    private Type parseType(Map<String, Type> constraints) {
        return splitQualified(parseQualifiedName()).into(
            (optionalModuleName, memberName) -> parseType_(constraints, optionalModuleName, memberName)
        );
    }

    private Type parseType_(Map<String, Type> constraints, Optional<String> optionalModuleName, String memberName) {
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
    }

    private DefinitionReference parseValueDefinition() {
        if (expectsAt(1, ASSIGN)) {
            return scoped(() -> definition(ValueDefinition.builder(), builder -> {
                Symbol symbol = qualify(requireMemberName());
                builder.withSymbol(symbol);
                memberNames.push(symbol.getMemberNames());
                require(ASSIGN);
                builder.withType(reserveType())
                    .withBody(parseExpression());
                memberNames.pop();
            }));
        } else {
            return scoped(() -> definition(UnshuffledDefinition.builder(), builder -> {
                builder.withMatches(parsePatternMatches());
                require(ASSIGN);
                builder.withSymbol(reserveSymbol())
                    .withBody(parseExpression());
            }));
        }
    }

    private Type parseValueSignature() {
        require(DOUBLE_COLON);
        return parseValueSignature_(parseSignatureConstraints());
    }

    private Type parseValueSignature_(Map<String, Type> constraints) {
        Type type = parseType(constraints);
        if (expects(ARROW)) {
            require(ARROW);
            type = fn(type, parseValueSignature_(constraints));
        }
        return type;
    }

    private List<DefinitionReference> parseValueSignatures() {
        List<Tuple2<Symbol, SourceRange>> symbolList = parseSymbols();
        Type type = parseValueSignature();
        return symbolList.stream()
            .map(pair -> pair.into(
                (symbol, sourceRange) -> ValueSignature.builder()
                    .withSymbol(symbol)
                    .withType(type)
                    .withSourceRange(sourceRange)
                    .build()))
            .map(this::collect)
            .collect(toList());
    }

    private Identifier parseWordReference() {
        return node(Identifier.builder(), builder -> builder
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

    private Symbol qualify(String memberName) {
        return qualified(currentModule, memberName);
    }

    private Symbol qualify(List<String> memberName) {
        return qualified(currentModule, memberName);
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

    private List<String> requireMemberName() {
        return ImmutableList.<String>builder()
            .addAll(memberNames.peek())
            .add(requireWord())
            .build();
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

    private Symbol reserveSymbol() {
        return scope().reserveSymbol(memberNames.peek());
    }

    private Type reserveType() {
        return scope().reserveType();
    }

    private Scope scope() {
        return scopes.peek();
    }

    private <T> T scoped(List<Import> imports, Supplier<T> supplier) {
        return scoped(Optional.of(imports), supplier);
    }

    private <T> T scoped(Supplier<T> supplier) {
        return scoped(Optional.empty(), supplier);
    }

    private <T> T scoped(Optional<List<Import>> optionalImports, Supplier<T> supplier) {
        scopes.push(optionalImports
            .map(imports -> scope().enterScope(currentModule, imports))
            .orElseGet(scope()::enterScope));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private ParseException unexpected(TokenKind wantedKind) {
        return new ParseException(
            "Unexpected " + scanner.peekAt(0).getKind()
                + "; wanted " + wantedKind + " " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object wantedValue) {
        return new ParseException(
            "Unexpected " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with value " + quote(wantedValue) + " " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(TokenKind wantedKind, Object... wantedValues) {
        return new ParseException(
            "Unexpected " + scanner.peekAt(0).getKind() + " with value " + quote(scanner.peekAt(0).getValue())
                + "; wanted " + wantedKind + " with one value of"
                + " [" + join(", ", stream(wantedValues).map(StringUtil::quote).collect(toList())) + "] " + scanner.getPosition().prettyPrint()
        );
    }

    private ParseException unexpected(List<TokenKind> wantedKinds) {
        return new ParseException(
            "Unexpected " + scanner.peekAt(0).getKind()
                + "; wanted one of [" + join(", ", wantedKinds.stream().map(Object::toString).collect(toList())) + "]"
                + " " + scanner.getPosition().prettyPrint()
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
                    if (token.is(EOF)) {
                        break;
                    }
                }
            }
        }
    }
}
