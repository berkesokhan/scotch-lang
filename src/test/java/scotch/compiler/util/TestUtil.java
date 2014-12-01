package scotch.compiler.util;

import static java.util.Arrays.asList;
import static scotch.compiler.parser.Scanner.forString;
import static scotch.compiler.syntax.SourceRange.NULL_SOURCE;
import static scotch.compiler.syntax.Symbol.fromString;

import java.util.List;
import scotch.compiler.analyzer.TypeAnalyzer;
import scotch.compiler.parser.InputParser;
import scotch.compiler.parser.Scanner;
import scotch.compiler.parser.SyntaxParser;
import scotch.compiler.parser.Token;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.ClassDefinition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.Import.ModuleImport;
import scotch.compiler.syntax.Operator.Fixity;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.SymbolResolver;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.LiteralValue;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;

public class TestUtil {

    public static SymbolTable analyzeTypes(SymbolResolver resolver, String... data) {
        return new TypeAnalyzer(parseSyntax(resolver, data)).analyze();
    }

    public static Value bodyOf(Definition definition) {
        return definition.accept(new DefinitionVisitor<Value>() {
            @Override
            public Value visit(ValueDefinition definition) {
                return definition.getBody();
            }

            @Override
            public Value visitOtherwise(Definition definition) {
                throw new UnsupportedOperationException("Can't get body from " + definition.getClass().getSimpleName());
            }
        });
    }

    public static CaptureMatch capture(String name, Type type) {
        return PatternMatch.capture(NULL_SOURCE, fromString(name), type);
    }

    public static ClassDefinition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return Definition.classDef(NULL_SOURCE, fromString(name), arguments, members);
    }

    public static EqualMatch equal(Value value) {
        return PatternMatch.equal(NULL_SOURCE, value);
    }

    public static Identifier id(String name, Type type) {
        return Value.id(NULL_SOURCE, fromString(name), type);
    }

    public static LiteralValue literal(Object value, Type type) {
        return Value.literal(NULL_SOURCE, value, type);
    }

    public static Message message(Value... members) {
        return Value.message(NULL_SOURCE, asList(members));
    }

    public static ModuleImport moduleImport(String moduleName) {
        return Import.moduleImport(NULL_SOURCE, moduleName);
    }

    public static OperatorDefinition operatorDef(String name, Fixity fixity, int precedence) {
        return Definition.operatorDef(NULL_SOURCE, fromString(name), fixity, precedence);
    }

    public static SymbolTable parseInput(String... data) {
        return new InputParser(forString("$test", data), new SyntaxBuilderFactory()).parse();
    }

    public static SymbolTable parseSyntax(SymbolResolver resolver, String... data) {
        return new SyntaxParser(parseInput(data), resolver).analyze();
    }

    public static PatternMatcher pattern(String name, List<PatternMatch> matches, Value body) {
        return PatternMatcher.pattern(NULL_SOURCE, fromString(name), matches, body);
    }

    public static PatternMatchers patterns(Type type, PatternMatcher... matchers) {
        return Value.patterns(NULL_SOURCE, type, asList(matchers));
    }

    public static RootDefinition root(List<DefinitionReference> definitions) {
        return Definition.root(NULL_SOURCE, definitions);
    }

    public static ValueSignature signature(String name, Type type) {
        return Definition.signature(NULL_SOURCE, fromString(name), type);
    }

    public static Token token(TokenKind kind, Object value) {
        return Token.token(kind, value, NULL_SOURCE);
    }

    public static Token tokenAt(Scanner scanner, int offset) {
        Token token = null;
        for (int i = 0; i < offset; i++) {
            token = scanner.nextToken();
        }
        return token;
    }

    public static UnshuffledPattern unshuffled(String name, List<PatternMatch> matches, Value body) {
        return Definition.unshuffled(NULL_SOURCE, fromString(name), matches, body);
    }

    public static ValueDefinition value(String name, Type type, Value value) {
        return Definition.value(NULL_SOURCE, fromString(name), type, value);
    }

    private TestUtil() {
        // intentionally empty
    }
}
