package functions.meta;

import functions.CompositeFunction;
import functions.MathFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class that scans the {@code functions} package for {@link SimpleFunction} implementations.
 */
public final class SimpleFunctionScanner {
    private static final Logger logger = LoggerFactory.getLogger(SimpleFunctionScanner.class);
    private static final String ROOT_PACKAGE = "functions";

    private SimpleFunctionScanner() {
    }

    public static List<SimpleFunctionDefinition> scan() {
        List<Class<?>> candidates = new ArrayList<>();
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(ROOT_PACKAGE.replace('.', '/'));
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    findClassesInDirectory(new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8)), ROOT_PACKAGE, candidates);
                } else if ("jar".equals(protocol)) {
                    findClassesInJar(url, candidates);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to scan package {}", ROOT_PACKAGE, e);
        }

        List<SimpleFunctionDefinition> functions = new ArrayList<>();
        for (Class<?> candidate : candidates) {
            SimpleFunction annotation = candidate.getAnnotation(SimpleFunction.class);
            if (annotation == null || !MathFunction.class.isAssignableFrom(candidate)) {
                continue;
            }
            try {
                MathFunction instance = (MathFunction) candidate.getDeclaredConstructor().newInstance();
                String localized = selectLocalizedName(annotation.names());
                int priority = annotation.priority();
                functions.add(new SimpleFunctionDefinition(candidate.getName(), localized, priority, instance));
            } catch (ReflectiveOperationException e) {
                logger.warn("Cannot instantiate function {}", candidate.getName(), e);
            }
        }
        Collections.sort(functions);
        return functions;
    }

    private static void findClassesInDirectory(File directory, String packageName, List<Class<?>> output) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), output);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                loadClass(className, output);
            }
        }
    }

    private static void findClassesInJar(URL resource, List<Class<?>> output) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(ROOT_PACKAGE.replace('.', '/')) && name.endsWith(".class")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        loadClass(className, output);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to read jar for package scan", e);
        }
    }

    private static void loadClass(String className, List<Class<?>> output) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isInterface() && !clazz.isEnum() && !clazz.equals(CompositeFunction.class)) {
                output.add(clazz);
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static String selectLocalizedName(LocalizedName[] names) {
        String russian = null;
        for (LocalizedName name : names) {
            if (Objects.equals("ru", name.locale())) {
                russian = name.value();
                break;
            }
        }
        if (russian != null) {
            return russian;
        }
        return names.length > 0 ? names[0].value() : "Функция";
    }

    public record SimpleFunctionDefinition(String key, String title, int priority, MathFunction function)
            implements Comparable<SimpleFunctionDefinition> {
        @Override
        public int compareTo(SimpleFunctionDefinition other) {
            int priorityCompare = Integer.compare(other.priority, priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return title.compareToIgnoreCase(other.title);
        }
    }
}