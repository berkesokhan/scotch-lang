package scotch.compiler.symbol;

import static java.util.Arrays.stream;
import static java.util.regex.Pattern.compile;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Value.Fixity.NONE;

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
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.SymbolEntry.ImmutableEntryBuilder;

public class ClasspathResolver implements SymbolResolver {

    private final ClassLoader              classLoader;
    private final Map<Symbol, SymbolEntry> entries;
    private final Set<String>              searchedModules;

    public ClasspathResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.entries = new HashMap<>();
        this.searchedModules = new HashSet<>();
    }

    @Override
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        search(symbol);
        return Optional.ofNullable(entries.get(symbol));
    }

    private String baseName(File file) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private File[] classFiles(File directory) {
        File[] files = directory.listFiles(pathName -> pathName.isFile() && pathName.getName().endsWith(".class"));
        return files == null ? new File[0] : files;
    }

    private void processClasses(String moduleName, List<Class<?>> classes) {
        Map<Symbol, ImmutableEntryBuilder> builders = new HashMap<>();
        classes.forEach(clazz -> processValues(moduleName, clazz, builders));
        builders.forEach((symbol, builder) -> entries.put(symbol, builder.build()));
    }

    private void processValues(String moduleName, Class<?> clazz, Map<Symbol, ImmutableEntryBuilder> builders) {
        stream(clazz.getDeclaredMethods()).forEach(method -> {
            Optional.ofNullable(method.getAnnotation(Value.class)).ifPresent(value -> {
                Symbol symbol = qualified(moduleName, value.memberName());
                ImmutableEntryBuilder builder = builders.computeIfAbsent(symbol, SymbolEntry::immutableEntry);
                builder.withValueSignature(new JavaSignature(p(clazz), method.getName(), sig(method.getReturnType())));
                if (value.fixity() != NONE && value.precedence() != -1) {
                    builder.withOperator(operator(value.fixity(), value.precedence()));
                }
            });
            Optional.ofNullable(method.getAnnotation(ValueType.class)).ifPresent(valueType -> {
                Symbol symbol = qualified(moduleName, valueType.forMember());
                ImmutableEntryBuilder builder = builders.computeIfAbsent(symbol, SymbolEntry::immutableEntry);
                try {
                    builder.withValue((Type) method.invoke(null));
                } catch (ReflectiveOperationException exception) {
                    // TODO
                }
            });
        });
    }

    private Optional<Class<?>> resolveClass(String className) {
        try {
            return Optional.of(Class.forName(className, true, classLoader));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    private List<Class<?>> resolveClasses(URL resource, String moduleName) {
        List<Class<?>> classes = new ArrayList<>();
        try (ZipInputStream zipStream = new ZipInputStream(resource.openStream())) {
            ZipEntry entry;
            while (null != (entry = zipStream.getNextEntry())) {
                try {
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        Pattern pattern = compile("(" + moduleName.replace('.', '/') + "/[^\\./]+)\\.class");
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
            exception.printStackTrace(); // TODO
        }
        return classes;
    }

    private List<Class<?>> resolveClasses(File directory, String moduleName) {
        List<Class<?>> classes = new ArrayList<>();
        if (directory.exists()) {
            for (File file : classFiles(directory)) {
                resolveClass(moduleName + '.' + baseName(file)).map(classes::add);
            }
        }
        return classes;
    }

    private void search(Symbol symbol) {
        symbol.accept(new SymbolVisitor<Void>() {
            @Override
            public Void visit(QualifiedSymbol symbol) {
                if (!searchedModules.contains(symbol.getModuleName())) {
                    searchedModules.add(symbol.getModuleName());
                    List<Class<?>> classes = new ArrayList<>();
                    try {
                        Enumeration<URL> resources = classLoader.getResources(symbol.getModuleName().replace('.', '/'));
                        while (resources.hasMoreElements()) {
                            URL resource = resources.nextElement();
                            if (resource.getFile().contains("!")) {
                                String path = new File(resource.getFile()).getPath();
                                classes.addAll(resolveClasses(new URL(path.substring(0, path.indexOf('!'))), symbol.getModuleName()));
                            } else {
                                classes.addAll(resolveClasses(new File(resource.getFile()), symbol.getModuleName()));
                            }
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace(); // TODO
                    }
                    processClasses(symbol.getModuleName(), classes);
                }
                return null;
            }

            @Override
            public Void visit(UnqualifiedSymbol symbol) {
                return null;
            }
        });
    }
}
