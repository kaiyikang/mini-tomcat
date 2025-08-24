package com.kaiyikang.minitomcat.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ClassPathUtilsTest {

    private static final String TEST_RESOURCE_PATH = "/test_resource_in_test_classpath.txt";
    private static final String TEST_RESOURCE_CONTENT = "Hello from classpath!\nThis is a test resource.\n";

    @Test
    void testReadBytes() {
        try {
            byte[] bytes = ClassPathUtils.readBytes(TEST_RESOURCE_PATH);
            assertNotNull(bytes, "Returned bytes array should not be null");
            assertTrue(bytes.length > 0, "Returned bytes array should not be empty");

            String content = new String(bytes, StandardCharsets.UTF_8);
            assertEquals(TEST_RESOURCE_CONTENT, content);

        } catch (UncheckedIOException e) {
            fail("UncheckedIOException should not be thrown for existing resource: " + e.getMessage());
        } catch (Exception e) {
            fail("An unexpected exception occurred: " + e.getMessage());

        }
    }

    @Test
    void testReadString() {
        try {
            String content = ClassPathUtils.readString(TEST_RESOURCE_PATH);
            assertNotNull(content, "Returned string should not be null");
            assertFalse(content.isEmpty(), "Returned string should not be empty");

            assertEquals(TEST_RESOURCE_CONTENT, content);

        } catch (UncheckedIOException e) {
            fail("FileNotFoundException should not be thrown for existing resource: " + e.getMessage());
        } catch (Exception e) {
            fail("An unexpected exception occurred: " + e.getMessage());
        }
    }

}
