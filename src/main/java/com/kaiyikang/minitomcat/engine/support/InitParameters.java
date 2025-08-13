package com.kaiyikang.minitomcat.engine.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InitParameters extends LazyMap<String> {

    public boolean setInitParameter(String name, String value) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name is null or empty.");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Value is null or empty.");
        }
        if (super.containsKey(name)) {
            return false;
        }
        super.put(name, value);
        return true;
    }

    public String getInitParamer(String name) {
        return super.get(name);
    }

    public Set<String> setInitParameters(Map<String, String> initParameters) {
        if (initParameters == null) {
            throw new IllegalArgumentException("initParameters is null.");
        }
        if (initParameters.isEmpty()) {
            return Set.of();
        }

        Set<String> conflicts = new HashSet<>();
        for (String name : initParameters.keySet()) {
            String value = initParameters.get(name);
            if (value == null) {
                throw new IllegalArgumentException("initParameters contains null value by name: " + name);
            }
            if (super.containsKey(name)) {
                conflicts.add(name);
            } else {
                super.put(name, value);
            }
        }
        return conflicts;
    }

    public Map<String, String> getInitParameters() {
        return super.map();
    }

    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(super.map().keySet());
    }

}
