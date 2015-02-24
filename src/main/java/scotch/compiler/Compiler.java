package scotch.compiler;

import java.util.List;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.output.GeneratedClass;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
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
        return new NameAccumulator(parsePrecedence()).accumulateNames();
    }

    public DefinitionGraph checkTypes() {
        return new TypeChecker(accumulateDependencies()).checkTypes();
    }

    public List<GeneratedClass> generateBytecode() {
        return new BytecodeGenerator(checkTypes()).generateBytecode();
    }

    public DefinitionGraph accumulateDependencies() {
        return new DependencyAccumulator(qualifyNames()).accumulateDependencies();
    }

    public DefinitionGraph parseInput() {
        return new InputParser(symbolResolver, scanner).parse();
    }

    public DefinitionGraph accumulateOperators() {
        return new OperatorAccumulator(parseInput()).accumulateOperators();
    }

    public DefinitionGraph parsePrecedence() {
        return new PrecedenceParser(accumulateOperators()).parsePrecedence();
    }

    public DefinitionGraph qualifyNames() {
        return new ScopedNameQualifier(accumulateNames()).qualifyNames();
    }
}
