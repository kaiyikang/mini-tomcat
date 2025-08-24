package com.kaiyikang.minitomcat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import javax.management.RuntimeErrorException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.kaiyikang.minitomcat.classloader.Resource;
import com.kaiyikang.minitomcat.classloader.WebAppClassLoader;
import com.kaiyikang.minitomcat.connector.HttpConnector;
import com.kaiyikang.minitomcat.utils.ClassPathUtils;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

public class Start {

    static Logger logger = org.slf4j.LoggerFactory.getLogger(Start.class);

    public static void main(String[] args) throws Exception {
        // Parser the var
        String warFile = null;
        String customConfigPath = null;
        Options options = new Options();
        options.addOption(Option.builder("w").longOpt("war").argName("file").hasArg().desc("specify war file.")
                .required().build());
        options.addOption(Option.builder("c").longOpt("config").argName("file").hasArg()
                .desc("specify external configuration file.").build());
        try {
            var parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            warFile = cmd.getOptionValue(
                    "war");
            customConfigPath = cmd.getOptionValue("config");

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            var help = new HelpFormatter();
            var jarName = Path.of(Start.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName()
                    .toString();
            help.printHelp("java -jar " + jarName + " [options]", options);
            System.exit(1);
            return;
        }

        new Start().start(warFile, customConfigPath);
    }

    private void start(String warFile, String customConfigPath) throws IOException {
        Path warPath = parseWarFile(warFile);

        Path[] ps = extractWarIfNecessary(warPath);// { classesPath, libPath }
        String webRoot = ps[0].getParent().toString();
        logger.info("set web root: {}", webRoot);

        // Load Default- and Custom Configs
        String defaultConfigYaml = ClassPathUtils.readString("/server.yml");
        String customConfigYaml = null;
        if (customConfigPath != null) {
            logger.info("load external config {}...", customConfigPath);
            try {
                customConfigYaml = Files.readString(Paths.get(customConfigPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Could not read config: " + customConfigPath, e);
                System.exit(1);
                return;
            }
        }

        // Setup Configs
        Config config;
        Config customConfig;
        try {
            config = loadConfig(defaultConfigYaml);
        } catch (JacksonException e) {
            logger.error("parse default config failed.", e);
            throw new RuntimeException(e);
        }
        if (customConfigYaml != null) {
            try {
                customConfig = loadConfig(customConfigYaml);
            } catch (JacksonException e) {
                logger.error("Parse custom config failed:" + customConfigPath, e);
                throw new RuntimeException(e);
            }

            try {
                merge(config, customConfig);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        // Set classLoader
        var classLoader = new WebAppClassLoader(ps[0], ps[1]);
        Set<Class<?>> classSet = new HashSet<>();

        // Scan and load classes
        Consumer<Resource> handler = (r) -> {
            if (r.name().endsWith(".class")) {
                // Scan classes
                String className = r.name().substring(0, r.name().length() - 6).replace('/', '.');
                if (className.endsWith("module-info") || className.endsWith("package-info")) {
                    return;
                }
                Class<?> clazz;
                try {
                    clazz = classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    logger.warn("load class '{} failed: {}: {}'", className, e.getClass().getSimpleName(),
                            e.getMessage());
                    return;
                } catch (NoClassDefFoundError err) {
                    logger.error("load class '{}' failed: {}: {}", className, err.getClass().getSimpleName(),
                            err.getMessage());
                    return;
                }
                // Load classes
                if (clazz.isAnnotationPresent(WebServlet.class)) {
                    logger.info("Found @WebServlet: {}", clazz.getName());
                    classSet.add(clazz);
                }
                if (clazz.isAnnotationPresent(WebFilter.class)) {
                    logger.info("Found @WebFilter: {}", clazz.getName());
                    classSet.add(clazz);
                }
                if (clazz.isAnnotationPresent(WebListener.class)) {
                    logger.info("Found @WebFilter: {}", clazz.getName());
                    classSet.add(clazz);
                }
            }
        };

        classLoader.scanClassPath(handler);
        classLoader.scanJar(handler);
        List<Class<?>> autoScannedClasses = new ArrayList<>(classSet);

        // Create a executor to execute the class
        if (config.server().enableVirtualThread()) {
            logger.info("Virtual thread is enabled.");
        }
        ExecutorService executor = config.server().enableVirtualThread() ? Executors.newVirtualThreadPerTaskExecutor()
                : new ThreadPoolExecutor(0, config.server().threadPoolSize(), 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingDeque<>());

        // Start the server
        try (HttpConnector connector = new HttpConnector(config, webRoot, executor, classLoader, autoScannedClasses)) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("mini-tomcat http server was shutdown.");
    }

    private Path[] extractWarIfNecessary(Path warPath) throws IOException {
        if (Files.isDirectory(warPath)) {
            // TODO
            logger.info("war is directory: {},", warPath);
            Path classesPath = warPath.resolve("WEB-INF/classes");
            Path libPath = warPath.resolve("WEB-INF/lib");
            Files.createDirectories(classesPath);
            Files.createDirectories(libPath);
            return new Path[] { classesPath, libPath };
        }

        Path extractPath = createExtractTo();
        logger.info("extract '{}' to '{}'", warPath, extractPath);
        try (JarFile war = new JarFile(warPath.toFile())) {
            war.stream().sorted((e1, e2) -> e1.getName().compareTo(e2.getName())).forEach(currentFile -> {
                if (!currentFile.isDirectory()) {
                    Path targetFile = extractPath.resolve(currentFile.getName());
                    Path targetDir = targetFile.getParent();
                    if (!Files.isDirectory(targetDir)) {
                        try {
                            Files.createDirectories(targetDir);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    try (InputStream currentInputStream = war.getInputStream(currentFile)) {
                        Files.copy(currentInputStream, targetFile);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

        Path classesPath = extractPath.resolve("WEB-INF/classes");
        Path libPath = extractPath.resolve("WEB-INF/lib");
        Files.createDirectories(classesPath);
        Files.createDirectories(libPath);
        return new Path[] { classesPath, libPath };

    }

    private Path parseWarFile(String warFile) {
        Path warPath = Path.of(warFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(warPath) && !Files.isDirectory(warPath)) {
            System.err.printf("war file '%s' was not found.\n", warFile);
            System.exit(1);
        }
        return warPath;
    }

    private Path createExtractTo() throws IOException {
        Path tmp = Files.createTempDirectory("_mt_");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deleteDir(tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return tmp;
    }

    private void deleteDir(Path p) throws IOException {
        Files.list(p).forEach(c -> {
            try {
                if (Files.isDirectory(c)) {
                    deleteDir(c);
                } else {
                    Files.delete(c);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Files.delete(p);
    }

    private Config loadConfig(String config) throws JacksonException {
        var objectMapper = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(config, Config.class);
    }

    static void merge(Object source, Object override) throws ReflectiveOperationException {
        for (Field field : source.getClass().getFields()) {
            Object overrideFiledValue = field.get(override);
            if (overrideFiledValue != null) {
                Class<?> type = field.getType();
                if (type == String.class || type.isPrimitive() || Number.class.isAssignableFrom(type)) {
                    field.set(source, overrideFiledValue);
                } else if (Map.class.isAssignableFrom(type)) {
                    Map<String, String> sourceMap = (Map<String, String>) field.get(source);
                    Map<String, String> overrideMap = (Map<String, String>) overrideFiledValue;
                    sourceMap.putAll(overrideMap);
                } else {
                    merge(field.get(source), overrideFiledValue);
                }
            }
        }
    }

}
