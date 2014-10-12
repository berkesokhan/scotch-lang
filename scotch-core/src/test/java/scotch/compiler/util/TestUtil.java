package scotch.compiler.util;

import static scotch.compiler.parser.Scanner.forString;

import scotch.compiler.analyzer.NameQualifier;
import scotch.compiler.analyzer.SymbolResolver;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.parser.Parser;
import scotch.compiler.parser.Scanner;
import scotch.compiler.parser.Token;

public class TestUtil {

    public static SymbolTable analyzeReferences(SymbolResolver resolver, String... data) {
        return new NameQualifier(resolver, parse(data)).analyze();
    }

    public static Value bodyOf(Definition definition) {
        return definition.accept(new DefinitionVisitor<Value>() {
            @Override
            public Value visit(ValueDefinition definition) {
                return definition.getBody();
            }
        });
    }

    public static SymbolTable parse(String... data) {
        return new Parser(forString("test-source.sch", data)).parse();
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
