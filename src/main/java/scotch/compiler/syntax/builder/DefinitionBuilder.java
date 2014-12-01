package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.Definition.classDef;
import static scotch.compiler.syntax.Definition.module;
import static scotch.compiler.syntax.Definition.operatorDef;
import static scotch.compiler.syntax.Definition.root;
import static scotch.compiler.syntax.Definition.signature;
import static scotch.compiler.syntax.Definition.unshuffled;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.ClassDefinition;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.Operator.Fixity;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.SourceRange;
import scotch.compiler.syntax.Symbol;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;

public abstract class DefinitionBuilder<T extends Definition> implements SyntaxBuilder<T> {

    public static ClassDefinitionBuilder classBuilder() {
        return new ClassDefinitionBuilder();
    }

    public static ModuleDefinitionBuilder moduleBuilder() {
        return new ModuleDefinitionBuilder();
    }

    public static OperatorDefinitionBuilder operatorBuilder() {
        return new OperatorDefinitionBuilder();
    }

    public static UnshuffledPatternBuilder patternBuilder() {
        return new UnshuffledPatternBuilder();
    }

    public static RootDefinitionBuilder rootBuilder() {
        return new RootDefinitionBuilder();
    }

    public static ValueSignatureBuilder signatureBuilder() {
        return new ValueSignatureBuilder();
    }

    public static ValueDefinitionBuilder valueDefBuilder() {
        return new ValueDefinitionBuilder();
    }

    private DefinitionBuilder() {
        // intentionally empty
    }

    public static class ClassDefinitionBuilder extends DefinitionBuilder<ClassDefinition> {

        private Optional<Symbol>                    symbol      = Optional.empty();
        private Optional<List<Type>>                arguments   = Optional.empty();
        private Optional<List<DefinitionReference>> members     = Optional.empty();
        private Optional<SourceRange>               sourceRange = Optional.empty();

        private ClassDefinitionBuilder() {
            // intentionally empty
        }

        @Override
        public ClassDefinition build() {
            return classDef(
                require(sourceRange, "Source range"),
                require(symbol, "Class symbol"),
                require(arguments, "Class arguments"),
                require(members, "Class member definitions")
            );
        }

        public ClassDefinitionBuilder withArguments(List<Type> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public ClassDefinitionBuilder withMembers(List<DefinitionReference> members) {
            this.members = Optional.of(members);
            return this;
        }

        @Override
        public ClassDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ClassDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }

    public static class ModuleDefinitionBuilder extends DefinitionBuilder<ModuleDefinition> {

        private Optional<String>                    symbol      = Optional.empty();
        private Optional<List<Import>>              imports     = Optional.empty();
        private Optional<List<DefinitionReference>> definitions = Optional.empty();
        private Optional<SourceRange>               sourceRange = Optional.empty();

        private ModuleDefinitionBuilder() {
            // intentionally empty
        }

        @Override
        public ModuleDefinition build() {
            return module(
                require(sourceRange, "Source range"),
                require(symbol, "Module symbol"),
                require(imports, "Imports are required"),
                require(definitions, "Member definitions")
            );
        }

        public ModuleDefinitionBuilder withDefinitions(List<DefinitionReference> definitions) {
            this.definitions = Optional.of(definitions);
            return this;
        }

        public ModuleDefinitionBuilder withImports(List<Import> imports) {
            this.imports = Optional.of(imports);
            return this;
        }

        @Override
        public ModuleDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ModuleDefinitionBuilder withSymbol(String symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }

    public static class OperatorDefinitionBuilder extends DefinitionBuilder<OperatorDefinition> {

        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Fixity>      fixity      = Optional.empty();
        private OptionalInt           precedence  = OptionalInt.empty();
        private Optional<SourceRange> sourceRange = Optional.empty();

        private OperatorDefinitionBuilder() {
            // intentionally empty
        }

        @Override
        public OperatorDefinition build() {
            return operatorDef(
                require(sourceRange, "Source range"),
                require(symbol, "Operator symbol"),
                require(fixity, "Operator fixity"),
                require(precedence, "Operator precedence")
            );
        }

        public OperatorDefinitionBuilder withFixity(Fixity fixity) {
            this.fixity = Optional.of(fixity);
            return this;
        }

        public OperatorDefinitionBuilder withPrecedence(int precedence) {
            this.precedence = OptionalInt.of(precedence);
            return this;
        }

        @Override
        public OperatorDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public OperatorDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }

    public static class RootDefinitionBuilder extends DefinitionBuilder<RootDefinition> {

        private List<DefinitionReference> definitions = new ArrayList<>();
        private Optional<SourceRange>     sourceRange = Optional.empty();

        private RootDefinitionBuilder() {
            // intentionally empty
        }

        @Override
        public RootDefinition build() {
            return root(require(sourceRange, "Source range"), definitions);
        }

        public RootDefinitionBuilder withModule(DefinitionReference module) {
            definitions.add(module);
            return this;
        }

        @Override
        public RootDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }

    public static class UnshuffledPatternBuilder extends DefinitionBuilder<UnshuffledPattern> {

        private Optional<Symbol>             symbol      = Optional.empty();
        private Optional<List<PatternMatch>> matches     = Optional.empty();
        private Optional<Value>              body        = Optional.empty();
        private Optional<SourceRange>        sourceRange = Optional.empty();

        private UnshuffledPatternBuilder() {
            // intentionally empty
        }

        @Override
        public UnshuffledPattern build() {
            return unshuffled(
                require(sourceRange, "Source range"),
                require(symbol, "Unshuffled pattern symbol"),
                require(matches, "Unshuffled pattern matches"),
                require(body, "Unshuffled pattern body")
            );
        }

        public UnshuffledPatternBuilder withBody(Value body) {
            this.body = Optional.of(body);
            return this;
        }

        public UnshuffledPatternBuilder withMatches(List<PatternMatch> matches) {
            this.matches = Optional.of(matches);
            return this;
        }

        @Override
        public UnshuffledPatternBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public UnshuffledPatternBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }

    public static class ValueDefinitionBuilder extends DefinitionBuilder<ValueDefinition> {

        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();
        private Optional<Value>       body        = Optional.empty();
        private Optional<SourceRange> sourceRange = Optional.empty();

        private ValueDefinitionBuilder() {
            // intentionally empty
        }

        @Override
        public ValueDefinition build() {
            return value(
                require(sourceRange, "Source range"),
                require(symbol, "Value symbol"),
                require(type, "Value type"),
                require(body, "Value body")
            );
        }

        public ValueDefinitionBuilder withBody(Value body) {
            this.body = Optional.of(body);
            return this;
        }

        @Override
        public ValueDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ValueDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }

        public ValueDefinitionBuilder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }

    public static class ValueSignatureBuilder extends DefinitionBuilder<ValueSignature> {

        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();
        private Optional<SourceRange> sourceRange = Optional.empty();

        @Override
        public ValueSignature build() {
            return signature(
                require(sourceRange, "Source range"),
                require(symbol, "Signature symbol"),
                require(type, "Signature type")
            );
        }

        @Override
        public ValueSignatureBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ValueSignatureBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }

        public ValueSignatureBuilder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
