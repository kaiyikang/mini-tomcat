package com.kaiyikang.minitomcat.utils;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.net.httpserver.Headers;

public class HttpUtils {

    static final Pattern QUERY_SPLIT = Pattern.compile("\\&");

    public static Map<String, List<String>> parseQuery(String query, Charset charSet) {

        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        // "key=value&key1=value1"
        String[] ss = QUERY_SPLIT.split(query);
        // Regex: String[] ss = Pattern.compile("\\&").split(query);
        Map<String, List<String>> map = new HashMap<>();
        for (String s : ss) {
            int n = s.indexOf('=');
            if (n >= 1) {
                String key = s.substring(0, n);
                String val = s.substring(n + 1);
                List<String> exist = map.get(key);
                if (exist == null) {
                    exist = new ArrayList<>(4);
                    map.put(key, exist);
                }
                exist.add(URLDecoder.decode(val, charSet));

            }
        }
        return map;
    }

    public static Map<String, List<String>> parseQuery(String query) {
        return parseQuery(query, StandardCharsets.UTF_8);
    }

    public static String getHeader(Headers headers, String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

}
