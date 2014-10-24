package scotch.compiler.parser;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionEntry.scopedEntry;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.message;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.lang.Either.left;
import static scotch.lang.Either.right;
import static scotch.lang.Type.t;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ParseException;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ModuleDefinition;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.Definition.RootDefinition;
import scotch.compiler.ast.Definition.UnshuffledPattern;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.ast.DefinitionReference.ModuleReference;
import scotch.compiler.ast.DefinitionReference.OperatorReference;
import scotch.compiler.ast.DefinitionReference.PatternReference;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.PatternMatch;
import scotch.compiler.ast.PatternMatch.CaptureMatch;
import scotch.compiler.ast.PatternMatch.PatternMatchVisitor;
import scotch.compiler.ast.PatternMatcher;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Value;
import scotch.compiler.ast.Value.Identifier;
import scotch.compiler.ast.Value.Message;
import scotch.compiler.ast.Value.PatternMatchers;
import scotch.compiler.ast.Value.ValueVisitor;
import scotch.lang.Either;
import scotch.lang.Either.EitherVisitor;
import scotch.lang.Symbol;
import scotch.lang.Type;
import scotch.lang.Type.TypeVisitor;

public class AstParser implements
    DefinitionReferenceVisitor<Optional<DefinitionReference>>,
    DefinitionVisitor<Optional<Definition>>,
    DefinitionEntryVisitor<DefinitionEntry>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch>,
    TypeVisitor<Type> {

    private final SymbolTable                               symbols;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final ScopeBuilder                              scope;
    private       String                                    currentModule;
    private       int                                       sequence;

    public AstParser(SymbolTable symbols) {
        this.symbols = symbols;
        this.definitions = new HashMap<>();
        this.scope = new ScopeBuilder();
        this.sequence = symbols.getSequence();
    }

    public SymbolTable analyze() {
        symbols.getDefinition(rootRef()).accept(this);
        return new SymbolTable(sequence, ImmutableList.copyOf(definitions.values()));
    }

    @Override
    public Optional<Definition> visit(ModuleDefinition definition) {
        return scoped(() -> collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions()))));
    }

    @Override
    public Optional<DefinitionReference> visit(ModuleReference reference) {
        currentModule = reference.getName();
        return symbols.getDefinition(reference).accept(this).map(Definition::getReference);
    }

    @Override
    public Optional<Definition> visit(OperatorDefinition definition) {
        scope.defineOperator(definition.getSymbol(), definition);
        return collect(definition);
    }

    @Override
    public Optional<DefinitionReference> visit(OperatorReference reference) {
        return symbols.getDefinition(reference).accept(this).map(Definition::getReference);
    }

    @Override
    public Optional<DefinitionReference> visit(PatternReference reference) {
        return symbols.getDefinition(reference).accept(this).map(Definition::getReference);
    }

    @Override
    public Optional<Definition> visit(RootDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Optional<Definition> visit(UnshuffledPattern pattern) {
        PatternMatcher matcher = pattern.getPattern();
        List<PatternMatch> matches = shufflePattern(matcher.getMatches());
        Symbol symbol = matches.remove(0).accept(new PatternMatchVisitor<Symbol>() {
            @Override
            public Symbol visit(CaptureMatch match) {
                return match.getSymbol().qualifyWith(currentModule);
            }

            @Override
            public Symbol visitOtherwise(PatternMatch match) {
                throw new ParseException("Illegal start of pattern: " + match.getClass().getSimpleName()); // TODO better error
            }
        });
        return collect(value(symbol, reserveType(), patterns(pattern(matches, matcher.getBody())).accept(this)));
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(matchers.getMatchers().stream().map(this::visitMatcher).collect(toList()));
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        scope.defineValue(match.getSymbol(), match.getType());
        return match;
    }

    @Override
    public Optional<Definition> visit(ValueDefinition definition) {
        scope.defineValue(scope.qualify(definition.getSymbol()), definition.getType());
        return scoped(() -> collect(definition.withBody(definition.getBody().accept(this))));
    }

    @Override
    public Value visit(Message message) {
        return shuffleMessage(message.getMembers());
    }

    private Optional<Definition> collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, scope.build()));
        return Optional.of(definition);
    }

    private boolean expectsArgument(Deque input) {
        return !input.isEmpty() && expectsArgument_(input);
    }

    private boolean expectsArgument_(Deque input) {
        if (input.peek() instanceof PatternMatch) {
            return !isOperator((PatternMatch) input.peek());
        } else {
            return !isOperator((Value) input.peek());
        }
    }

    private OperatorPair<CaptureMatch> getOperator(PatternMatch match, boolean expectsPrefix) {
        return match.accept(new PatternMatchVisitor<OperatorPair<CaptureMatch>>() {
            @Override
            public OperatorPair<CaptureMatch> visit(CaptureMatch match) {
                Operator operator = scope.getOperator(match.getSymbol());
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + quote(match.getSymbol()));
                }
                return new OperatorPair<>(operator, match);
            }
        });
    }

    private OperatorPair<Identifier> getOperator(Value value, boolean expectsPrefix) {
        return value.accept(new ValueVisitor<OperatorPair<Identifier>>() {
            @Override
            public OperatorPair<Identifier> visit(Identifier identifier) {
                Operator operator = scope.getOperator(identifier.getSymbol());
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + identifier.getSymbol());
                }
                return new OperatorPair<>(scope.getOperator(identifier.getSymbol()), identifier);
            }
        });
    }

    private boolean isOperator(PatternMatch match) {
        return match.accept(new PatternMatchVisitor<Boolean>() {
            @Override
            public Boolean visit(CaptureMatch match) {
                return scope.isOperator(match.getSymbol());
            }

            @Override
            public Boolean visitOtherwise(PatternMatch match) {
                return false;
            }
        });
    }

    private boolean isOperator(Value value) {
        return value.accept(new ValueVisitor<Boolean>() {
            @Override
            public Boolean visit(Identifier identifier) {
                return scope.isOperator(identifier.getSymbol());
            }

            @Override
            public Boolean visitOtherwise(Value value) {
                return false;
            }
        });
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    }

    private <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, Deque<OperatorPair<T>> stack) {
        return !stack.isEmpty() && nextOperatorHasGreaterPrecedence(current, stack.peek());
    }

    private <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, OperatorPair<T> next) {
        return current.isLeftAssociative() && current.hasSamePrecedenceAs(next)
            || current.hasLessPrecedenceThan(next);
    }

    private Type reserveType() {
        return t(sequence++);
    }

    private <T> T scoped(Supplier<T> supplier) {
        scope.enterScope(currentModule);
        try {
            return supplier.get();
        } finally {
            scope.leaveScope();
        }
    }

    private Value shuffleMessage(List<Value> message) {
        if (message.size() == 1) {
            return message(message);
        } else {
            return shuffleMessage_(message);
        }
    }

    private Value shuffleMessageApply(Deque<Either<OperatorPair<Identifier>, Value>> message) {
        Deque<Value> stack = new ArrayDeque<>();
        while (!message.isEmpty()) {
            stack.push(message.pollLast().getRightOr(pair -> {
                if (pair.isPrefix()) {
                    return apply(pair.getValue(), stack.pop(), reserveType());
                } else {
                    Value right = stack.pop();
                    Value left = stack.pop();
                    return apply(apply(pair.getValue(), left, reserveType()), right, reserveType());
                }
            }));
        }
        if (stack.size() > 1) {
            stack.push(apply(stack.pollLast(), stack.pollLast(), reserveType()));
        }
        return stack.pop();
    }

    private Value shuffleMessage_(List<Value> message) {
        Deque<Value> input = new ArrayDeque<>(message);
        Deque<Either<OperatorPair<Identifier>, Value>> output = new ArrayDeque<>();
        Deque<OperatorPair<Identifier>> stack = new ArrayDeque<>();
        boolean expectsPrefix = isOperator(input.peek());
        while (!input.isEmpty()) {
            if (isOperator(input.peek())) {
                OperatorPair<Identifier> o1 = getOperator(input.poll(), expectsPrefix);
                while (nextOperatorHasGreaterPrecedence(o1, stack)) {
                    output.push(left(stack.pop()));
                }
                stack.push(o1);
                expectsPrefix = isOperator(input.peek());
            } else {
                output.push(right(shuffleNext(input)));
                while (expectsArgument(input)) {
                    output.push(right(apply(
                        output.pop().getRightOr(OperatorPair::getValue),
                        shuffleNext(input),
                        reserveType()
                    )));
                }
            }
        }
        while (!stack.isEmpty()) {
            output.push(left(stack.pop()));
        }
        return shuffleMessageApply(output);
    }

    private Value shuffleNext(Deque<Value> input) {
        return input.poll().accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Message message) {
                return shuffleMessage(message.getMembers());
            }

            @Override
            public Value visitOtherwise(Value value) {
                return value;
            }
        });
    }

    private List<PatternMatch> shufflePattern(List<PatternMatch> matches) {
        Deque<PatternMatch> input = new ArrayDeque<>(matches);
        Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> output = new ArrayDeque<>();
        Deque<OperatorPair<CaptureMatch>> stack = new ArrayDeque<>();
        boolean expectsPrefix = isOperator(input.peek());
        while (!input.isEmpty()) {
            if (isOperator(input.peek())) {
                OperatorPair<CaptureMatch> o1 = getOperator(input.poll(), expectsPrefix);
                while (nextOperatorHasGreaterPrecedence(o1, stack)) {
                    output.push(left(stack.pop()));
                }
                stack.push(o1);
                expectsPrefix = isOperator(input.peek());
            } else {
                output.push(right(input.poll()));
                while (expectsArgument(input)) {
                    output.push(right(input.poll()));
                }
            }
        }
        while (!stack.isEmpty()) {
            output.push(left(stack.pop()));
        }
        return shufflePatternApply(output);
    }

    private List<PatternMatch> shufflePatternApply(Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> input) {
        Deque<PatternMatch> output = new ArrayDeque<>();
        while (!input.isEmpty()) {
            input.pollLast().accept(new EitherVisitor<OperatorPair<CaptureMatch>, PatternMatch, Void>() {
                @Override
                public Void visitLeft(OperatorPair<CaptureMatch> left) {
                    if (left.isPrefix()) {
                        PatternMatch head = output.pop();
                        output.push(left.getValue());
                        output.push(head);
                    } else {
                        PatternMatch l = output.pop();
                        PatternMatch r = output.pop();
                        output.push(left.getValue());
                        output.push(r);
                        output.push(l);
                    }
                    return null;
                }

                @Override
                public Void visitRight(PatternMatch right) {
                    output.push(right);
                    return null;
                }
            });
        }
        List<PatternMatch> matches = new ArrayList<>(output);
        reverse(matches);
        return matches;
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(() -> matcher
                .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
                .withBody(matcher.getBody().accept(this))
        );
    }

    private static final class OperatorPair<T> {

        private final Operator operator;
        private final T        value;

        private OperatorPair(Operator operator, T value) {
            this.operator = operator;
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public boolean hasLessPrecedenceThan(OperatorPair other) {
            return operator.hasLessPrecedenceThan(other.operator);
        }

        public boolean hasSamePrecedenceAs(OperatorPair other) {
            return operator.hasSamePrecedenceAs(other.operator);
        }

        public boolean isLeftAssociative() {
            return operator.isLeftAssociative();
        }

        public boolean isPrefix() {
            return operator.isPrefix();
        }
    }
}
