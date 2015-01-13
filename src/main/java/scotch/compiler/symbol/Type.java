package scotch.compiler.symbol;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Unification.circular;
import static scotch.compiler.symbol.Unification.contextMismatch;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;

public abstract class Type {

    public static FunctionType fn(Type argument, Type result) {
        return new FunctionType(NULL_SOURCE, argument, result);
    }

    public static InstanceType instance(Symbol symbol, Type binding) {
        return new InstanceType(symbol, binding);
    }

    public static InstanceType instance(String name, Type binding) {
        return instance(fromString(name), binding);
    }

    public static SumType sum(String name) {
        return sum(name, ImmutableList.of());
    }

    public static SumType sum(String name, List<Type> arguments) {
        return sum(fromString(name));
    }

    public static SumType sum(Symbol symbol) {
        return new SumType(NULL_SOURCE, symbol);
    }

    public static VariableType t(int id) {
        return t(id, emptyList());
    }

    public static VariableType t(int id, List context) {
        return var("t" + id, context);
    }

    public static VariableType var(String name) {
        return var(name, ImmutableList.of());
    }

    public static VariableType var(String name, Collection<?> context) {
        return new VariableType(NULL_SOURCE, name, toSymbols(context));
    }

    private static int sort(Tuple2<VariableType, Symbol> left, Tuple2<VariableType, Symbol> right) {
        return left.into((t1, s1) -> right.into((t2, s2) -> {
            int result = t1.getName().compareTo(t2.getName());
            if (result != 0) {
                return result;
            }
            return s1.compareTo(s2);
        }));
    }

    @SuppressWarnings("unchecked")
    private static Set<Symbol> toSymbols(Collection<?> context) {
        return context.stream()
            .map(item -> item instanceof String ? fromString((String) item) : (Symbol) item)
            .collect(Collectors.toSet());
    }

    private static Unification unifyVariable(Type actual, VariableType target, TypeScope scope) {
        if (scope.isBound(target)) {
            return scope.getTarget(target).unify(actual, scope);
        } else if (target.context.isEmpty()) {
            return scope.bind(target, actual);
        } else if (scope.getContext(actual).containsAll(target.context)) {
            return scope.bind(target, actual);
        } else {
            return contextMismatch(target, actual, target.context, scope.getContext(actual));
        }
    }

    private Type() {
        // intentionally Optional.empty
    }

    public abstract <T> T accept(TypeVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public Set<Tuple2<VariableType, Symbol>> getContexts() {
        return gatherContext_();
    }

    public abstract Map<String, Type> getContexts(Type type, TypeScope scope);

    public abstract String getSignature();

    public abstract SourceRange getSourceRange();

    public boolean hasContext() {
        return !getContexts().isEmpty();
    }

    @Override
    public abstract int hashCode();

    public Type simplify() {
        return this;
    }

    @Override
    public abstract String toString();

    public abstract Unification unify(Type type, TypeScope scope);

    protected abstract boolean contains(VariableType type);

    protected String gatherContext() {
        Set<Tuple2<VariableType, Symbol>> context = gatherContext_();
        if (context.isEmpty()) {
            return "";
        } else {
            return "(" + context.stream()
                .map(tuple -> tuple.into((type, symbol) -> symbol.getMemberName() + " " + type.name))
                .collect(joining(", ")) + ") => ";
        }
    }

    protected abstract Set<Tuple2<VariableType, Symbol>> gatherContext_();

    protected abstract String getSignature_();

    protected abstract String toParenthesizedString();

    protected abstract String toString_();

    protected abstract Unification unifyWith(FunctionType target, TypeScope scope);

    protected abstract Unification unifyWith(VariableType target, TypeScope scope);

    protected abstract Unification unifyWith(SumType target, TypeScope scope);

    public interface TypeVisitor<T> {

        default T visit(FunctionType type) {
            return visitOtherwise(type);
        }

        default T visit(InstanceType type) {
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

        @Override
        public Map<String, Type> getContexts(Type type, TypeScope scope) {
            Map<String, Type> map = new HashMap<>();
            type.accept(new TypeVisitor<Void>() {
                @Override
                public Void visit(FunctionType type) {
                    map.putAll(argument.getContexts(type.getArgument(), scope));
                    map.putAll(result.getContexts(type.getResult(), scope));
                    return null;
                }

                @Override
                public Void visitOtherwise(Type type) {
                    return null;
                }
            });
            return map;
        }

        public Type getResult() {
            return result;
        }

        @Override
        public String getSignature() {
            return "(" + argument.getSignature_() + ");" + result.getSignature_();
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(argument, result);
        }

        @Override
        public String toString() {
            return gatherContext() + toString_();
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
        protected boolean contains(VariableType type) {
            return argument.contains(type) || result.contains(type);
        }

        @Override
        protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
            Set<Tuple2<VariableType, Symbol>> context = new HashSet<>();
            context.addAll(argument.gatherContext_());
            context.addAll(result.gatherContext_());
            return ImmutableSortedSet.copyOf(Type::sort, context);
        }

        @Override
        protected String getSignature_() {
            return p(Function.class);
        }

        @Override
        protected String toParenthesizedString() {
            return "(" + argument.toParenthesizedString() + " -> " + result.toString_() + ")";
        }

        @Override
        protected String toString_() {
            return argument.toParenthesizedString() + " -> " + result.toString_();
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope scope) {
            return mismatch(target, this);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope scope) {
            return target.argument.unify(argument, scope).andThen(
                argumentResult -> target.result.unify(result, scope).andThen(
                    resultResult -> unified(fn(argumentResult, resultResult))
                )
            );
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope scope) {
            return unifyVariable(this, target, scope);
        }
    }

    public static class InstanceType extends Type {

        private final Symbol symbol;
        private final Type binding;

        private InstanceType(Symbol symbol, Type binding) {
            this.symbol = symbol;
            this.binding = binding;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof InstanceType && Objects.equals(symbol, ((InstanceType) o).symbol);
        }

        public Type getBinding() {
            return binding;
        }

        @Override
        public Map<String, Type> getContexts(Type type, TypeScope scope) {
            return ImmutableMap.of();
        }

        @Override
        public String getSignature() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public SourceRange getSourceRange() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public boolean hasContext() {
            return binding instanceof VariableType;
        }

        public boolean isBound() {
            return !(binding instanceof VariableType);
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        public boolean is(Type type) {
            return binding.simplify().equals(type.simplify());
        }

        @Override
        public String toString() {
            return toString_();
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            throw new UnsupportedOperationException();
        }

        public InstanceType withBinding(Type binding) {
            return new InstanceType(symbol, binding);
        }

        @Override
        protected boolean contains(VariableType type) {
            return false;
        }

        @Override
        protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
            return ImmutableSet.of();
        }

        @Override
        protected String getSignature_() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        protected String toParenthesizedString() {
            return toString_();
        }

        @Override
        protected String toString_() {
            return symbol.getMemberName();
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope scope) {
            throw new UnsupportedOperationException();
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
        public Map<String, Type> getContexts(Type type, TypeScope scope) {
            return ImmutableMap.of();
        }

        @Override
        public String getSignature() {
            return "()L" + getSignature_() + ";";
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
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return toString_();
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
        protected boolean contains(VariableType type) {
            return false;
        }

        @Override
        protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
            return ImmutableSet.of();
        }

        @Override
        protected String getSignature_() {
            return symbol.getClassName();
        }

        @Override
        protected String toParenthesizedString() {
            return toString_();
        }

        @Override
        protected String toString_() {
            return symbol.getMemberName();
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope scope) {
            if (equals(target)) {
                return unified(this);
            } else {
                return mismatch(target, this);
            }
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope scope) {
            return mismatch(target, this);
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope scope) {
            return unifyVariable(this, target, scope);
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
        public Map<String, Type> getContexts(Type type, TypeScope scope) {
            Map<String, Type> map = new HashMap<>();
            if (!context.isEmpty() && context.containsAll(scope.getContext(type))) {
                map.put(name, type.accept(new TypeVisitor<Type>() {
                    @Override
                    public Type visit(VariableType type) {
                        return VariableType.this;
                    }

                    @Override
                    public Type visitOtherwise(Type type) {
                        return type;
                    }
                }));
            }
            return map;
        }

        public String getName() {
            return name;
        }

        @Override
        public String getSignature() {
            return sig(Object.class);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, context);
        }

        public boolean is(String variable) {
            return Objects.equals(name, variable);
        }

        @Override
        public VariableType simplify() {
            if (context.isEmpty()) {
                return this;
            } else {
                return var(name);
            }
        }

        @Override
        public String toString() {
            return gatherContext() + toString_();
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

        private Unification bind(Type target, TypeScope scope) {
            return scope.bind(this, target);
        }

        private Optional<Unification> unify_(Type target, TypeScope scope) {
            if (scope.isBound(this)) {
                return Optional.of(target.unify(scope.getTarget(this), scope));
            } else if (target.contains(this) && !equals(target)) {
                return Optional.of(circular(target, this));
            } else {
                return Optional.empty();
            }
        }

        @Override
        protected boolean contains(VariableType type) {
            return equals(type);
        }

        @Override
        protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
            return ImmutableSortedSet.copyOf(Type::sort, context.stream().map(s -> tuple2(this, s)).collect(toList()));
        }

        @Override
        protected String getSignature_() {
            return p(Object.class);
        }

        @Override
        protected String toParenthesizedString() {
            return toString_();
        }

        @Override
        protected String toString_() {
            return name;
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope scope) {
            return unify_(target, scope).orElseGet(() -> bind(target, scope));
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope scope) {
            return unify_(target, scope).orElseGet(() -> bind(target, scope));
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope scope) {
            if (scope.isBound(this)) {
                return target.unify(scope.getTarget(this), scope);
            } else {
                Set<Symbol> additionalContext = new HashSet<>();
                additionalContext.addAll(context);
                additionalContext.addAll(target.context);
                scope.extendContext(target, additionalContext);
                scope.extendContext(this, additionalContext);
                return bind(target, scope);
            }
        }
    }
}
