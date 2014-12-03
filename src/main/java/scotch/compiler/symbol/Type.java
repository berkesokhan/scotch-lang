package scotch.compiler.symbol;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Unification.circular;
import static scotch.compiler.symbol.Unification.contextMismatch;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;
import static scotch.util.StringUtil.stringify;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.text.SourceRange;

public abstract class Type {

    public static Type fn(Type argument, Type result) {
        return new FunctionType(NULL_SOURCE, argument, result);
    }

    public static Type sum(String name) {
        return sum(name, emptyList());
    }

    public static Type sum(String name, List<Type> arguments) {
        return sum(fromString(name));
    }

    public static Type sum(Symbol symbol) {
        return new SumType(NULL_SOURCE, symbol);
    }

    public static Type t(int id) {
        return var("t" + id);
    }

    public static VariableType var(String name) {
        return var(name, emptyList());
    }

    @SuppressWarnings("unchecked")
    public static VariableType var(String name, List context) {
        Set<Symbol> contextSymbols = new HashSet<>();
        if (!context.isEmpty()) {
            if (context.get(0) instanceof String) {
                contextSymbols = ((List<String>) context).stream()
                    .map(Symbol::fromString)
                    .collect(toSet());
            } else if (context.get(0) instanceof Symbol) {
                contextSymbols.addAll(context);
            } else {
                throw new IllegalArgumentException("Got list of " + context.get(0).getClass());
            }
        }
        return new VariableType(NULL_SOURCE, name, contextSymbols);
    }

    private static Unification unifyVariable(Type actual, VariableType target, TypeScope typeScope) {
        if (typeScope.isBound(target)) {
            return typeScope.getTarget(target).unify(actual, typeScope);
        } else if (target.context.isEmpty()) {
            typeScope.bind(target, actual);
            return unified(actual);
        } else if (typeScope.getContext(actual).containsAll(target.context)) {
            typeScope.bind(target, actual);
            return unified(actual);
        } else {
            Set<Symbol> contextDifference = new HashSet<>();
            contextDifference.addAll(target.context);
            contextDifference.removeAll(typeScope.getContext(actual));
            return contextMismatch(target, actual, contextDifference);
        }
    }

    private Type() {
        // intentionally Optional.empty
    }

    public abstract <T> T accept(TypeVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract String getSignature();

    @Override
    public abstract int hashCode();

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    public abstract Unification unify(Type type, TypeScope scope);

    protected abstract boolean contains(VariableType variableType);

    protected abstract String getSignature_();

    protected abstract Unification unifyWith(VariableType target, TypeScope typeScope);

    protected abstract Unification unifyWith(FunctionType target, TypeScope typeScope);

    protected abstract Unification unifyWith(SumType target, TypeScope typeScope);

    public interface TypeVisitor<T> {

        default T visit(FunctionType type) {
            return visitOtherwise(type);
        }

        default T visit(VariableType type) {
            return visitOtherwise(type);
        }

        default T visit(SumType type) {
            return visitOtherwise(type);
        }

        default T visitOtherwise(Type type) {
            throw new UnsupportedOperationException("Can't visit " + type.getClass().getSimpleName());
        }
    }

    public static class FunctionType extends Type {

        private final SourceRange sourceRange;
        private final Type        argument;
        private final Type        result;

        private FunctionType(SourceRange sourceRange, Type argument, Type result) {
            this.sourceRange = sourceRange;
            this.argument = argument;
            this.result = result;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FunctionType) {
                FunctionType other = (FunctionType) o;
                return argument.equals(other.argument) && result.equals(other.result);
            } else {
                return false;
            }
        }

        public Type getArgument() {
            return argument;
        }

        public Type getResult() {
            return result;
        }

        @Override
        public String getSignature() {
            return "(" + argument.getSignature_() + ");" + result.getSignature_();
        }

        @Override
        public int hashCode() {
            return Objects.hash(argument, result);
        }

        @Override
        public String prettyPrint() {
            return argument.accept(new TypeVisitor<String>() {
                @Override
                public String visit(FunctionType type) {
                    return "(" + type.prettyPrint() + ")";
                }

                @Override
                public String visitOtherwise(Type type) {
                    return type.prettyPrint();
                }
            }) + " -> " + result.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + argument + " -> " + result + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        public FunctionType withArgument(Type argument) {
            return new FunctionType(sourceRange, argument, result);
        }

        public FunctionType withResult(Type result) {
            return new FunctionType(sourceRange, argument, result);
        }

        public FunctionType withSourceRange(SourceRange sourceRange) {
            return new FunctionType(sourceRange, argument, result);
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return argument.contains(variableType) || result.contains(variableType);
        }

        @Override
        protected String getSignature_() {
            return p(Function.class);
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unifyVariable(this, target, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return target.argument.unify(argument, typeScope).andThen(
                argumentResult -> target.result.unify(result, typeScope).andThen(
                    resultResult -> unified(fn(argumentResult, resultResult))
                )
            );
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            return mismatch(target, this);
        }
    }

    public static class SumType extends Type {

        private final SourceRange sourceRange;
        private final Symbol      symbol;

        private SumType(SourceRange sourceRange, Symbol symbol) {
            shouldBeUpperCase(symbol.getMemberName());
            this.sourceRange = sourceRange;
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof SumType) {
                SumType other = (SumType) o;
                return Objects.equals(symbol, other.symbol);
            } else {
                return false;
            }
        }

        @Override
        public String getSignature() {
            return "()L" + getSignature_() + ";";
        }

        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String prettyPrint() {
            return symbol.getCanonicalName();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        public SumType withSourceRange(SourceRange sourceRange) {
            return new SumType(sourceRange, symbol);
        }

        public SumType withSymbol(Symbol symbol) {
            return new SumType(sourceRange, symbol);
        }

        private void shouldBeUpperCase(String name) {
            if (!isUpperCase(name.charAt(0))) {
                throw new IllegalArgumentException("Sum type should have upper-case name: got '" + name + "'");
            }
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return false;
        }

        @Override
        protected String getSignature_() {
            return symbol.getClassName();
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unifyVariable(this, target, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return mismatch(target, this);
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            if (equals(target)) {
                return unified(this);
            } else {
                return mismatch(target, this);
            }
        }
    }

    public static class VariableType extends Type {

        private final SourceRange sourceRange;
        private final String      name;
        private final Set<Symbol> context;

        private VariableType(SourceRange sourceRange, String name, Collection<Symbol> context) {
            if (!isLowerCase(name.charAt(0))) {
                throw new IllegalArgumentException("Variable type should have lower-case name: got '" + name + "'");
            }
            this.sourceRange = sourceRange;
            this.name = name;
            this.context = ImmutableSet.copyOf(context);
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof VariableType) {
                VariableType other = (VariableType) o;
                return Objects.equals(name, other.name)
                    && Objects.equals(context, other.context);
            } else {
                return false;
            }
        }

        public Set<Symbol> getContext() {
            return new HashSet<>(context);
        }

        @Override
        public String getSignature() {
            return sig(Object.class);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, context);
        }

        @Override
        public String prettyPrint() {
            return name;
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ", context=" + context + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        public VariableType withContext(Collection<Symbol> context) {
            return new VariableType(sourceRange, name, context);
        }

        public VariableType withSourceRange(SourceRange sourceRange) {
            return new VariableType(sourceRange, name, context);
        }

        private Unification bind(Type target, TypeScope typeScope) {
            typeScope.bind(this, target);
            return unified(target);
        }

        private Optional<Unification> unify_(Type target, TypeScope typeScope) {
            if (typeScope.isBound(this)) {
                return Optional.of(target.unify(typeScope.getTarget(this), typeScope));
            } else if (target.contains(this)) {
                return Optional.of(circular(target, this));
            } else {
                return Optional.empty();
            }
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return equals(variableType);
        }

        @Override
        protected String getSignature_() {
            return p(Object.class);
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unify_(target, typeScope).orElseGet(() -> {
                Set<Symbol> additionalContext = new HashSet<>();
                additionalContext.addAll(context);
                additionalContext.addAll(target.context);
                typeScope.extendContext(target, additionalContext);
                typeScope.extendContext(this, additionalContext);
                return bind(target, typeScope);
            });
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return unify_(target, typeScope).orElseGet(() -> bind(target, typeScope));
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            return unify_(target, typeScope).orElseGet(() -> bind(target, typeScope));
        }
    }
}
