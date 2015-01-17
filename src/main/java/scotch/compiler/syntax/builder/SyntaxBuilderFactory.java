package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.definition.DefinitionGraph.createGraph;

import java.util.List;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph.DefinitionGraphBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ClassDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ModuleDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.OperatorDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.RootDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ScopeDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.UnshuffledPatternBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ValueDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ValueSignatureBuilder;
import scotch.compiler.syntax.builder.ImportBuilder.ModuleImportBuilder;
import scotch.compiler.syntax.builder.PatternMatchBuilder.CaptureMatchBuilder;
import scotch.compiler.syntax.builder.PatternMatchBuilder.EqualMatchBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.ArgumentBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.FunctionBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.IdentifierBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.LetBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.LiteralBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.MessageBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.PatternsBuilder;

public class SyntaxBuilderFactory {

    public ArgumentBuilder argumentBuilder() {
        return ValueBuilder.argumentBuilder();
    }

    public CaptureMatchBuilder captureMatchBuilder() {
        return PatternMatchBuilder.captureMatchBuilder();
    }

    public ClassDefinitionBuilder classBuilder() {
        return DefinitionBuilder.classBuilder();
    }

    public DefinitionGraphBuilder definitionGraphBuilder(List<DefinitionEntry> definitions) {
        return createGraph(definitions);
    }

    public EqualMatchBuilder equalMatchBuilder() {
        return PatternMatchBuilder.equalMatchBuilder();
    }

    public FunctionBuilder functionBuilder() {
        return ValueBuilder.functionBuilder();
    }

    public IdentifierBuilder idBuilder() {
        return ValueBuilder.idBuilder();
    }

    public LetBuilder letBuilder() {
        return ValueBuilder.letBuilder();
    }

    public LiteralBuilder literalBuilder() {
        return ValueBuilder.literalBuilder();
    }

    public MessageBuilder messageBuilder() {
        return ValueBuilder.messageBuilder();
    }

    public ModuleDefinitionBuilder moduleBuilder() {
        return DefinitionBuilder.moduleBuilder();
    }

    public ModuleImportBuilder moduleImportBuilder() {
        return ImportBuilder.moduleImportBuilder();
    }

    public OperatorDefinitionBuilder operatorBuilder() {
        return DefinitionBuilder.operatorBuilder();
    }

    public PatternsBuilder patternsBuilder() {
        return ValueBuilder.patternsBuilder();
    }

    public RootDefinitionBuilder rootBuilder() {
        return DefinitionBuilder.rootBuilder();
    }

    public ScopeDefinitionBuilder scopeBuilder() {
        return DefinitionBuilder.scopeBuilder();
    }

    public ValueSignatureBuilder signatureBuilder() {
        return DefinitionBuilder.signatureBuilder();
    }

    public UnshuffledPatternBuilder unshuffledBuilder() {
        return DefinitionBuilder.patternBuilder();
    }

    public ValueDefinitionBuilder valueDefBuilder() {
        return DefinitionBuilder.valueDefBuilder();
    }
}
