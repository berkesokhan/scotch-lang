package scotch.compiler.util;

import static scotch.compiler.parser.Scanner.forString;
import static scotch.compiler.syntax.SourceRange.NULL_SOURCE;

import scotch.compiler.analyzer.TypeAnalyzer;
import scotch.compiler.parser.InputParser;
import scotch.compiler.parser.Scanner;
import scotch.compiler.parser.SyntaxParser;
import scotch.compiler.parser.Token;
import scotch.compiler.parser.Token.TokenKind;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.SymbolResolver;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;

public class TestUtil {

    public static Token token(TokenKind kind, Object value) {
        return Token.token(kind, value, NULL_SOURCE);
    }

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

    public static SymbolTable parseSyntax(SymbolResolver resolver, String... data) {
        return new SyntaxParser(parseInput(data), resolver).analyze();
    }

    public static SymbolTable parseInput(String... data) {
        return new InputParser(forString("$test", data), new SyntaxBuilderFactory()).parse();
    }

    public static Token tokenAt(Scanner scanner, int offset) {
        Token token = null;
        for (int i = 0; i < offset; i++) {
            token = scanner.nextToken();
        }
        return token;
    }

    private TestUtil() {
        // intentionally empty
    }
}
