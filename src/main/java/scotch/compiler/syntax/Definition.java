package scotch.compiler.syntax;

import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.instanceRef;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.DefinitionReference.operatorRef;
import static scotch.compiler.syntax.DefinitionReference.patternRef;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.PatternMatcher.pattern;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.DefinitionReference.ClassReference;
import scotch.compiler.syntax.DefinitionReference.ModuleReference;
import scotch.compiler.text.SourceRange;

public abstract class Definition {

    public static ClassDefinition classDef(SourceRange sourceRange, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        return new ClassDefinition(sourceRange, symbol, arguments, members);
    }

    public static ModuleDefinition module(SourceRange sourceRange, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceRange, symbol, imports, definitions);
    }

    public static OperatorDefinition operatorDef(SourceRange sourceRange, Symbol symbol, Fixity fixity, int precedence) {
        return new OperatorDefinition(sourceRange, symbol, fixity, precedence);
    }

    public static RootDefinition root(SourceRange sourceRange, List<DefinitionReference> definitions) {
        return new RootDefinition(sourceRange, definitions);
    }

    public static ScopeDefinition scopeDef(SourceRange sourceRange, Symbol symbol) {
        return new ScopeDefinition(sourceRange, symbol);
    }

    public static ValueSignature signature(SourceRange sourceRange, Symbol symbol, Type type) {
        return new ValueSignature(sourceRange, symbol, type);
    }

    public static UnshuffledPattern unshuffled(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new UnshuffledPattern(sourceRange, symbol, matches, body);
    }

    public static ValueDefinition value(SourceRange sourceRange, Symbol symbol, Type type, Value value) {
        return new ValueDefinition(sourceRange, symbol, value, type);
    }

    private Definition() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract DefinitionReference getReference();

    public abstract SourceRange getSourceRange();

    @Override
    public abstract int hashCode();

    public void markLine(CodeBlock codeBlock) {
        getSourceRange().markLine(codeBlock);
    }

    @Override
    public abstract String toString();

    public interface DefinitionVisitor<T> {

        default T visit(ClassDefinition definition) {
            return visitOtherwise(definition);
        }

        default T visit(InstanceDefinition definition) {
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

        default T visit(ScopeDefinition definition) {
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

        default T visitOtherwise(Definition definition) {
            throw new UnsupportedOperationException("Can't visit " + definition.getClass().getSimpleName());
        }
    }

    public static class ClassDefinition extends Definition {

        private final SourceRange               sourceRange;
        private final Symbol                    symbol;
        private final List<Type>                arguments;
        private final List<DefinitionReference> members;

        private ClassDefinition(SourceRange sourceRange, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException("Can't create class definition with 0 arguments");
            } else if (members.isEmpty()) {
                throw new IllegalArgumentException("Can't create class definition with 0 members");
            }
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

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, arguments, members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }

    public static class InstanceDefinition extends Definition {

        private final SourceRange      sourceRange;
        private final ClassReference   classReference;
        private final ModuleReference  moduleReference;
        private final List<Type>       types;
        private final List<Definition> members;

        private InstanceDefinition(
            SourceRange sourceRange,
            ClassReference classReference,
            ModuleReference moduleReference,
            List<Type> types,
            List<Definition> members
        ) {
            this.sourceRange = sourceRange;
            this.classReference = classReference;
            this.moduleReference = moduleReference;
            this.types = types;
            this.members = members;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof InstanceDefinition) {
                InstanceDefinition other = (InstanceDefinition) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(classReference, other.classReference)
                    && Objects.equals(moduleReference, other.moduleReference)
                    && Objects.equals(types, other.types)
                    && Objects.equals(members, other.members);
            } else {
                return false;
            }
        }

        @Override
        public DefinitionReference getReference() {
            return instanceRef(classReference, moduleReference, types);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceRange, classReference, moduleReference, types, members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(classReference=" + classReference + ", moduleReference=" + moduleReference + ", types=" + types + ")";
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

        public String getClassName() {
            return fromString(symbol + ".ScotchModule").getClassName();
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
    }

    public static class ScopeDefinition extends Definition {

        private final SourceRange sourceRange;
        private final Symbol symbol;

        private ScopeDefinition(SourceRange sourceRange, Symbol symbol) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ScopeDefinition) {
                ScopeDefinition other = (ScopeDefinition) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(symbol, other.symbol);
            } else {
                return false;
            }
        }

        @Override
        public DefinitionReference getReference() {
            return scopeRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol.getCanonicalName() + ")";
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

        public PatternMatcher asPatternMatcher(List<PatternMatch> matches) {
            return pattern(sourceRange, symbol, matches, body);
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

        public List<PatternMatch> getMatches() {
            return matches;
        }

        @Override
        public DefinitionReference getReference() {
            return patternRef(symbol);
        }

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

        public String getMethodName() {
            return symbol.unqualify().getMethodName();
        }

        @Override
        public DefinitionReference getReference() {
            return valueRef(symbol);
        }

        public String getSignature() {
            return type.getSignature();
        }

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
