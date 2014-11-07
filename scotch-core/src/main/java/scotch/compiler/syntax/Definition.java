package scotch.compiler.syntax;

import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.DefinitionReference.operatorRef;
import static scotch.compiler.syntax.DefinitionReference.patternRef;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.Operator.operator;
import static scotch.compiler.syntax.SourceRange.NULL_SOURCE;
import static scotch.compiler.syntax.Symbol.fromString;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.syntax.Operator.Fixity;

public abstract class Definition implements SourceAware<Definition> {

    public static Definition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return classDef(fromString(name), arguments, members);
    }

    public static Definition classDef(Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        return new ClassDefinition(NULL_SOURCE, symbol, arguments, members);
    }

    public static Definition module(String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        return new ModuleDefinition(NULL_SOURCE, symbol, imports, definitions);
    }

    public static Definition operatorDef(String name, Fixity fixity, int precedence) {
        return operatorDef(fromString(name), fixity, precedence);
    }

    public static Definition operatorDef(Symbol symbol, Fixity fixity, int precedence) {
        return new OperatorDefinition(NULL_SOURCE, symbol, fixity, precedence);
    }

    public static Definition root(List<DefinitionReference> definitions) {
        return new RootDefinition(NULL_SOURCE, definitions);
    }

    public static Definition signature(String name, Type type) {
        return signature(fromString(name), type);
    }

    public static Definition signature(Symbol symbol, Type type) {
        return new ValueSignature(NULL_SOURCE, symbol, type);
    }

    public static UnshuffledPattern unshuffled(String name, List<PatternMatch> matches, Value body) {
        return unshuffled(fromString(name), matches, body);
    }

    public static UnshuffledPattern unshuffled(Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledPattern(NULL_SOURCE, symbol, matches, body);
    }

    public static ValueDefinition value(String name, Type type, Value value) {
        return value(fromString(name), type, value);
    }

    public static ValueDefinition value(Symbol symbol, Type type, Value value) {
        return new ValueDefinition(NULL_SOURCE, symbol, value, type);
    }

    private Definition() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract DefinitionReference getReference();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public interface DefinitionVisitor<T> {

        default T visit(ClassDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(ModuleDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(OperatorDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(RootDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(UnshuffledPattern pattern) {
            return visitOtherwise(pattern);
        }

        default T visit(ValueDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(ValueSignature signature) {
            return visitOtherwise(signature);
        }

        T visitOtherwise(Definition definition);
    }

    public static class ClassDefinition extends Definition {

        private final SourceRange               sourceRange;
        private final Symbol                    symbol;
        private final List<Type>                arguments;
        private final List<DefinitionReference> members;

        private ClassDefinition(SourceRange sourceRange, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.arguments = ImmutableList.copyOf(arguments);
            this.members = ImmutableList.copyOf(members);
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ClassDefinition) {
                ClassDefinition other = (ClassDefinition) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(arguments, other.arguments)
                    && Objects.equals(members, other.members);
            } else {
                return false;
            }
        }

        @Override
        public DefinitionReference getReference() {
            return classRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, arguments, members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        @Override
        public ClassDefinition withSourceRange(SourceRange sourceRange) {
            return new ClassDefinition(sourceRange, symbol, arguments, members);
        }
    }

    public static class ModuleDefinition extends Definition {

        private final SourceRange               sourceRange;
        private final String                    symbol;
        private final List<Import>              imports;
        private final List<DefinitionReference> definitions;

        private ModuleDefinition(SourceRange sourceRange, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.imports = ImmutableList.copyOf(imports);
            this.definitions = ImmutableList.copyOf(definitions);
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ModuleDefinition) {
                ModuleDefinition other = (ModuleDefinition) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(imports, other.imports)
                    && Objects.equals(definitions, other.definitions);
            } else {
                return false;
            }
        }

        public List<DefinitionReference> getDefinitions() {
            return definitions;
        }

        public List<Import> getImports() {
            return imports;
        }

        @Override
        public DefinitionReference getReference() {
            return moduleRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, imports, definitions);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        public ModuleDefinition withDefinitions(List<DefinitionReference> definitions) {
            return new ModuleDefinition(sourceRange, symbol, imports, definitions);
        }

        @Override
        public ModuleDefinition withSourceRange(SourceRange sourceRange) {
            return new ModuleDefinition(sourceRange, symbol, imports, definitions);
        }
    }

    public static class OperatorDefinition extends Definition {

        private final SourceRange sourceRange;
        private final Symbol      symbol;
        private final Fixity      fixity;
        private final int         precedence;

        private OperatorDefinition(SourceRange sourceRange, Symbol symbol, Fixity fixity, int precedence) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.fixity = fixity;
            this.precedence = precedence;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof OperatorDefinition) {
                OperatorDefinition other = (OperatorDefinition) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(fixity, other.fixity)
                    && Objects.equals(precedence, other.precedence);
            } else {
                return false;
            }
        }

        public Operator getOperator() {
            return operator(fixity, precedence);
        }

        @Override
        public DefinitionReference getReference() {
            return operatorRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, fixity, precedence);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + " :: " + fixity + ", " + precedence + ")";
        }

        @Override
        public OperatorDefinition withSourceRange(SourceRange sourceRange) {
            return new OperatorDefinition(sourceRange, symbol, fixity, precedence);
        }
    }

    public static class RootDefinition extends Definition {

        private final SourceRange               sourceRange;
        private final List<DefinitionReference> definitions;

        private RootDefinition(SourceRange sourceRange, List<DefinitionReference> definitions) {
            this.sourceRange = sourceRange;
            this.definitions = ImmutableList.copyOf(definitions);
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof RootDefinition && Objects.equals(definitions, ((RootDefinition) o).definitions);
        }

        public List<DefinitionReference> getDefinitions() {
            return definitions;
        }

        @Override
        public DefinitionReference getReference() {
            return rootRef();
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(definitions);
        }

        @Override
        public String toString() {
            return stringify(this);
        }

        public RootDefinition withDefinitions(List<DefinitionReference> definitions) {
            return new RootDefinition(sourceRange, definitions);
        }

        @Override
        public RootDefinition withSourceRange(SourceRange sourceRange) {
            return new RootDefinition(sourceRange, definitions);
        }
    }

    public static class UnshuffledPattern extends Definition {

        private final SourceRange        sourceRange;
        private final Symbol             symbol;
        private final List<PatternMatch> matches;
        private final Value              body;

        private UnshuffledPattern(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.matches = ImmutableList.copyOf(matches);
            this.body = body;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnshuffledPattern) {
                UnshuffledPattern other = (UnshuffledPattern) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(matches, other.matches)
                    && Objects.equals(body, other.body);
            } else {
                return false;
            }
        }

        public Value getBody() {
            return body;
        }

        public List<PatternMatch> getMatches() {
            return matches;
        }

        @Override
        public DefinitionReference getReference() {
            return patternRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, body, matches);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        @Override
        public UnshuffledPattern withSourceRange(SourceRange sourceRange) {
            return new UnshuffledPattern(sourceRange, symbol, matches, body);
        }
    }

    public static class ValueDefinition extends Definition {

        private final SourceRange sourceRange;
        private final Symbol      symbol;
        private final Value       body;
        private final Type        type;

        private ValueDefinition(SourceRange sourceRange, Symbol symbol, Value body, Type type) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.body = body;
            this.type = type;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ValueDefinition) {
                ValueDefinition other = (ValueDefinition) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(body, other.body)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public Value getBody() {
            return body;
        }

        @Override
        public DefinitionReference getReference() {
            return valueRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, body, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + " :: " + type + ")";
        }

        public ValueDefinition withBody(Value body) {
            return new ValueDefinition(sourceRange, symbol, body, type);
        }

        @Override
        public ValueDefinition withSourceRange(SourceRange sourceRange) {
            return new ValueDefinition(sourceRange, symbol, body, type);
        }

        public ValueDefinition withType(Type type) {
            return new ValueDefinition(sourceRange, symbol, body, type);
        }
    }

    public static class ValueSignature extends Definition {

        private final SourceRange sourceRange;
        private final Symbol      symbol;
        private final Type        type;

        private ValueSignature(SourceRange sourceRange, Symbol symbol, Type type) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.type = type;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ValueSignature) {
                ValueSignature other = (ValueSignature) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        @Override
        public DefinitionReference getReference() {
            return signatureRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public ValueSignature withSourceRange(SourceRange sourceRange) {
            return new ValueSignature(sourceRange, symbol, type);
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + " :: " + type + ")";
        }
    }
}
