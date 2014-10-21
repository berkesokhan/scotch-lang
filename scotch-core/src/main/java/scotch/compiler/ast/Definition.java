package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ast.Operator.Fixity;
import scotch.lang.Type;

public abstract class Definition {

    public static Definition classDef(String name, List<Type> arguments, List<DefinitionReference> members) {
        return new ClassDefinition(name, arguments, members);
    }

    public static Definition module(String name, List<Import> imports, List<DefinitionReference> definitions) {
        return new ModuleDefinition(name, imports, definitions);
    }

    public static Definition operatorDef(String name, Fixity fixity, int precedence) {
        return new OperatorDefinition(name, fixity, precedence);
    }

    public static Definition root(List<DefinitionReference> definitions) {
        return new RootDefinition(definitions);
    }

    public static Definition signature(String name, Type type) {
        return new ValueSignature(name, type);
    }

    public static Definition unshuffled(PatternMatcher pattern) {
        return new UnshuffledPattern(pattern);
    }

    public static Definition value(String name, Type type, Value value) {
        return new ValueDefinition(name, value, type);
    }

    private Definition() {
        // intentionally empty
    }

    public abstract <T> T accept(DefinitionVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

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

        default T visitOtherwise(Definition definition) {
            throw new UnsupportedOperationException("Can't visit " + definition);
        }
    }

    public static class ClassDefinition extends Definition {

        private final String                    name;
        private final List<Type>                arguments;
        private final List<DefinitionReference> members;

        private ClassDefinition(String name, List<Type> arguments, List<DefinitionReference> members) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(arguments, other.arguments)
                    && Objects.equals(members, other.members);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments, members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
        }
    }

    public static class ModuleDefinition extends Definition {

        private final String                    name;
        private final List<Import>              imports;
        private final List<DefinitionReference> definitions;

        private ModuleDefinition(String name, List<Import> imports, List<DefinitionReference> definitions) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(imports, other.imports)
                    && Objects.equals(definitions, other.definitions);
            } else {
                return false;
            }
        }

        public List<Import> getImports() {
            return imports;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, imports, definitions);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
        }
    }

    public static class OperatorDefinition extends Definition {

        private final String name;
        private final Fixity fixity;
        private final int    precedence;

        private OperatorDefinition(String name, Fixity fixity, int precedence) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(fixity, other.fixity)
                    && Objects.equals(precedence, other.precedence);
            } else {
                return false;
            }
        }

        public Fixity getFixity() {
            return fixity;
        }

        public String getName() {
            return name;
        }

        public int getPrecedence() {
            return precedence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, fixity, precedence);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + " :: " + fixity + ", " + precedence + ")";
        }
    }

    public static class RootDefinition extends Definition {

        private final List<DefinitionReference> definitions;

        private RootDefinition(List<DefinitionReference> definitions) {
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

        @Override
        public int hashCode() {
            return Objects.hash(definitions);
        }

        @Override
        public String toString() {
            return stringify(this);
        }
    }

    public static class UnshuffledPattern extends Definition {

        private final PatternMatcher pattern;

        private UnshuffledPattern(PatternMatcher pattern) {
            this.pattern = pattern;
        }

        @Override
        public <T> T accept(DefinitionVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof UnshuffledPattern && Objects.equals(pattern, ((UnshuffledPattern) o).pattern);
        }

        public PatternMatcher getPattern() {
            return pattern;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern);
        }

        @Override
        public String toString() {
            return "Unshuffled(" + pattern + ")";
        }
    }

    public static class ValueDefinition extends Definition {

        private final String name;
        private final Value  body;
        private final Type   type;

        private ValueDefinition(String name, Value body, Type type) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(body, other.body)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public Value getBody() {
            return body;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, body, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + " :: " + type + ")";
        }
    }

    public static class ValueSignature extends Definition {

        private final String name;
        private final Type   type;

        private ValueSignature(String name, Type type) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + " :: " + type + ")";
        }
    }
}
