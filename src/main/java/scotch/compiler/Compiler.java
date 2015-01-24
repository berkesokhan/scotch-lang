package scotch.compiler;

import java.util.List;
import scotch.compiler.parser.InputParser;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

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
        return new NameAccumulatorState(parsePrecedence()).accumulateNames();
    }

    public DefinitionGraph checkTypes() {
        return new TypeCheckerState(accumulateDependencies()).checkTypes();
    }

    public List<GeneratedClass> generateBytecode() {
        return new BytecodeGeneratorState(checkTypes()).generateBytecode();
    }

    public DefinitionGraph accumulateDependencies() {
        return new DependencyAccumulatorState(qualifyNames()).accumulateDependencies();
    }

    public DefinitionGraph parseInput() {
        return new InputParser(symbolResolver, scanner).parse();
    }

    public DefinitionGraph defineOperators() {
        return new OperatorDefinitionState(parseInput()).defineOperators();
    }

    public DefinitionGraph parsePrecedence() {
        return new PrecedenceParserState(defineOperators()).parsePrecedence();
    }

    public DefinitionGraph qualifyNames() {
        return new NameQualifierState(accumulateNames()).qualifyNames();
    }
}
