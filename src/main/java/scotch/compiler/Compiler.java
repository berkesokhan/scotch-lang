package scotch.compiler;

import java.util.List;
import scotch.compiler.generator.BytecodeGenerator;
import scotch.compiler.generator.GeneratedClass;
import scotch.compiler.parser.DependencyParser;
import scotch.compiler.parser.InputParser;
import scotch.compiler.parser.NameAccumulator;
import scotch.compiler.parser.NameQualifier;
import scotch.compiler.parser.OperatorParser;
import scotch.compiler.parser.PrecedenceParser;
import scotch.compiler.parser.TypeAnalyzer;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;

public class Compiler {

    public static Compiler compiler(SymbolResolver symbolResolver, String sourceName, String... lines) {
        return new Compiler(symbolResolver, Scanner.forString(sourceName, lines));
    }

    private final SymbolResolver symbolResolver;
    private final Scanner        scanner;

    private Compiler(SymbolResolver symbolResolver, Scanner scanner) {
        this.symbolResolver = symbolResolver;
        this.scanner = scanner;
    }

    public DefinitionGraph accumulateNames() {
        return new NameAccumulator(parsePrecedence()).parse();
    }

    public DefinitionGraph analyzeTypes() {
        return new TypeAnalyzer(parseDependencies()).analyze();
    }

    public List<GeneratedClass> generateBytecode() {
        return new BytecodeGenerator(analyzeTypes()).generate();
    }

    public DefinitionGraph parseDependencies() {
        return new DependencyParser(qualifyNames()).parse();
    }

    public DefinitionGraph parseInput() {
        return new InputParser(symbolResolver, scanner, new SyntaxBuilderFactory()).parse();
    }

    public DefinitionGraph parseOperators() {
        return new OperatorParser(parseInput()).parse();
    }

    public DefinitionGraph parsePrecedence() {
        return new PrecedenceParser(parseOperators(), new SyntaxBuilderFactory()).parse();
    }

    public DefinitionGraph qualifyNames() {
        return new NameQualifier(accumulateNames()).parse();
    }
}
