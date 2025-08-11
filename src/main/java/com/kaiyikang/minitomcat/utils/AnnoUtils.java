package com.kaiyikang.minitomcat.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.Servlet;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;

public class AnnoUtils {
    public static String getServletName(Class<? extends Servlet> servletClass) {
        WebServlet w = servletClass.getAnnotation(WebServlet.class);
        if (w != null && !w.name().isEmpty()) {
            return w.name();
        }
        return defaultNameByClass(servletClass);
    }

    public static Map<String, String> getServletInitParams(Class<? extends Servlet> servletClass) {
        WebServlet w = servletClass.getAnnotation(WebServlet.class);
        if (w == null) {
            return Map.of();
        }
        return initParamsToMap(w.initParams());
    }

    public static String[] getServletUrlPatterns(Class<? extends Servlet> servletClass) {
        WebServlet w = servletClass.getAnnotation(WebServlet.class);
        if (w == null) {
            return new String[0];
        }
        return arraysToSet(w.value(), w.urlPatterns()).toArray(String[]::new);
    }

    private static String defaultNameByClass(Class<? extends Servlet> servletClass) {
        String name = servletClass.getSimpleName();
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        return name;
    }

    private static Map<String, String> initParamsToMap(WebInitParam[] params) {
        return Arrays.stream(params).collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    }

    private static Set<String> arraysToSet(String[] arr) {
        Set<String> set = new LinkedHashSet<>();
        for (String s : arr) {
            set.add(s);
        }
        return set;
    }

    private static Set<String> arraysToSet(String[] arr1, String[] arr2) {
        Set<String> set = arraysToSet(arr1);
        set.addAll(arraysToSet(arr2));
        return set;
    }
}
