package com.kaiyikang.minitomcat.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;

public class ServletRegistrationImpl implements ServletRegistration.Dynamic {

    final ServletContext servletContext;
    final String name;
    final Servlet servlet;
    final List<String> urlPatterns = new ArrayList<>(4);

    boolean initialized = false;

    public ServletRegistrationImpl(ServletContext servletContext, String name, Servlet servlet) {
        this.servletContext = servletContext;
        this.name = name;
        this.servlet = servlet;
    }

    public ServletConfig getServletConfig() {
        return new ServletConfig() {

            @Override
            public String getServletName() {
                return null;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String name) {
                return null;
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return null;
            }

        };
    }

    // ===== Not Implemented Yet =====

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addMapping'");
    }

    @Override
    public Collection<String> getMappings() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMappings'");
    }

    @Override
    public String getRunAsRole() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRunAsRole'");
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public String getClassName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClassName'");
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInitParameter'");
    }

    @Override
    public String getInitParameter(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInitParameter'");
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInitParameters'");
    }

    @Override
    public Map<String, String> getInitParameters() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInitParameters'");
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAsyncSupported'");
    }

    @Override
    public void setLoadOnStartup(int loadOnStartup) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLoadOnStartup'");
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setServletSecurity'");
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMultipartConfig'");
    }

    @Override
    public void setRunAsRole(String roleName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRunAsRole'");
    }
}
