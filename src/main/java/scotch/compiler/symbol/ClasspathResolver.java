package scotch.compiler.symbol;

import static java.util.regex.Pattern.compile;
import static scotch.compiler.symbol.Symbol.getPackageName;
import static scotch.compiler.symbol.Symbol.getPackagePath;
import static scotch.compiler.util.Pair.pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.exception.SymbolResolutionError;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.util.Pair;

public class ClasspathResolver implements SymbolResolver {

    private final ClassLoader                                                classLoader;
    private final Map<Symbol, SymbolEntry>                                   namedSymbols;
    private final Set<String>                                                searchedModules;
    private final Map<Pair<Symbol, List<Type>>, Set<TypeInstanceDescriptor>> typeInstances;
    private final Map<Symbol, Set<TypeInstanceDescriptor>>                   typeInstancesByClass;
    private final Map<List<Type>, Set<TypeInstanceDescriptor>>               typeInstancesByArguments;
    private final Map<String, Set<TypeInstanceDescriptor>>                   typeInstancesByModule;

    public ClasspathResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.namedSymbols = new HashMap<>();
        this.searchedModules = new HashSet<>();
        this.typeInstances = new HashMap<>();
        this.typeInstancesByClass = new HashMap<>();
        this.typeInstancesByArguments = new HashMap<>();
        this.typeInstancesByModule = new HashMap<>();
    }

    @Override
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        search(symbol);
        return Optional.ofNullable(namedSymbols.get(symbol));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> arguments) {
        search(symbol);
        search(arguments);
        return typeInstances.getOrDefault(pair(symbol, arguments), ImmutableSet.of());
    }

    public Set<TypeInstanceDescriptor> getTypeInstancesByArguments(List<Type> arguments) {
        search(arguments);
        return ImmutableSet.copyOf(typeInstancesByArguments.getOrDefault(arguments, ImmutableSet.of()));
    }

    public Set<TypeInstanceDescriptor> getTypeInstancesByClass(Symbol symbol) {
        search(symbol);
        return ImmutableSet.copyOf(typeInstancesByClass.getOrDefault(symbol, ImmutableSet.of()));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName) {
        search(moduleName);
        return ImmutableSet.copyOf(typeInstancesByModule.getOrDefault(moduleName, ImmutableSet.of()));
    }

    private String baseName(File file) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private File[] classFiles(File directory) {
        File[] files = directory.listFiles(pathName -> pathName.isFile() && pathName.getName().endsWith(".class"));
        return files == null ? new File[0] : files;
    }

    private Optional<Class<?>> resolveClass(String className) {
        try {
            return Optional.of(Class.forName(className, true, classLoader));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    private List<Class<?>> resolveClasses(URL resource, String packagePath) {
        List<Class<?>> classes = new ArrayList<>();
        try (ZipInputStream zipStream = new ZipInputStream(resource.openStream())) {
            ZipEntry entry;
            while (null != (entry = zipStream.getNextEntry())) {
                try {
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        Pattern pattern = compile("(" + packagePath + "/[^\\./]+)\\.class");
                        Matcher matcher = pattern.matcher(name);
                        if (matcher.find()) {
                            resolveClass(matcher.group(1).replace('/', '.')).ifPresent(classes::add);
                        }
                    }
                } finally {
                    zipStream.closeEntry();
                }
            }
        } catch (IOException exception) {
            throw new SymbolResolutionError(exception);
        }
        return classes;
    }

    private List<Class<?>> resolveClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (directory.exists()) {
            for (File file : classFiles(directory)) {
                resolveClass(packageName + '.' + baseName(file)).map(classes::add);
            }
        }
        return classes;
    }

    private void search(List<Type> arguments) {
        arguments.forEach(argument -> argument.getContext().forEach(this::search));
    }

    private void search(Symbol symbol) {
        symbol.accept(new SymbolVisitor<Void>() {
            @Override
            public Void visit(QualifiedSymbol symbol) {
                search(symbol.getModuleName());
                return null;
            }

            @Override
            public Void visit(UnqualifiedSymbol symbol) {
                return null;
            }
        });
    }

    private void search(String moduleName) {
        if (!searchedModules.contains(moduleName)) {
            searchedModules.add(moduleName);
            List<Class<?>> classes = new ArrayList<>();
            try {
                Enumeration<URL> resources = classLoader.getResources(moduleName.replace('.', '/'));
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if (resource.getFile().contains("!")) {
                        String path = new File(resource.getFile()).getPath();
                        classes.addAll(resolveClasses(new URL(path.substring(0, path.indexOf('!'))), getPackagePath(moduleName)));
                    } else {
                        classes.addAll(resolveClasses(new File(resource.getFile()), getPackageName(moduleName)));
                    }
                }
            } catch (IOException exception) {
                throw new SymbolResolutionError(exception);
            }
            new ModuleScanner(moduleName, classes).scan().into((entries, instances) -> {
                entries.forEach(entry -> namedSymbols.put(entry.getSymbol(), entry));
                instances.forEach(instance -> {
                    typeInstances.computeIfAbsent(pair(instance.getTypeClass(), instance.getParameters()), k -> new HashSet<>()).add(instance);
                    typeInstancesByClass.computeIfAbsent(instance.getTypeClass(), k -> new HashSet<>()).add(instance);
                    typeInstancesByArguments.computeIfAbsent(instance.getParameters(), k -> new HashSet<>()).add(instance);
                    typeInstancesByModule.computeIfAbsent(moduleName, k -> new HashSet<>()).add(instance);
                });
                return null;
            });
        }
    }
}
