package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.Definition.classDef;
import static scotch.compiler.syntax.Definition.module;
import static scotch.compiler.syntax.Definition.operatorDef;
import static scotch.compiler.syntax.Definition.root;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.Definition.signature;
import static scotch.compiler.syntax.Definition.unshuffled;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.ClassDefinition;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ScopeDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.InstanceMap;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;

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

    public static ScopeDefinitionBuilder scopeBuilder() {
        return new ScopeDefinitionBuilder();
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

        private Optional<Symbol>                    symbol;
        private Optional<List<Type>>                arguments;
        private Optional<List<DefinitionReference>> members;
        private Optional<SourceRange>               sourceRange;

        private ClassDefinitionBuilder() {
            symbol = Optional.empty();
            arguments = Optional.empty();
            members = Optional.empty();
            sourceRange = Optional.empty();
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

        private Optional<String>                    symbol;
        private Optional<List<Import>>              imports;
        private Optional<List<DefinitionReference>> definitions;
        private Optional<SourceRange>               sourceRange;

        private ModuleDefinitionBuilder() {
            symbol = Optional.empty();
            imports = Optional.empty();
            definitions = Optional.empty();
            sourceRange = Optional.empty();
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

        private Optional<Symbol>      symbol;
        private Optional<Fixity>      fixity;
        private OptionalInt           precedence;
        private Optional<SourceRange> sourceRange;

        private OperatorDefinitionBuilder() {
            symbol = Optional.empty();
            fixity = Optional.empty();
            precedence = OptionalInt.empty();
            sourceRange = Optional.empty();
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

        private List<DefinitionReference> definitions;
        private Optional<SourceRange>     sourceRange;

        private RootDefinitionBuilder() {
            definitions = new ArrayList<>();
            sourceRange = Optional.empty();
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

    public static class ScopeDefinitionBuilder extends DefinitionBuilder<ScopeDefinition> {

        private Optional<Symbol>      symbol;
        private Optional<SourceRange> sourceRange;

        private ScopeDefinitionBuilder() {
            symbol = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public ScopeDefinition build() {
            return scopeDef(
                require(sourceRange, "Source range"),
                require(symbol, "Scope symbol")
            );
        }

        @Override
        public ScopeDefinitionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ScopeDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }

    public static class UnshuffledPatternBuilder extends DefinitionBuilder<UnshuffledPattern> {

        private Optional<Symbol>             symbol;
        private Optional<List<PatternMatch>> matches;
        private Optional<Value>              body;
        private Optional<SourceRange>        sourceRange;

        private UnshuffledPatternBuilder() {
            symbol = Optional.empty();
            matches = Optional.empty();
            body = Optional.empty();
            sourceRange = Optional.empty();
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

        private Optional<Symbol>      symbol;
        private Optional<Type>        type;
        private Optional<Value>       body;
        private Optional<SourceRange> sourceRange;

        private ValueDefinitionBuilder() {
            symbol = Optional.empty();
            type = Optional.empty();
            body = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public ValueDefinition build() {
            return value(
                require(sourceRange, "Source range"),
                require(symbol, "Value symbol"),
                require(type, "Value type"),
                require(body, "Value body").accept(new ValueVisitor<Value>() {
                    @Override
                    public Value visit(Message message) {
                        return message.collapse();
                    }

                    @Override
                    public Value visitOtherwise(Value value) {
                        return value;
                    }
                }),
                InstanceMap.empty()
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

        private Optional<Symbol>      symbol;
        private Optional<Type>        type;
        private Optional<SourceRange> sourceRange;

        private ValueSignatureBuilder() {
            type = Optional.empty();
            symbol = Optional.empty();
            sourceRange = Optional.empty();
        }

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
