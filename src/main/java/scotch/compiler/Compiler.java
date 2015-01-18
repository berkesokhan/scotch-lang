package scotch.compiler;

import java.util.List;
import scotch.compiler.parser.InputParser;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;
import scotch.compiler.syntax.definition.Definition;
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
        NameAccumulator state = new NameAccumulatorState(parsePrecedence());
        state.accumulateNames();
        return state.getGraph();
    }

    public DefinitionGraph checkTypes() {
        TypeChecker state = new TypeCheckerState(accumulateDependencies());
        state.fromSorted(Definition::checkTypes);
        return state.getGraph();
    }

    public List<GeneratedClass> generateBytecode() {
        DefinitionGraph graph = checkTypes();
        if (graph.hasErrors()) {
            throw new CompileException(graph.getErrors());
        } else {
            BytecodeGenerator state = new BytecodeGeneratorState(graph);
            state.fromRoot();
            return state.getClasses();
        }
    }

    public DefinitionGraph accumulateDependencies() {
        DependencyAccumulator state = new DependencyAccumulatorState(qualifyNames());
        state.accumulateDependencies();
        DefinitionGraph graph = state.getGraph();
        return graph.sort();
    }

    public DefinitionGraph parseInput() {
        return new InputParser(symbolResolver, scanner, new SyntaxBuilderFactory()).parse();
    }

    public DefinitionGraph defineOperators() {
        OperatorDefinitionParser state = new OperatorDefinitionState(parseInput());
        state.defineOperators();
        return state.getGraph();
    }

    public DefinitionGraph parsePrecedence() {
        PrecedenceParser state = new PrecedenceParserState(defineOperators());
        state.parsePrecedence();
        return state.getGraph();
    }

    public DefinitionGraph qualifyNames() {
        NameQualifier state = new NameQualifierState(accumulateNames());
        state.qualifyNames();
        return state.getGraph();
    }
}
