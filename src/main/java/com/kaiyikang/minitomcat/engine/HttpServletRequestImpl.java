package com.kaiyikang.minitomcat.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.kaiyikang.minitomcat.engine.support.Attributes;

import com.kaiyikang.minitomcat.connector.HttpExchangeRequest;
import com.kaiyikang.minitomcat.engine.support.HttpHeaders;
import com.kaiyikang.minitomcat.engine.support.Parameters;
import com.kaiyikang.minitomcat.utils.HttpUtils;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

public class HttpServletRequestImpl implements HttpServletRequest {

    final ServletContextImpl servletContext;
    final HttpExchangeRequest exchangeRequest;
    final HttpServletResponse response;
    final String method;
    final HttpHeaders headers;
    final Parameters parameters;

    String characterEncoding;
    int contentLength = 0;

    String reqeustId = null;
    Attributes attributes = new Attributes();

    Boolean inputCalled = null;

    public HttpServletRequestImpl(ServletContextImpl servletContext, HttpExchangeRequest exchangeRequest,
            HttpServletResponse response) {
        this.servletContext = servletContext;
        this.exchangeRequest = exchangeRequest;
        this.response = response;

        this.characterEncoding = "UTF-8";
        this.method = exchangeRequest.getRequestMethod();
        this.headers = new HttpHeaders(exchangeRequest.getRequestHeaders());
        this.parameters = new Parameters(exchangeRequest, "UTF-8");

        if ("POST".equals(this.method) || "PUT".equals(this.method) || "DELETE".equals(this.method)
                || "PATCH".equals(this.method)) {
            this.contentLength = getIntHeader("Content-Length");
        }
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
        this.parameters.setCharSet(env);
    }

    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.inputCalled == null) {
            this.inputCalled = Boolean.TRUE;
            return new ServletInputStreamImpl(this.exchangeRequest.getRequestBody());
        }
        throw new IllegalStateException("Cannot reopen input stream after "
                + (this.inputCalled ? "getInputStream()" : "getReader()") + " was called");
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.inputCalled == null) {
            this.inputCalled = Boolean.FALSE;
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(this.exchangeRequest.getRequestBody()), StandardCharsets.UTF_8));
        }
        throw new IllegalStateException("Cannot reopen input stream after "
                + (this.inputCalled ? "getInputStream()" : "getReader()") + " was called.");
    }

    @Override
    public String getParameter(String name) {
        return this.parameters.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return this.parameters.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return this.parameters.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameters.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getSchema() {
        return "http";
    }

    @Override
    public String getServerName() {
        return "localhost";
    }

    @Override
    public String getServerPort() {
        InetSocketAddress address = this.exchangeRequest.getLocalAddress();
        return address.getPort();
    }

    @Override
    public Locale getLocale() {
        return Locale.CHINA;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(Locale.CHINA, Locale.US));
    }

    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSecure'");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestDispatcher'");
    }

    @Override
    public ServletContext getServletContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletContext'");
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startAsync'");
    }

    @Override
    public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startAsync'");
    }

    @Override
    public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAsyncStarted'");
    }

    @Override
    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAsyncSupported'");
    }

    @Override
    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAsyncContext'");
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDispatcherType'");
    }

    @Override
    public String getRequestId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestId'");
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletConnection'");
    }

    @Override
    public String getAuthType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAuthType'");
    }

    @Override
    public Cookie[] getCookies() {
        String cookieValue = this.getHeader("Cookie");
        return HttpUtils.parseCookies(cookieValue);
    }

    // -------------------------------------------------------------------------------------------------
    @Override
    public String getMethod() {
        return exchangeRequest.getRequestMethod();
    }

    @Override
    public String getPathInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPathInfo'");
    }

    @Override
    public String getPathTranslated() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPathTranslated'");
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getRemoteUser() {
        // not support auth:
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        // not support auth:
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // not support auth:
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return exchangeRequest.getRequestURI().getPath();
    }

    @Override
    public HttpSession getSession(boolean create) {
        String sessionId = null;

        // Find Cookie from client
        Cookie[] cookies = getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        if (sessionId == null && !create) {
            return null;
        }
        // Create ID in session of server
        if (sessionId == null) {
            if (this.response.isCommitted()) {
                throw new IllegalStateException("Cannot create session for response is committed.");
            }
            sessionId = UUID.randomUUID().toString();
            // Set Cookie
            String cookieValue = "JSESSIONID=" + sessionId + "; Path=/; SameSite=Strict; HttpOnly";
            this.response.addHeader("Set-Cookie", cookieValue);
        }
        return this.servletContext.sessionManager.getSession(sessionId);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException("changeSessionId() is not supported.");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return true;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public long getDateHeader(String name) {
        return this.headers.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return this.headers.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> hs = this.headers.getHeaders(name);
        if (hs == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(hs);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.getHeaderNames());
    }

    @Override
    public int getIntHeader(String name) {
        return this.headers.getIntHeader(name);
    }

    // ========= Not implement yet =========

    @Override
    public Object getAttribute(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttribute'");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeNames'");
    }

    @Override
    public String getLocalAddr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocalAddr'");
    }

    @Override
    public String getLocalName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocalName'");
    }

    @Override
    public int getLocalPort() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocalPort'");
    }

    @Override
    public String getRemoteAddr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRemoteAddr'");
    }

    @Override
    public String getRemoteHost() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRemoteHost'");
    }

    @Override
    public int getRemotePort() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRemotePort'");
    }

    @Override
    public String getScheme() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getScheme'");
    }

    @Override
    public void removeAttribute(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeAttribute'");
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    @Override
    public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'authenticate'");
    }

    @Override
    public Part getPart(String arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPart'");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParts'");
    }

    @Override
    public String getQueryString() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQueryString'");
    }

    @Override
    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestURL'");
    }

    @Override
    public String getServletPath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletPath'");
    }

    @Override

    @Override
    public void login(String arg0, String arg1) throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'login'");
    }

    @Override
    public void logout() throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'logout'");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upgrade'");
    }

}
