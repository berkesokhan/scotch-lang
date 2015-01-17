package scotch.compiler;

import java.util.List;
import scotch.compiler.parser.InputParser;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.syntax.SyntaxTreeParser;
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
        SyntaxTreeParser state = new TreeParserState(parsePrecedence());
        state.fromRoot(Definition::accumulateNames);
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
            BytecodeGeneratorState state = new BytecodeGeneratorState(graph);
            state.fromRoot();
            return state.getClasses();
        }
    }

    public DefinitionGraph accumulateDependencies() {
        SyntaxTreeParser state = new TreeParserState(qualifyNames());
        state.fromRoot(Definition::accumulateDependencies);
        DefinitionGraph graph = state.getGraph();
        return graph.sort();
    }

    public DefinitionGraph parseInput() {
        return new InputParser(symbolResolver, scanner, new SyntaxBuilderFactory()).parse();
    }

    public DefinitionGraph defineOperators() {
        SyntaxTreeParser state = new TreeParserState(parseInput());
        state.fromRoot(Definition::defineOperators);
        return state.getGraph();
    }

    public DefinitionGraph parsePrecedence() {
        SyntaxTreeParser state = new TreeParserState(defineOperators());
        state.fromRoot((r, s) -> r.parsePrecedence(s).map(s::collect).get());
        return state.getGraph();
    }

    public DefinitionGraph qualifyNames() {
        SyntaxTreeParser state = new TreeParserState(accumulateNames());
        state.fromRoot(Definition::qualifyNames);
        return state.getGraph();
    }
}
