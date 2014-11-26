package scotch.compiler.parser;

import static scotch.compiler.syntax.DefinitionEntry.patternEntry;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.Type.t;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.syntax.DefinitionEntry.ScopedEntry;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.Operator;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Symbol;
import scotch.compiler.syntax.SymbolResolver;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value.ValueVisitor;

public class ScopeBuilder {

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private       String                                    currentModule;
    private       Scope                                     scope;
    private       int                                       sequence;

    public ScopeBuilder(int sequence, SymbolResolver resolver) {
        this.definitions = new HashMap<>();
        this.scope = null;
        this.sequence = sequence;
    }

    public void addPattern(Symbol symbol) {
        scope.addPattern(symbol);
    }

    public PatternMatcher collect(PatternMatcher pattern) {
        definitions.put(pattern.getReference(), patternEntry(pattern, scope));
        return pattern;
    }

    public Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, scope));
        return definition;
    }

    public void defineOperator(Symbol symbol, Operator operator) {
        scope.defineOperator(symbol, operator);
    }

    public void defineSignature(Symbol symbol, Type type) {
        scope.defineSignature(symbol, type);
    }

    public void defineValue(Symbol symbol, Type type) {
        scope.defineValue(symbol, type);
    }

    public void enterScope() {
        scope = scope.enterScope();
    }

    public void enterScope(List<Import> imports) {
        scope = scope.enterScope(currentModule, imports);
    }

    public DefinitionEntry getDefinition(DefinitionReference reference) {
        return definitions.get(reference);
    }

    public List<DefinitionEntry> getDefinitions() {
        return ImmutableList.copyOf(definitions.values());
    }

    public Operator getOperator(Symbol symbol) {
        return scope.getOperator(symbol);
    }

    public int getSequence() {
        return sequence;
    }

    public Optional<Type> getSignature(Symbol symbol) {
        return scope.getSignature(symbol);
    }

    public boolean isOperator(Symbol symbol) {
        return scope.isOperator(symbol);
    }

    public boolean isPattern(Symbol symbol) {
        return scope.isPattern(symbol);
    }

    public void leaveScope() {
        scope = scope.leaveScope();
    }

    public Optional<Symbol> qualify(Symbol symbol) {
        return scope.qualify(symbol);
    }

    public Symbol qualifyCurrent(Symbol symbol) {
        return symbol.qualifyWith(currentModule);
    }

    public void replaceDefinitionValue(DefinitionReference reference, Function<ValueDefinition, ValueVisitor<ValueDefinition>> function) {
        DefinitionEntry entry = getDefinition(reference);
        definitions.put(reference, entry.withDefinition(entry.accept(new DefinitionEntryVisitor<Definition>() {
            @Override
            public Definition visit(ScopedEntry entry) {
                return entry.getDefinition().accept(new DefinitionVisitor<Definition>() {
                    @Override
                    public Definition visit(ValueDefinition definition) {
                        return definition.getBody().accept(function.apply(definition));
                    }
                });
            }
        })));
    }

    public Type reserveType() {
        return t(sequence++);
    }

    public void setCurrentModule(String currentModule) {
        this.currentModule = currentModule;
    }
}
