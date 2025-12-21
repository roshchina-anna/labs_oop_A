package ui;

import functions.CompositeFunction;
import functions.MathFunction;
import functions.meta.LocalizedName;
import functions.meta.SimpleFunction;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SimpleFunctionRegistry {
    private record FunctionEntry(MathFunction function, int priority, Map<Locale, String> localizedNames) {
        boolean matchesName(String candidate) {
            return localizedNames.values().stream().anyMatch(candidate::equals);
        }
    }

    private final List<FunctionEntry> entries = new ArrayList<>();
    private final AtomicInteger customCounter = new AtomicInteger();

    public SimpleFunctionRegistry() {
        discover("functions");
    }
    public Map<String, MathFunction> getFunctions() {
        return getFunctions(Locale.getDefault());
    }

    public Map<String, MathFunction> getFunctions(Locale locale) {
        Locale target = locale != null ? locale : Locale.getDefault();
        return entries.stream()
                .sorted(Comparator.comparingInt(FunctionEntry::priority).reversed()
                        .thenComparing(entry -> resolveName(entry, target)))
                .collect(Collectors.toMap(
                        entry -> resolveName(entry, target),
                        FunctionEntry::function,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public MathFunction getByName(String name) {
        return entries.stream()
                .filter(entry -> entry.matchesName(name))
                .findFirst()
                .map(FunctionEntry::function)
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная функция: " + name));
    }

    public void registerComposite(String displayName, MathFunction first, MathFunction second, Locale locale, int priority) {
        Objects.requireNonNull(first, "Первая функция не задана");
        Objects.requireNonNull(second, "Вторая функция не задана");
        Locale target = locale != null ? locale : Locale.getDefault();
        Map<Locale, String> names = new LinkedHashMap<>();
        names.put(target, displayName != null && !displayName.isBlank()
                ? displayName
                : "Составная функция " + customCounter.incrementAndGet());
        entries.add(new FunctionEntry(new CompositeFunction(first, second), priority, names));
    }

    private void discover(String basePackage) {
        for (Class<?> candidate : findClasses(basePackage)) {
            SimpleFunction annotation = candidate.getAnnotation(SimpleFunction.class);
            if (annotation == null || !MathFunction.class.isAssignableFrom(candidate)) {
                continue;
            }
            try {
                MathFunction function = (MathFunction) candidate.getDeclaredConstructor().newInstance();
                Map<Locale, String> names = new LinkedHashMap<>();
                for (LocalizedName localizedName : annotation.names()) {
                    names.put(Locale.forLanguageTag(localizedName.locale()), localizedName.value());
                }
                entries.add(new FunctionEntry(function, annotation.priority(), names));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Не удалось создать функцию " + candidate.getName(), e);
            }
        }
    }

    private List<Class<?>> findClasses(String basePackage) {
        String path = basePackage.replace('.', '/');
        List<Class<?>> result = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> result.addAll(loadFromDirectory(url.toURI(), basePackage));
                    case "jar" -> result.addAll(loadFromJar(url, basePackage));
                    default -> {}
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Не удалось просканировать пакет функций", e);
        }
        return result;
    }

    private List<Class<?>> loadFromDirectory(java.net.URI uri, String basePackage) {
        Path directory = Paths.get(uri);
        try (Stream<Path> stream = Files.walk(directory, 1)) {
            return stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .map(path -> toClassName(basePackage, directory.relativize(path)))
                    .map(this::tryLoadClass)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения директории функций", e);
        }
    }

    private List<Class<?>> loadFromJar(URL url, String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                String prefix = basePackage.replace('.', '/') + "/";
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.endsWith(".class") || !name.startsWith(prefix) || name.contains("$")) {
                        continue;
                    }
                    String relative = name.substring(prefix.length());
                    classes.add(Class.forName(basePackage + '.' + relative.replace('/', '.').replace(".class", "")));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать jar с функциями", e);
        }
        return classes;
    }

    private String toClassName(String basePackage, Path relativePath) {
        String fileName = relativePath.toString().replace('/', '.').replace('\\', '.');
        return basePackage + '.' + fileName.substring(0, fileName.length() - ".class".length());
    }

    private Optional<Class<?>> tryLoadClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private String resolveName(FunctionEntry entry, Locale locale) {
        Map<Locale, String> localized = entry.localizedNames();
        if (localized.containsKey(locale)) {
            return localized.get(locale);
        }
        for (Locale key : localized.keySet()) {
            if (Objects.equals(key.getLanguage(), locale.getLanguage())) {
                return localized.get(key);
            }
        }
        return localized.values().stream().findFirst().orElse("Функция");
    }
}