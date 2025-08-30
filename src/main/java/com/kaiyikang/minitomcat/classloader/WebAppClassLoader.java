package com.kaiyikang.minitomcat.classloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class WebAppClassLoader extends URLClassLoader {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final Path classPath;
    final Path[] libJars;

    public WebAppClassLoader(Path classPath, Path libPath) throws IOException {
        super("WebAppClassLoader", createUrls(classPath, libPath), ClassLoader.getSystemClassLoader());

        this.classPath = classPath.toAbsolutePath().normalize();
        this.libJars = Files.list(libPath).filter(p -> p.toString().endsWith(".jar"))
                .map(p -> p.toAbsolutePath().normalize()).sorted().toArray(Path[]::new);

        logger.info("set classes path: {}", this.classPath);
        Arrays.stream(this.libJars).forEach(p -> {
            logger.info("set jar path: {}", p);
        });
    }

    // Class
    public void scanClassPath(Consumer<Resource> handler) {
        scanClassPath0(handler, this.classPath, this.classPath);
    }

    /*
     * The active scanning mechanism traverses the classpath and JAR packages of the
     * web application to locate all .class files.
     */
    private void scanClassPath0(Consumer<Resource> handler, Path basePath, Path path) {
        try {
            Files.list(path)
                    .sorted()
                    .forEach(p -> {
                        if (Files.isDirectory(p)) {
                            scanClassPath0(handler, basePath, p);
                        } else if (Files.isRegularFile(p)) {
                            Path subPath = basePath.relativize(p);
                            handler.accept(new Resource(p, subPath.toString().replace('\\', '/')));
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Jar
    public void scanJar(Consumer<Resource> handler) {
        try {
            for (Path jarPath : this.libJars) {
                scanJar0(handler, jarPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void scanJar0(Consumer<Resource> handler, Path jarPath) throws IOException {
        JarFile jarFile = new JarFile(jarPath.toFile());
        jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .forEach(entry -> {
                    String name = entry.getName();
                    handler.accept(new Resource(jarPath, name));
                });
        jarFile.close();
    }

    /*
     * It converts the classPath (typically the path to the WEB-INF/classes
     * directory) into a URL.
     * Converts all JAR files under the libPath (typically the path to the
     * WEB-INF/lib directory) into URLs.
     * Collects these URLs into a URL[] array.
     */
    static URL[] createUrls(Path classPath, Path libPath) throws IOException {
        List<URL> urls = new ArrayList<>();
        urls.add(toDirURL(classPath));
        Files.list(libPath).filter(p -> p.toString().endsWith(".jar"))
                .sorted()
                .forEach(p -> {
                    urls.add(toJarURL(p));
                });
        return urls.toArray(URL[]::new);
    }

    static URL toDirURL(Path p) {
        try {
            if (Files.isDirectory(p)) {
                String abs = toAbsPath(p);
                if (!abs.endsWith("/")) {
                    abs = abs + "/";
                }
                return URI.create("file://" + abs).toURL();
            }
            throw new IOException("Path is not a directory: " + p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static URL toJarURL(Path p) {
        try {
            if (Files.isRegularFile(p)) {
                String abs = toAbsPath(p);
                return URI.create("file://" + abs).toURL();
            }
            throw new IOException("Path is not a jar file:" + p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String toAbsPath(Path p) throws IOException {
        String abs = p.toAbsolutePath().normalize().toString().replace('\\', '/');
        return abs;
    }

}
