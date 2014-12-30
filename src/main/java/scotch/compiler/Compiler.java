package scotch.compiler;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import scotch.compiler.generator.BytecodeGenerator;
import scotch.compiler.generator.GeneratedClass;
import scotch.compiler.parser.InputParser;
import scotch.compiler.parser.MethodBinder;
import scotch.compiler.parser.SyntaxParser;
import scotch.compiler.parser.TypeAnalyzer;
import scotch.compiler.scanner.Scanner;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;

public class Compiler {

    public static Compiler compiler() {
        return new Compiler();
    }

    public static Compiler compiler(SymbolResolver symbolResolver, String source, String... data) {
        SyntaxBuilderFactory builderFactory = new SyntaxBuilderFactory();
        return compiler()
            .withScanner(Scanner.forString(source, data))
            .withInputParser(scanner -> new InputParser(scanner, builderFactory))
            .withSyntaxParser(graph -> new SyntaxParser(graph, symbolResolver, builderFactory))
            .withTypeAnalyzer(TypeAnalyzer::new)
            .withMethodBinder(MethodBinder::new)
            .withBytecodeGenerator(BytecodeGenerator::new);
    }

    private Optional<Scanner>                                      scanner              = Optional.empty();
    private Optional<Function<DefinitionGraph, SyntaxParser>>      syntaxParser         = Optional.empty();
    private Optional<Function<DefinitionGraph, TypeAnalyzer>>      typeAnalyzer         = Optional.empty();
    private Optional<Function<DefinitionGraph, BytecodeGenerator>> bytecodeGenerator    = Optional.empty();
    private Optional<Function<Scanner, InputParser>>               inputParser          = Optional.empty();
    private Optional<Function<DefinitionGraph, MethodBinder>>      methodBinder         = Optional.empty();

    private Compiler() {
        // intentionally empty
    }

    public DefinitionGraph analyzeTypes() {
        return typeAnalyzer
            .map(fn -> fn.apply(parseSyntax()).analyze())
            .orElseThrow(() -> new IllegalStateException("No type analyzer given"));
    }

    public DefinitionGraph bindMethods() {
        return methodBinder
            .map(fn -> fn.apply(analyzeTypes()).bindMethods())
            .orElseThrow(() -> new IllegalStateException("No method binder given"));
    }

    public List<GeneratedClass> generateBytecode() {
        return bytecodeGenerator
            .map(fn -> fn.apply(bindMethods()).generate())
            .orElseThrow(() -> new IllegalStateException("No bytecode generator given"));
    }

    public DefinitionGraph parseInput() {
        return inputParser
            .map(fn -> fn.apply(scanner.orElseThrow(() -> new IllegalStateException("No scanner given"))).parse())
            .orElseThrow(() -> new IllegalStateException("No input parser given"));
    }

    public DefinitionGraph parseSyntax() {
        return syntaxParser
            .map(fn -> fn.apply(parseInput()).parse())
            .orElseThrow(() -> new IllegalStateException("No syntax parser given"));
    }

    public Compiler withBytecodeGenerator(Function<DefinitionGraph, BytecodeGenerator> bytecodeGenerator) {
        this.bytecodeGenerator = Optional.of(bytecodeGenerator);
        return this;
    }

    public Compiler withInputParser(Function<Scanner, InputParser> inputParser) {
        this.inputParser = Optional.of(inputParser);
        return this;
    }

    public Compiler withMethodBinder(Function<DefinitionGraph, MethodBinder> methodBinder) {
        this.methodBinder = Optional.of(methodBinder);
        return this;
    }

    public Compiler withScanner(Scanner scanner) {
        this.scanner = Optional.of(scanner);
        return this;
    }

    public Compiler withSyntaxParser(Function<DefinitionGraph, SyntaxParser> syntaxParser) {
        this.syntaxParser = Optional.of(syntaxParser);
        return this;
    }

    public Compiler withTypeAnalyzer(Function<DefinitionGraph, TypeAnalyzer> typeAnalyzer) {
        this.typeAnalyzer = Optional.of(typeAnalyzer);
        return this;
    }
}
