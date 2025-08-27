package com.kaiyikang.minitomcat.engine;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaiyikang.minitomcat.engine.mapping.ServletMapping;
import com.kaiyikang.minitomcat.engine.support.Attributes;
import com.kaiyikang.minitomcat.Config;
import com.kaiyikang.minitomcat.engine.mapping.FilterMapping;

import com.kaiyikang.minitomcat.utils.AnnoUtils;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class ServletContextImpl implements ServletContext {

    final Logger logger = LoggerFactory.getLogger(getClass());

    // Load Class
    final ClassLoader classLoader;
    final Config config;
    final Path webRoot;

    // Session
    final SessionManager sessionManager; // = new SessionManager(this, 10);
    final SessionCookieConfig sessionCookieConfig;

    private boolean initialized = false;

    // Context attributes
    private Attributes attributes = new Attributes(true);

    private Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private Map<String, FilterRegistrationImp> filterRegistrations = new HashMap<>();

    private Map<String, Servlet> nameToServlets = new HashMap<>();
    private Map<String, Filter> nameToFilters = new HashMap<>();

    final List<ServletMapping> servletMappings = new ArrayList<>();
    final List<FilterMapping> filterMappings = new ArrayList<>();

    Servlet defaultServlet; // Q

    // Listener
    private List<ServletContextListener> servletContextListeners = null;
    private List<ServletContextAttributeListener> servletContextAttributeListeners = null;
    private List<ServletRequestListener> servletRequestListeners = null;
    private List<ServletRequestAttributeListener> servletRequestAttributeListeners = null;
    private List<HttpSessionListener> httpSessionListeners = null;
    private List<HttpSessionAttributeListener> httpSessionAttributeListeners = null;

    public ServletContextImpl(ClassLoader classLoader, Config config, String webRoot) {
        this.classLoader = classLoader; // Q
        this.config = config;
        this.sessionCookieConfig = new SessionCookieConfigIml(config); // Q
        this.webRoot = Paths.get(webRoot).normalize().toAbsolutePath();
        this.sessionManager = new SessionManager(this, config.server.webApp.sessionTimeout);
        logger.info("set web root: {}", this.webRoot);
    }

    public void process(HttpServletRequestImpl request, HttpServletResponseImpl response)
            throws IOException, ServletException {
        String uri = request.getRequestURI();

        // Find the matched servlet
        Servlet servlet = defaultServlet;
        if (!"/".equals(uri)) {
            for (ServletMapping mapping : this.servletMappings) {
                if (mapping.matches(uri)) {
                    servlet = mapping.servlet;
                    break;
                }
            }
        }

        if (servlet == null) {
            PrintWriter pw = response.getWriter();
            pw.write("<h1>404 Not Found</h1> <p>No mapping for URL: " + uri + "</p>");
            pw.flush();
            response.cleanup();
            return;
        }

        // Find the matched filters:
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matches(uri)) {
                enabledFilters.add(mapping.filter);
            }
        }

        // Process the request and response
        Filter[] filters = enabledFilters.toArray(Filter[]::new);
        logger.atDebug().log("process {} by filter {}, servlet {}", uri, Arrays.toString(filters), servlet);
        FilterChain chain = new FilterChainImpl(filters, servlet);

        try {
            this.invokeServletRequestInitialized(request);
            chain.doFilter(request, response);
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            this.invokeServletRequestDestroyed(request);
        }
    }

    // Begin of Invoke listeners
    void invokeServletContextInitialized() {
        logger.debug("invoke ServletContextInitialized: {}", this);
        if (this.servletContextListeners != null) {
            var event = new ServletContextEvent(this);
            for (var listener : this.servletContextListeners) {
                listener.contextInitialized(event);
            }
        }
    }

    void invokeServletContextDestroyed() {
        logger.debug("invoke ServletContextDestroyed: {}", this);
        if (this.servletContextListeners != null) {
            var event = new ServletContextEvent(this);
            for (var listener : this.servletContextListeners) {
                listener.contextDestroyed(event);
            }
        }
    }

    void invokeServletContextAttributeAdded(String name, Object object) {
        logger.info("invoke ServletContextAttributeAdded: {} = {}", name, object);
        if (this.servletContextAttributeListeners != null) {
            var event = new ServletContextAttributeEvent(this, name, object);
            for (var listener : this.servletContextAttributeListeners) {
                listener.attributeAdded(event);
            }
        }
    }

    void invokeServletContextAttributeRemoved(String name, Object value) {
        logger.info("invoke ServletContextAttributeRemoved: {} = {}", name, value);
        if (this.servletContextAttributeListeners != null) {
            var event = new ServletContextAttributeEvent(this, name, value);
            for (var listener : this.servletContextAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeServletContextAttributeReplaced(String name, Object value) {
        logger.info("invoke ServletContextAttributeReplaced: {} = {}", name, value);
        if (this.servletContextAttributeListeners != null) {
            var event = new ServletContextAttributeEvent(this, name, value);
            for (var listener : this.servletContextAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeServletRequestAttributeAdded(HttpServletRequest request, String name, Object value) {
        logger.info("invoke ServletRequestAttributeAdded: {} = {}", name, value);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeAdded(event);
            }
        }
    }

    void invokeServletRequestAttributeRemoved(HttpServletRequest request, String name, Object value) {
        logger.info("invoke invokeServletRequestAttributeRemoved: {} = {}", name, value);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeServletRequestAttributeReplaced(HttpServletRequest request, String name, Object value) {
        logger.info("invoke ServletRequestAttributeReplaced: {} = {}, request = {}", name, value, request);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeHttpSessionAttributeAdded(HttpSession session, String name, Object value) {
        logger.info("invoke HttpSessionAttributeAdded: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeAdded(event);
            }
        }
    }

    void invokeHttpSessionAttributeRemoved(HttpSession session, String name, Object value) {
        logger.info("invoke ServletContextAttributeRemoved: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeHttpSessionAttributeReplaced(HttpSession session, String name, Object value) {
        logger.info("invoke ServletContextAttributeReplaced: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeServletRequestInitialized(HttpServletRequest request) {
        logger.info("invoke ServletRequestInitialized: request = {}", request);
        if (this.servletRequestListeners != null) {
            var event = new ServletRequestEvent(this, request);
            for (var listener : this.servletRequestListeners) {
                listener.requestInitialized(event);
            }
        }
    }

    void invokeServletRequestDestroyed(HttpServletRequest request) {
        logger.info("invoke ServletRequestDestroyed: request = {}", request);
        if (this.servletRequestListeners != null) {
            var event = new ServletRequestEvent(this, request);
            for (var listener : this.servletRequestListeners) {
                listener.requestDestroyed(event);
            }
        }
    }

    void invokeHttpSessionCreated(HttpSession session) {
        logger.info("invoke HttpSessionCreated: session = {}", session);
        if (this.httpSessionListeners != null) {
            var event = new HttpSessionEvent(session);
            for (var listener : this.httpSessionListeners) {
                listener.sessionCreated(event);
            }
        }
    }

    void invokeHttpSessionDestroyed(HttpSession session) {
        logger.info("invoke HttpSessionDestroyed: session = {}", session);
        if (this.httpSessionListeners != null) {
            var event = new HttpSessionEvent(session);
            for (var listener : this.httpSessionListeners) {
                listener.sessionDestroyed(event);
            }
        }
    }

    // End of Invoke listeners

    @Override
    public String getContextPath() {
        // Only support root context path
        return "";
    }

    @Override
    public ServletContext getContext(String uriPath) {
        if ("".equals(uriPath)) {
            return this;
        }
        return null;
    }

    @Override
    public String getMimeType(String file) {
        return config.server.getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {

        String normalizedPath = (path != null && path.startsWith("/")) ? path.substring(1) : path;

        Path resolvedPath = this.webRoot.resolve(normalizedPath).normalize();

        if (!resolvedPath.startsWith(this.webRoot) || !Files.isDirectory(resolvedPath)) {
            return null;
        }

        try {
            return Files.list(resolvedPath).map(p -> p.getFileName().toString()).collect(Collectors.toSet());
        } catch (IOException e) {
            logger.warn("list files failed for path: {}", path);
        }
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        String normalizedPath = (path != null && path.startsWith("/")) ? path.substring(1) : path;
        Path resolvedPath = this.webRoot.resolve(normalizedPath).normalize();

        if (resolvedPath.startsWith(this.webRoot)) {
            return URI.create("file://" + resolvedPath.toString()).toURL();
        }

        throw new MalformedURLException("Path not found: " + path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        String normalizedPath = (path != null && path.startsWith("/")) ? path.substring(1) : path;

        Path resolvedPath = this.webRoot.resolve(normalizedPath).normalize();

        if (!resolvedPath.startsWith(this.webRoot) || !Files.isReadable(resolvedPath)) {
            return null;
        }

        try {
            return new BufferedInputStream(new FileInputStream(resolvedPath.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public String getRealPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Path resolvedPath = this.webRoot.resolve(path).normalize();
        if (resolvedPath.startsWith(this.webRoot)) {
            return resolvedPath.toString();
        }
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public void log(String msg) {
        logger.info(msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public String getServerInfo() {
        return this.config.server.name;
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
    public Object getAttribute(String name) {
        return this.attributes.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        if (object == null) {
            removeAttribute(name);
        } else {
            Object old = this.attributes.setAttribute(name, object);
            if (old == null) {
                this.invokeServletContextAttributeAdded(name, object);
            } else {
                this.invokeServletContextAttributeReplaced(name, object);
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object old = this.attributes.getAttribute(name);
        this.invokeServletContextAttributeRemoved(name, old);
    }

    @Override
    public String getServletContextName() {
        return this.config.server.webApp.name;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        checkNotInitialized("addServlet");
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Servlet servlet = null;
        try {
            servlet = createInstance(className);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(servletName, servlet);

    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        checkNotInitialized("addServlet");

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
        checkNotInitialized("addServlet");

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
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        throw new UnsupportedOperationException("addJspFile");
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        checkNotInitialized("createServlet");
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
        checkNotInitialized("addFilter");

        if (className == null || className.isEmpty()) {
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
        checkNotInitialized("addFilter");

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
        checkNotInitialized("addFilter");

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
        checkNotInitialized("createFilter");

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
    public SessionCookieConfig getSessionCookieConfig() {
        return this.sessionCookieConfig;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException("Unimplemented method 'setSessionTrackingModes'");
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Set.of(SessionTrackingMode.COOKIE);
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return this.getDefaultSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        checkNotInitialized("addListener");

        EventListener listener = null;
        try {
            Class<EventListener> clazz = createInstance(className);
            listener = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        addListener(listener);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        checkNotInitialized("addListener");

        EventListener listener = null;
        try {
            listener = createInstance(listenerClass);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        addListener(listener);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        checkNotInitialized("addListener");

        if (t instanceof ServletContextListener listener) {
            if (this.servletContextListeners == null) {
                this.servletContextListeners = new ArrayList<>();
            }
            this.servletContextListeners.add(listener);
        } else if (t instanceof ServletContextAttributeListener listener) {
            if (this.servletContextAttributeListeners == null) {
                this.servletContextAttributeListeners = new ArrayList<>();
            }
            this.servletContextAttributeListeners.add(listener);
        } else if (t instanceof ServletRequestListener listener) {
            if (this.servletRequestListeners == null) {
                this.servletRequestListeners = new ArrayList<>();
            }
            this.servletRequestListeners.add(listener);
        } else if (t instanceof ServletRequestAttributeListener listener) {
            if (this.servletRequestAttributeListeners == null) {
                this.servletRequestAttributeListeners = new ArrayList<>();
            }
            this.servletRequestAttributeListeners.add(listener);
        } else if (t instanceof HttpSessionAttributeListener listener) {
            if (this.httpSessionAttributeListeners == null) {
                this.httpSessionAttributeListeners = new ArrayList<>();
            }
            this.httpSessionAttributeListeners.add(listener);
        } else if (t instanceof HttpSessionListener listener) {
            if (this.httpSessionListeners == null) {
                this.httpSessionListeners = new ArrayList<>();
            }
            this.httpSessionListeners.add(listener);
        } else {
            throw new IllegalArgumentException("Unsupported listener: " + t.getClass().getName());
        }
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        checkNotInitialized("createListener");
        return createInstance(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException("Unimplemented method 'declareRoles'");
    }

    @Override
    public String getVirtualServerName() {
        return this.config.server.webApp.virtualServerName;
    }

    @Override
    public int getSessionTimeout() {
        return this.sessionManager.inactiveInterval;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.config.server.webApp.sessionTimeout = sessionTimeout;
    }

    @Override
    public String getRequestCharacterEncoding() {
        return this.config.server.requestEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        checkNotInitialized("setRequestCharacterEncoding");
        throw new UnsupportedOperationException(
                "Request character encoding cannot be changed at runtime. It must be set in configuration.");
    }

    @Override
    public String getResponseCharacterEncoding() {
        return this.config.server.responseEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        checkNotInitialized("setResponseCharacterEncoding");
        throw new UnsupportedOperationException(
                "Response character encoding cannot be changed at runtime. It must be set in configuration.");
    }

    // get API version
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

    // Custom Methods

    public void initialize(List<Class<?>> autoScannedClasses) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot re-initialize.");
        }

        // Register WebListener
        for (Class<?> c : autoScannedClasses) {
            if (c.isAssignableFrom(WebListener.class)) {
                logger.info("auto register @WebListener: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends EventListener> clazz = (Class<? extends EventListener>) c;
                this.addListener(clazz);
            }
        }

        this.invokeServletContextInitialized();

        // Register WebServlet and WebFilter
        for (Class<?> c : autoScannedClasses) {
            WebServlet ws = c.getAnnotation(WebServlet.class);
            WebFilter wf = c.getAnnotation(WebFilter.class);

            if (ws != null) {
                logger.info("auto register @WebServlet: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> clazz = (Class<? extends Servlet>) c;
                ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(clazz), clazz);
                registration.addMapping(AnnoUtils.getServletUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getServletInitParams(clazz));
            }
            if (wf != null) {
                logger.info("auto register @WebFilter: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Filter> clazz = (Class<? extends Filter>) c;
                FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
                registration.addMappingForUrlPatterns(AnnoUtils.getFilterDispatcherTypes(clazz), true,
                        AnnoUtils.getFilterUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
            }
        }

        // Init Servlets
        Servlet defaultServlet = null;
        for (String name : this.servletRegistrations.keySet()) {
            var registration = this.servletRegistrations.get(name);
            try {
                registration.servlet.init(registration.getServletConfig());
                this.nameToServlets.put(name, registration.servlet);
                for (String urlPattern : registration.getMappings()) {
                    this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
                    if (urlPattern.equals("/")) {
                        if (defaultServlet == null) {
                            defaultServlet = registration.servlet;
                            logger.info("set default servlet: " + registration.getClassName());
                        } else {
                            logger.warn("found duplicate default servlet: " + registration.getClassName());
                        }

                    }
                }
            } catch (ServletException e) {
                logger.error("init servlet failed: " + name + " / " + registration.servlet.getClass().getName(), e);
            }
        }

        if (defaultServlet == null && config.server.webApp.fileListings) {
            logger.info("no default servlet. auto register {}...", DefaultServlet.class.getName());
            defaultServlet = new DefaultServlet();
            try {
                defaultServlet.init(new ServletConfig() {

                    @Override
                    public String getServletName() {
                        return "DefaultServlet";
                    }

                    @Override
                    public ServletContext getServletContext() {
                        return ServletContextImpl.this;
                    }

                    @Override
                    public String getInitParameter(String name) {
                        return null;
                    }

                    @Override
                    public Enumeration<String> getInitParameterNames() {
                        return Collections.emptyEnumeration();
                    }

                });
                this.servletMappings.add(new ServletMapping("/", defaultServlet));
            } catch (ServletException e) {
                logger.error("init default failed.", e);
            }
        }
        this.defaultServlet = defaultServlet;

        // Init Filters
        for (String name : this.filterRegistrations.keySet()) {
            var registration = this.filterRegistrations.get(name);
            try {
                registration.filter.init(registration.getFilterConfig());
                this.nameToFilters.put(name, registration.filter);
                for (String urlPattern : registration.getUrlPatternMappings()) {
                    this.filterMappings.add(new FilterMapping(name, urlPattern, registration.filter)); // Q
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init filter failed: " + name + "/" + registration.filter.getClass().getName(), e);
            }
        }

        // Sort by servlet mappings
        Collections.sort(this.servletMappings);
        // Sort by filter name
        Collections.sort(this.filterMappings, (f1, f2) -> {
            int cmp = f1.filterName.compareTo(f2.filterName);
            if (cmp == 0) {
                cmp = f1.compareTo(f2);
            }
            return cmp;
        });

        this.initialized = true;

    }

    public void destroy() {
        this.filterMappings.forEach(mapping -> {
            try {
                mapping.filter.destroy();
            } catch (Exception e) {
                logger.error("destroy filter '" + mapping.filter + "' failed.", e);
            }
        });

        this.servletMappings.forEach(mapping -> {
            try {
                mapping.servlet.destroy();
            } catch (Exception e) {
                logger.error("destroy servlet '" + mapping.servlet + "' failed.", e);
            }
        });

        this.invokeServletContextDestroyed();

    }

    private void checkNotInitialized(String name) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot call " + name + "after initialization.");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(String className) throws ServletException {
        Class<T> clazz;
        try {
            clazz = (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found.", e);
        }
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

}
