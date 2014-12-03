package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.DefinitionGraph.createGraph;

import java.util.List;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph.DefinitionGraphBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ClassDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ModuleDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.OperatorDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.RootDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.UnshuffledPatternBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ValueDefinitionBuilder;
import scotch.compiler.syntax.builder.DefinitionBuilder.ValueSignatureBuilder;
import scotch.compiler.syntax.builder.ImportBuilder.ModuleImportBuilder;
import scotch.compiler.syntax.builder.PatternMatchBuilder.CaptureMatchBuilder;
import scotch.compiler.syntax.builder.PatternMatchBuilder.EqualMatchBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.IdentifierBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.LiteralBuilder;
import scotch.compiler.syntax.builder.ValueBuilder.MessageBuilder;

public class SyntaxBuilderFactory {

    public CaptureMatchBuilder captureMatchBuilder() {
        return PatternMatchBuilder.captureMatchBuilder();
    }

    public ClassDefinitionBuilder classBuilder() {
        return DefinitionBuilder.classBuilder();
    }

    public EqualMatchBuilder equalMatchBuilder() {
        return PatternMatchBuilder.equalMatchBuilder();
    }

    public IdentifierBuilder idBuilder() {
        return ValueBuilder.idBuilder();
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

    public RootDefinitionBuilder rootBuilder() {
        return DefinitionBuilder.rootBuilder();
    }

    public ValueSignatureBuilder signatureBuilder() {
        return DefinitionBuilder.signatureBuilder();
    }

    public DefinitionGraphBuilder definitionGraphBuilder(List<DefinitionEntry> definitions) {
        return createGraph(definitions);
    }

    public UnshuffledPatternBuilder unshuffledBuilder() {
        return DefinitionBuilder.patternBuilder();
    }

    public ValueDefinitionBuilder valueDefBuilder() {
        return DefinitionBuilder.valueDefBuilder();
    }
}
