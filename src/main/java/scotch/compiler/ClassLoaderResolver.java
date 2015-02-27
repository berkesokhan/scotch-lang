package scotch.compiler;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Symbol.getPackageName;
import static scotch.compiler.symbol.Symbol.getPackagePath;
import static scotch.compiler.symbol.Symbol.toJavaName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
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
import scotch.compiler.output.GeneratedClass;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.descriptor.TypeInstanceDescriptor;
import scotch.compiler.symbol.descriptor.TypeParameterDescriptor;
import scotch.compiler.symbol.exception.SymbolResolutionError;
import scotch.compiler.symbol.type.SumType;
import scotch.compiler.symbol.type.Type;

public class ClassLoaderResolver extends URLClassLoader implements SymbolResolver {

    public static ClassLoaderResolver resolver(Optional<File> optionalOutputPath) {
        return new ClassLoaderResolver(optionalOutputPath, ClassLoaderResolver.class.getClassLoader());
    }

    private final Optional<File>                                                               optionalOutputPath;
    private final Map<Symbol, SymbolEntry>                                                     namedSymbols;
    private final Set<String>                                                                  searchedClasses;
    private final Set<URL>                                                                     searchedUrls;
    private final Map<Symbol, Map<List<TypeParameterDescriptor>, Set<TypeInstanceDescriptor>>> typeInstances;
    private final Map<Symbol, Set<TypeInstanceDescriptor>>                                     typeInstancesByClass;
    private final Map<List<TypeParameterDescriptor>, Set<TypeInstanceDescriptor>>              typeInstancesByArguments;
    private final Map<String, Set<TypeInstanceDescriptor>>                                     typeInstancesByModule;
    private final Map<String, Set<Class<?>>>                                                   definedClasses;

    public ClassLoaderResolver(Optional<File> optionalOutputPath, ClassLoader parent) {
        this(optionalOutputPath, new URL[0], parent);
    }

    public ClassLoaderResolver(Optional<File> optionalOutputPath, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.optionalOutputPath = optionalOutputPath;
        this.namedSymbols = new HashMap<>();
        this.searchedClasses = new HashSet<>();
        this.searchedUrls = new HashSet<>();
        this.typeInstances = new HashMap<>();
        this.typeInstancesByClass = new HashMap<>();
        this.typeInstancesByArguments = new HashMap<>();
        this.typeInstancesByModule = new HashMap<>();
        this.definedClasses = new HashMap<>();
    }

    public Class<?> define(GeneratedClass generatedClass) {
        byte[] bytes = generatedClass.getBytes();
        optionalOutputPath.ifPresent(outputPath -> writeClass(generatedClass, bytes, outputPath));
        Class<?> clazz = defineClass(generatedClass.getClassName(), bytes, 0, bytes.length);
        definedClasses
            .computeIfAbsent(clazz.getName().replace(Pattern.quote("." + clazz.getSimpleName()) + "$", ""), k -> new HashSet<>())
            .add(clazz);
        return clazz;
    }

    public List<Class<?>> defineAll(List<GeneratedClass> generatedClasses) {
        return generatedClasses.stream()
            .map(this::define)
            .collect(toList());
    }

    @Override
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        search(symbol);
        return Optional.ofNullable(namedSymbols.get(symbol));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types) {
        search(symbol);
        search(types);
        return Optional.ofNullable(typeInstances.get(symbol))
            .flatMap(instances -> instances.keySet().stream()
                .filter(parameters -> parametersMatch(parameters, types))
                .map(instances::get)
                .findFirst())
            .orElse(ImmutableSet.of());
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName) {
        search(moduleName);
        return typeInstancesByModule.getOrDefault(moduleName, ImmutableSet.of());
    }

    private String baseName(File file) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private File[] classFiles(File directory) {
        File[] files = directory.listFiles(pathName -> pathName.isFile() && pathName.getName().endsWith(".class"));
        return files == null ? new File[0] : files;
    }

    private boolean parametersMatch(List<TypeParameterDescriptor> parameters, List<Type> types) {
        if (parameters.size() == types.size()) {
            for (int i = 0; i < parameters.size(); i++) {
                if (!(types.get(i) instanceof SumType) || !parameters.get(i).matches(types.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Optional<Class<?>> resolveClass(String className) {
        try {
            return Optional.of(loadClass(className));
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
                resolveClass(packageName + '.' + baseName(file)).ifPresent(classes::add);
            }
        }
        return classes;
    }

    private void search(List<Type> parameters) {
        parameters.forEach(parameter -> parameter.getContext().forEach(this::search));
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
        List<Class<?>> classes = new ArrayList<>();
        try {
            Enumeration<URL> resources = getResources(getPackagePath(moduleName));
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!searchedUrls.contains(resource)) {
                    searchedUrls.add(resource);
                    if (resource.getFile().contains("!")) {
                        String path = new File(resource.getFile()).getPath();
                        classes.addAll(resolveClasses(new URL(path.substring(0, path.indexOf('!'))), getPackagePath(moduleName)));
                    } else {
                        classes.addAll(resolveClasses(new File(resource.getFile()), getPackageName(moduleName)));
                    }
                }
            }
            Optional
                .ofNullable(definedClasses.get(toJavaName(moduleName)))
                .ifPresent(cs -> cs.forEach(classes::add));
        } catch (IOException exception) {
            throw new SymbolResolutionError(exception);
        }
        classes.removeIf(c -> searchedClasses.contains(c.getName()));
        classes.stream().map(Class::getName).forEach(searchedClasses::add);
        new ModuleScanner(moduleName, classes).scan().into((entries, instances) -> {
            entries.forEach(entry -> namedSymbols.put(entry.getSymbol(), entry));
            instances.forEach(typeInstance -> {
                typeInstances
                    .computeIfAbsent(typeInstance.getTypeClass(), k -> new HashMap<>())
                    .computeIfAbsent(typeInstance.getParameters(), k -> new HashSet<>())
                    .add(typeInstance);
                typeInstancesByClass.computeIfAbsent(typeInstance.getTypeClass(), k -> new HashSet<>()).add(typeInstance);
                typeInstancesByArguments.computeIfAbsent(typeInstance.getParameters(), k -> new HashSet<>()).add(typeInstance);
                typeInstancesByModule.computeIfAbsent(typeInstance.getModuleName(), k -> new HashSet<>()).add(typeInstance);
            });
            return null;
        });
    }

    private void writeClass(GeneratedClass generatedClass, byte[] bytes, File outputPath) {
        File file = new File(outputPath, generatedClass.getClassName().replace('.', '/') + ".class");
        if (!file.getParentFile().mkdirs() && !file.getParentFile().exists()) {
            throw new RuntimeException("Can't define " + generatedClass.getClassName()
                + ", directory " + file.getParentFile() + " could not be created");
        }
        try (OutputStream classFile = new FileOutputStream(file)) {
            classFile.write(bytes);
            classFile.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
