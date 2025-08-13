package com.kaiyikang.minitomcat.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kaiyikang.minitomcat.engine.support.InitParameters;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.FilterRegistration.Dynamic;

public class FilterRegistrationImp implements Dynamic {

    final Filter filter;
    final String name;
    final ServletContext servletContext;

    final InitParameters initParameters = new InitParameters();
    final List<String> urlPatterns = new ArrayList<>(4);
    boolean initialized = false;

    public FilterRegistrationImp(ServletContext servletContext, String name, Filter filter) {
        this.servletContext = servletContext;
        this.name = name;
        this.filter = filter;
    }

    public FilterConfig getFilterConfig() {
        return new FilterConfig() {

            @Override
            public String getFilterName() {
                return FilterRegistrationImp.this.name;
            }

            @Override
            public ServletContext getServletContext() {
                return FilterRegistrationImp.this.servletContext;
            }

            @Override
            public String getInitParameter(String name) {
                return FilterRegistrationImp.this.initParameters.getInitParamer(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return FilterRegistrationImp.this.initParameters.getInitParameterNames();
            }

        };
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... servletNames) {
        throw new UnsupportedOperationException("Unimplemented method 'addMappingForServletNames'");
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return List.of();
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
            String... urlPatterns) {
        checkNotInitialized("addMappingForUrlPatterns");
        if (!dispatcherTypes.contains(DispatcherType.REQUEST) || dispatcherTypes.size() != 1) {
            throw new IllegalArgumentException("Only support DispatcherType.REQUEST.");
        }
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("MissingPatterns");
        }
        for (String urlPattern : urlPatterns) {
            this.urlPatterns.add(urlPattern);
        }
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return this.urlPatterns;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        checkNotInitialized("setInitParameter");
        return this.initParameters.setInitParameter(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        return this.initParameters.getInitParamer(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        checkNotInitialized("setInitParameter");
        return this.initParameters.setInitParameters(initParameters);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return this.initParameters.getInitParameters();
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        checkNotInitialized("setInitParameter");
        if (isAsyncSupported) {
            throw new UnsupportedOperationException("Async is not supported.");
        }
    }

    private void checkNotInitialized(String name) {
        if (this.initialized) {
            throw new IllegalArgumentException("Cannot call" + name + " after initialization.");
        }
    }
}
