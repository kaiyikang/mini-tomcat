package com.kaiyikang.minitomcat.utils;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;

public class ClassPathUtils {

    static final Logger logger = LoggerFactory.getLogger(ClassPathUtils.class);

    public static byte[] readBytes(String path) {
        try (InputStream input = ClassPathUtils.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String readString(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }
}
