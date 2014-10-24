package scotch.compiler.util;

import static scotch.compiler.parser.Scanner.forString;

import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.parser.AstParser;
import scotch.compiler.parser.InputParser;
import scotch.compiler.parser.Scanner;
import scotch.compiler.parser.Token;

public class TestUtil {

    public static Value bodyOf(Definition definition) {
        return definition.accept(new DefinitionVisitor<Value>() {
            @Override
            public Value visit(ValueDefinition definition) {
                return definition.getBody();
            }
        });
    }

    public static SymbolTable parseAst(String... data) {
        return new AstParser(parseInput(data)).analyze();
    }

    public static SymbolTable parseInput(String... data) {
        return new InputParser(forString("test-source.sch", data)).parse();
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
