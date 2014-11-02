package scotch.compiler.analyzer;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.DefinitionEntry.scopedEntry;
import static scotch.compiler.ast.DefinitionReference.rootRef;
import static scotch.compiler.ast.Type.fn;
import static scotch.compiler.ast.Type.sum;
import static scotch.compiler.ast.Type.t;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ModuleDefinition;
import scotch.compiler.ast.Definition.RootDefinition;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.ast.PatternMatch;
import scotch.compiler.ast.PatternMatch.CaptureMatch;
import scotch.compiler.ast.PatternMatch.PatternMatchVisitor;
import scotch.compiler.ast.PatternMatcher;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Type;
import scotch.compiler.ast.Unification;
import scotch.compiler.ast.Unification.CircularReference;
import scotch.compiler.ast.Unification.TypeMismatch;
import scotch.compiler.ast.Unification.UnificationVisitor;
import scotch.compiler.ast.Unification.Unified;
import scotch.compiler.ast.Value;
import scotch.compiler.ast.Value.Apply;
import scotch.compiler.ast.Value.Identifier;
import scotch.compiler.ast.Value.LiteralValue;
import scotch.compiler.ast.Value.PatternMatchers;
import scotch.compiler.ast.Value.ValueVisitor;

public class TypeAnalyzer implements
    DefinitionReferenceVisitor<DefinitionReference>,
    DefinitionVisitor<Definition>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch> {

    private final SymbolTable                               symbols;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;

    public TypeAnalyzer(SymbolTable symbols) {
        this.symbols = symbols;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
    }

    public SymbolTable analyze() {
        symbols.getDefinition(rootRef()).accept(this);
        return symbols.copyWith(ImmutableList.copyOf(definitions.values()));
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(RootDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        Value body = definition.getBody().accept(this);
        currentScope().redefineValue(definition.getSymbol(), body.getType());
        return collect(definition.withBody(body).withType(body.getType()));
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        List<PatternMatcher> patterns = matchers.getMatchers().stream().map(this::visitMatcher).collect(toList());
        Type type = matchers.getType();
        for (PatternMatcher pattern : patterns) {
            pattern.withType(type.unify(pattern.getType(), currentScope()).accept(new UnificationVisitor<Type>() {
                @Override
                public Type visit(Unified unified) {
                    return unified.getUnifiedType();
                }

                @Override
                public Type visitOtherwise(Unification unification) {
                    throw new UnsupportedOperationException(); // TODO
                }
            }));
            type = currentScope().generate(type);
        }
        return matchers.withMatchers(patterns).withType(type);
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        return match;
    }

    @Override
    public Value visit(Identifier identifier) {
        return identifier.withType(currentScope().getValue(identifier.getSymbol()));
    }

    @Override
    public Value visit(Apply apply) {
        Value function = apply.getFunction().accept(this);
        Value argument = apply.getArgument().accept(this);
        Type resultType = t(0);
        return function.getType().unify(fn(argument.getType(), resultType), currentScope()).accept(new UnificationVisitor<Value>() {
            @Override
            public Value visit(Unified unified) {
                return apply.withType(currentScope().getTarget(resultType));
            }

            @Override
            public Value visitOtherwise(Unification unification) {
                return report(unification);
            }
        });
    }

    @Override
    public Value visit(LiteralValue literal) {
        Object value = literal.getValue();
        Type type;
        if (value instanceof Double) {
            type = sum("scotch.data.double.Double");
        } else if (value instanceof Integer) {
            type = sum("scotch.data.int.Int");
        } else if (value instanceof String) {
            type = sum("scotch.data.string.String");
        } else if (value instanceof Boolean) {
            type = sum("scotch.data.bool.Bool");
        } else if (value instanceof Character) {
            type = sum("scotch.data.char.Char");
        } else {
            throw new UnsupportedOperationException(); // TODO
        }
        return literal.withType(type);
    }

    @Override
    public DefinitionReference visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> symbols.getDefinition(reference).accept(this).getReference());
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, getScope(definition.getReference())));
        return definition;
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Scope getScope(DefinitionReference reference) {
        return symbols.getScope(reference);
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .collect(toList());
    }

    private <T> T report(Unification unification) {
        return unification.accept(new UnificationVisitor<T>() {
            @Override
            public T visit(CircularReference circularReference) {
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public T visit(TypeMismatch typeMismatch) {
                throw new UnsupportedOperationException(); // TODO
            }
        });
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(symbols.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(matcher.getReference(), () -> matcher
                .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
                .withBody(matcher.getBody().accept(this))
        );
    }
}
