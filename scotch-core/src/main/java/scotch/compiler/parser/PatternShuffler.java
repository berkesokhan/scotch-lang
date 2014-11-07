package scotch.compiler.parser;

import static java.util.Collections.reverse;
import static scotch.compiler.parser.ParseUtil.nextOperatorHasGreaterPrecedence;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.PatternMatcher.pattern;
import static scotch.compiler.syntax.Value.patterns;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.lang.Either.left;
import static scotch.lang.Either.right;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ParseException;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.syntax.DefinitionEntry.ScopedEntry;
import scotch.compiler.syntax.DefinitionEntry.UnscopedEntry;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Operator;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Symbol;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.data.tuple.Tuple2;
import scotch.lang.Either;
import scotch.lang.Either.EitherVisitor;

public class PatternShuffler {

    private final ScopeBuilder                             scope;
    private final Function<PatternMatcher, PatternMatcher> parser;

    public PatternShuffler(ScopeBuilder scope, Function<PatternMatcher, PatternMatcher> parser) {
        this.scope = scope;
        this.parser = parser;
    }

    public Optional<Definition> shuffle(UnshuffledPattern pattern) {
        return splitPattern(pattern).into(
            (symbol, matches) -> createOrRetrievePattern(symbol).into((optionalDefinition, reference) -> {
                scope.modify(reference, new DefinitionEntryVisitor<Definition>() {
                    @Override
                    public Definition visit(UnscopedEntry entry) {
                        return accumulate(entry.getDefinition());
                    }

                    @Override
                    public Definition visit(ScopedEntry entry) {
                        return accumulate(entry.getDefinition());
                    }

                    private Definition accumulate(Definition definition) {
                        return definition.<Definition>accept(new DefinitionVisitor<Definition>() {
                            @Override
                            public Definition visit(ValueDefinition definition) {
                                return definition.getBody().<Definition>accept(new ValueVisitor<Definition>() {
                                    @Override
                                    public Definition visit(PatternMatchers matchers) {
                                        return definition.withBody(matchers.withMatchers(ImmutableList.<PatternMatcher>builder()
                                                .addAll(matchers.getMatchers())
                                                .add(parser.apply(pattern(pattern.getSymbol(), matches, pattern.getBody())))
                                                .build()
                                        ));
                                    }

                                    @Override
                                    public Definition visitOtherwise(Value value) {
                                        throw new UnsupportedOperationException(); // TODO
                                    }
                                });
                            }

                            @Override
                            public Definition visitOtherwise(Definition definition) {
                                throw new UnsupportedOperationException(); // TODO
                            }
                        });
                    }
                });
                return optionalDefinition;
            })
        );
    }

    private Tuple2<Optional<Definition>, DefinitionReference> createOrRetrievePattern(Symbol symbol) {
        if (scope.isPattern(symbol)) {
            return retrievePattern(symbol);
        } else {
            return createPattern(symbol);
        }
    }

    private Tuple2<Optional<Definition>, DefinitionReference> createPattern(Symbol symbol) {
        Type type = tryGetType(symbol);
        Definition definition = scope.collect(value(symbol, type, patterns(scope.reserveType())));
        scope.defineValue(symbol, type);
        scope.addPattern(symbol);
        return tuple2(Optional.of(definition), definition.getReference());
    }

    private Type tryGetType(Symbol symbol) {
        return scope.getSignature(symbol).orElseGet(scope::reserveType);
    }

    private boolean expectsArgument(Deque<PatternMatch> input) {
        return !input.isEmpty() && !isOperator(input.peek());
    }

    private OperatorPair<CaptureMatch> getOperator(PatternMatch match, boolean expectsPrefix) {
        return match.accept(new PatternMatchVisitor<OperatorPair<CaptureMatch>>() {
            @Override
            public OperatorPair<CaptureMatch> visit(CaptureMatch match) {
                Operator operator = scope.qualify(match.getSymbol()).map(scope::getOperator).get(); // TODO
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + quote(match.getSymbol()));
                }
                return new OperatorPair<>(operator, match);
            }

            @Override
            public OperatorPair<CaptureMatch> visitOtherwise(PatternMatch match) {
                throw new UnsupportedOperationException(); // TODO
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

    private Tuple2<Optional<Definition>, DefinitionReference> retrievePattern(Symbol symbol) {
        return tuple2(Optional.empty(), valueRef(symbol));
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

    private Tuple2<Symbol, List<PatternMatch>> splitPattern(UnshuffledPattern pattern) {
        List<PatternMatch> matches = shufflePattern(pattern.getMatches());
        Symbol symbol = matches.remove(0).accept(new PatternMatchVisitor<Symbol>() {
            @Override
            public Symbol visit(CaptureMatch match) {
                return scope.qualifyCurrent(match.getSymbol());
            }

            @Override
            public Symbol visitOtherwise(PatternMatch match) {
                throw new ParseException("Illegal start of pattern: " + match.getClass().getSimpleName()); // TODO better error
            }
        });
        return tuple2(symbol, matches);
    }
}
