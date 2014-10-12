package scotch.compiler.ast;

import static java.util.Collections.reverse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.Symbol.symbol;
import static scotch.compiler.util.TextUtil.normalizeQualified;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.splitQualified;
import static scotch.lang.Type.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.analyzer.SymbolNotFoundException;
import scotch.compiler.analyzer.SymbolResolver;
import scotch.compiler.ast.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.ast.DefinitionReference.ModuleReference;
import scotch.compiler.ast.DefinitionReference.ValueReference;
import scotch.lang.Type;

public class Scope {

    public static Scope scope(Optional<Scope> parent) {
        return new Scope(parent);
    }

    private final Optional<Scope>               optionalParent;
    private final Map<String, Symbol>           localSymbols;
    private final Map<String, Symbol>           forwardSymbols;
    private final List<Import>                  imports;
    private       Optional<DefinitionReference> reference;

    private Scope(Optional<Scope> optionalParent) {
        this.optionalParent = optionalParent;
        this.localSymbols = new HashMap<>();
        this.forwardSymbols = new HashMap<>();
        this.imports = new ArrayList<>();
        this.reference = empty();
    }

    public void define(String name, Type type) {
        if (isLocal(name)) {
            throw new IllegalStateException("Can't redefine local " + quote(name));
        } else {
            localSymbols.computeIfAbsent(name, k -> symbol(name)).setValueType(type);
        }
    }

    public Symbol forwardReference(String name) {
        if (forwardSymbols.containsKey(name)) {
            return forwardSymbols.get(name);
        } else if (optionalParent.isPresent()) {
            optionalParent.get().forwardReference(name);
            forwardSymbols.put(name, getSymbol(name));
            return getSymbol(name);
        } else {
            forwardSymbols.put(name, symbol(name));
            return getSymbol(name);
        }
    }

    public List<Symbol> getForwardReferences() {
        return ImmutableList.copyOf(forwardSymbols.values());
    }

    public List<Import> getImports() {
        return imports;
    }

    public Scope getParent() {
        return optionalParent.orElse(null); // TODO null? really?
    }

    public DefinitionReference getReference() {
        return reference.orElseThrow(() -> new IllegalStateException("Can't get reference from unnamed scope"));
    }

    public List<Symbol> getSymbols() {
        return ImmutableList.<Symbol>builder()
            .addAll(localSymbols.values())
            .addAll(forwardSymbols.values())
            .build();
    }

    public Type getType(String name) {
        return splitQualified(name).into(
            (optionalModuleName, memberName) -> {
                if (isDefined(name)) {
                    return getSymbol(name).getType();
                } else {
                    Symbol symbol = forwardReference(name);
                    symbol.setType(lookup(normalizeQualified(optionalModuleName, name)));
                    return symbol.getType();
                }
            }
        );
    }

    public Type getValueType(String name) {
        if (isDefined(name)) {
            if (localSymbols.containsKey(name)) {
                return localSymbols.get(name).getValueType();
            } else if (forwardSymbols.containsKey(name)) {
                return forwardSymbols.get(name).getValueType();
            }
        }
        throw new IllegalArgumentException("Could not find symbol for name " + quote(name));
    }

    public boolean isDefined(String name) {
        return localSymbols.containsKey(name) || forwardSymbols.containsKey(name);
    }

    public boolean isForwardReference(String name) {
        return forwardSymbols.containsKey(name);
    }

    public boolean isLocal(String name) {
        return localSymbols.containsKey(name);
    }

    public void pruneReferences() {
        forwardSymbols.keySet().stream()
            .filter(localSymbols::containsKey)
            .collect(toList())
            .forEach(forwardSymbols::remove);
        reference.ifPresent(ref -> ref.accept(new DefinitionReferenceVisitor<Void>() {
            @Override
            public Void visit(ValueReference reference) {
                forwardSymbols.remove(reference.getName());
                return null;
            }

            @Override
            public Void visitOtherwise(DefinitionReference reference) {
                return null;
            }
        }));
    }

    public String qualify(String name, SymbolResolver resolver) {
        if (isLocal(name)) {
            return getReference().accept(new DefinitionReferenceVisitor<String>() {
                @Override
                public String visit(ModuleReference reference) {
                    return normalizeQualified(reference.getName(), name);
                }

                @Override
                public String visitOtherwise(DefinitionReference reference) {
                    return name;
                }
            });
        } else {
            return imports.stream()
                .map(i -> i.qualify(name, resolver))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get)
                .orElseGet(() -> {
                    Optional<String> optionalName = Optional.empty();
                    if (isForwardReference(name)) {
                        optionalName = optionalParent.map(parent -> parent.qualify(name, resolver));
                    }
                    return optionalName.orElseThrow(() -> new SymbolNotFoundException(undefinedSymbol(name)));
                });
        }
    }

    public void setImports(List<Import> imports) {
        this.imports.addAll(imports);
        reverse(this.imports);
    }

    public void setReference(DefinitionReference reference) {
        this.reference = of(reference);
    }

    public void undefine(String name) {
        localSymbols.remove(name);
    }

    private Symbol getSymbol(String name) {
        if (isDefined(name)) {
            if (localSymbols.containsKey(name)) {
                return localSymbols.get(name);
            } else if (forwardSymbols.containsKey(name)) {
                return forwardSymbols.get(name);
            }
        }
        if (optionalParent.isPresent()) {
            return optionalParent.get().getSymbol(name);
        } else {
            throw new IllegalArgumentException("Could not find symbol for name " + quote(name));
        }
    }

    private String undefinedSymbol(String name) {
        return "Reference to undefined symbol " + quote(name);
    }
}
