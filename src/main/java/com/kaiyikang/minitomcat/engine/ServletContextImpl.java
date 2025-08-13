package com.kaiyikang.minitomcat.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaiyikang.minitomcat.engine.mapping.ServletMapping;
import com.kaiyikang.minitomcat.engine.mapping.FilterMapping;

import com.kaiyikang.minitomcat.utils.AnnoUtils;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServletContextImpl implements ServletContext {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private Map<String, FilterRegistrationImp> filterRegistrations = new HashMap<>();

    private Map<String, Servlet> nameToServlets = new HashMap<>();
    private Map<String, Filter> nameToFilters = new HashMap<>();

    final List<ServletMapping> servletMappings = new ArrayList<>();
    final List<FilterMapping> filterMappings = new ArrayList<>();

    public void initFilters(List<Class<?>> filterClasses) {
        // Load Filter Classes
        for (Class<?> c : filterClasses) {
            WebFilter wf = c.getAnnotation(WebFilter.class);
            if (wf != null) {
                logger.info("auto register @WebFilter: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Filter> clazz = (Class<? extends Filter>) c;
                // This step will update this.filterRegistrations
                FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
                        AnnoUtils.getFilterUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
            }
        }
        // Init filters which are wrapped in registration
        for (String name : this.filterRegistrations.keySet()) {
            var registration = this.filterRegistrations.get(name);
            try {
                registration.filter.init(registration.getFilterConfig());
                this.nameToFilters.put(name, registration.filter);
                for (String urlPattern : registration.getUrlPatternMappings()) {
                    this.filterMappings.add(new FilterMapping(urlPattern, registration.filter));
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init filter failed: " + name + " / " + registration.filter.getClass().getName(), e);
            }
        }
    }

    public void initServlets(List<Class<?>> servletClasses) {
        // Load the servlet classes
        for (Class<?> servletClass : servletClasses) {
            WebServlet ws = servletClass.getAnnotation(WebServlet.class);
            if (ws != null) {
                logger.info("auto register @WebServlet: {}", servletClass.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> clazz = (Class<? extends Servlet>) servletClass;
                ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(clazz), clazz);
                registration.addMapping(AnnoUtils.getServletUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getServletInitParams(clazz));
            }
        }

        // Init all servlets
        for (String name : this.servletRegistrations.keySet()) {
            var registration = this.servletRegistrations.get(name);
            try {
                registration.servlet.init(registration.getServletConfig());
                this.nameToServlets.put(name, registration.servlet);
                for (String urlPattern : registration.getMappings()) {
                    this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init servlet failed: " + name + " / " + registration.servlet.getClass().getName(), e);
            }
        }
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uri = request.getRequestURI();

        // Find the matched servlet
        Servlet servlet = null;
        for (ServletMapping mapping : this.servletMappings) {
            if (mapping.matches(uri)) {
                servlet = mapping.servlet;
                break;
            }
        }
        if (servlet == null) {
            PrintWriter pm = response.getWriter();
            pm.write("<h1>404 Not Found</h1> <p>No mapping for URL: " + uri + "</p>");
            pm.close();
            return;
        }

        // servlet.service(request, response);
        // Find the matched filters:
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matches(uri)) {
                enabledFilters.add(mapping.filter);
            }
        }
        Filter[] filters = enabledFilters.toArray(Filter[]::new);
        logger.atDebug().log("process {} by filter {}, servlet {}", uri, Arrays.toString(filters), servlet);
        FilterChain chain = new FilterChainImpl(filters, servlet);
        try {
            chain.doFilter(request, response);
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getContextPath() {
        // Only support root context path
        return "";
    }

    @Override
    public ServletContext getContext(String uripath) {
        if ("".equals(uripath)) {
            return this;
        }
        return null;
    }

    @Override
    public String getMimeType(String file) {
        String defaultMime = "application/octet-stream";
        Map<String, String> mimes = Map.of(".html", "text/html", ".txt", "text/plain", ".png", "image/png", ".jpg",
                "image/jpeg");
        int n = file.lastIndexOf(".");
        if (n == -1) {
            return defaultMime;
        }
        String ext = file.substring(n);
        return mimes.getOrDefault(ext, defaultMime);
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        throw new UnsupportedOperationException("setInitParameter");
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {

        // or className == null || className.isEmpty()
        if (className.isBlank()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Servlet servlet = null;
        try {
            Class<? extends Servlet> clazz = createInstance(className);
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(servletName, servlet);

    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        if (servletClass == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Servlet servlet = null;
        try {
            servlet = createInstance(servletClass);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        return addServlet(servletName, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        // Final step of addServlet: registration
        if (servletName == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("servlet is null,");
        }
        var registration = new ServletRegistrationImpl(this, servletName, servlet);
        this.servletRegistrations.put(servletName, registration);
        return registration;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return createInstance(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return this.servletRegistrations.get(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Map.copyOf(this.servletRegistrations);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        if (className.isBlank()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Filter filter = null;
        try {
            Class<? extends Filter> clazz = createInstance(className);
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if (filterName == null) {
            throw new IllegalArgumentException("class is null.");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is null,");
        }
        var registration = new FilterRegistrationImp(this, filterName, filter);
        this.filterRegistrations.put(filterName, registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) {
        if (filterClass == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Filter filter = null;
        try {
            filter = createInstance(filterClass);

        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(filterName, filter);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return createInstance(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return this.filterRegistrations.get(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.copyOf(this.filterRegistrations);
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    private <T> T createInstance(String className) throws ServletException {
        Class<T> clazz = null;

        return createInstance(clazz);
    }

    private <T> T createInstance(Class<T> clazz) throws ServletException {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Cannot instantiate class" + clazz.getName(), e);
        }
    }

    // =============== Not implemented yet ===============

    @Override
    public Set<String> getResourcePaths(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResourcePaths'");
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResource'");
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResourceAsStream'");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestDispatcher'");
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNamedDispatcher'");
    }

    @Override
    public void log(String msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'log'");
    }

    @Override
    public void log(String message, Throwable throwable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'log'");
    }

    @Override
    public String getRealPath(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRealPath'");
    }

    @Override
    public String getServerInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServerInfo'");
    }

    @Override
    public Object getAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttribute'");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeNames'");
    }

    @Override
    public void setAttribute(String name, Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    @Override
    public void removeAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAttribute'");
    }

    @Override
    public String getServletContextName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletContextName'");
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addJspFile'");
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSessionCookieConfig'");
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSessionTrackingModes'");
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultSessionTrackingModes'");
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEffectiveSessionTrackingModes'");
    }

    @Override
    public void addListener(String className) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createListener'");
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getJspConfigDescriptor'");
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClassLoader'");
    }

    @Override
    public void declareRoles(String... roleNames) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'declareRoles'");
    }

    @Override
    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVirtualServerName'");
    }

    @Override
    public int getSessionTimeout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSessionTimeout'");
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSessionTimeout'");
    }

    @Override
    public String getRequestCharacterEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestCharacterEncoding'");
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRequestCharacterEncoding'");
    }

    @Override
    public String getResponseCharacterEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResponseCharacterEncoding'");
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setResponseCharacterEncoding'");
    }

}
