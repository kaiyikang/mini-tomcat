package com.kaiyikang.minitomcat.engine.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class AbstractMappingTest {

    @Test
    public void testBuildPattern() {
        AbstractMapping mapping1 = new AbstractMapping("example.com/path+file.txt");
        Pattern pattern1 = mapping1.pattern;
        assertEquals("^example\\.com\\/path\\+file\\.txt$", pattern1.pattern());

        AbstractMapping mapping2 = new AbstractMapping("*.example.com");
        Pattern pattern2 = mapping2.pattern;
        assertEquals("^*\\.example\\.com$", pattern2.pattern());

        AbstractMapping mapping3 = new AbstractMapping("/users/{userId}");
        Pattern pattern3 = mapping3.pattern;
        assertEquals("^\\/users\\/\\{userId\\}$", pattern3.pattern());

        AbstractMapping mapping4 = new AbstractMapping("/");
        Pattern pattern4 = mapping4.pattern;
        assertEquals("^\\/$", pattern4.pattern());

        AbstractMapping mapping5 = new AbstractMapping("index.html");
        Pattern pattern5 = mapping5.pattern;
        assertEquals("^index\\.html$", pattern5.pattern());
    }

    @Test
    void testMatches() {
        AbstractMapping mapping = new AbstractMapping("example.com/path+file.txt");
        assertTrue(mapping.matches("example.com/path+file.txt"));
        assertFalse(mapping.matches("exampleXcom/path-fileYtxt"));
        assertFalse(mapping.matches("example.com/path+file.txt/extra"));
    }

    @Test
    void testPriority() {
        AbstractMapping mapping1 = new AbstractMapping("/");
        assertEquals(Integer.MAX_VALUE, mapping1.priority());

        AbstractMapping mapping2 = new AbstractMapping("*");
        assertEquals(Integer.MAX_VALUE - 1, mapping2.priority());

        AbstractMapping mapping3 = new AbstractMapping("/about");
        assertEquals(100000 - "/about".length(), mapping3.priority());

        AbstractMapping mapping4 = new AbstractMapping("/products/electronics/cameras/digital-slr");
        assertEquals(100000 - "/products/electronics/cameras/digital-slr".length(), mapping4.priority());

        AbstractMapping mapping5 = new AbstractMapping("a");
        assertEquals(100000 - "a".length(), mapping5.priority());

        AbstractMapping mapping6 = new AbstractMapping("verylongurl");
        assertEquals(100000 - "verylongurl".length(), mapping6.priority());
    }
}
