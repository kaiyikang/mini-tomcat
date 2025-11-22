package com.kaiyikang.minitomcat.engine;

import java.util.Map;
import java.util.stream.Collectors;

import com.kaiyikang.minitomcat.Config;
import com.kaiyikang.minitomcat.engine.support.Attributes;

import jakarta.servlet.SessionCookieConfig;

public class SessionCookieConfigImpl implements SessionCookieConfig {
    final Config config;
    final Attributes attributes = new Attributes();

    int maxAge;
    boolean httpOnly = true;
    boolean secure = false;
    String domain;
    // Browser will match the url path and this.path of cookie
    String path;

    public SessionCookieConfigImpl(Config config) {
        this.config = config;
        this.maxAge = config.server.webApp.sessionTimeout * 60;
    }

    @Override
    public void setName(String name) {
        config.server.webApp.sessionCookieName = name;
    }

    @Override
    public String getName() {
        return config.server.webApp.sessionCookieName;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String getDomain() {
        return this.domain;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setComment(String comment) {
        // ignore
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Override
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public int getMaxAge() {
        return this.maxAge;
    }

    @Override
    public void setAttribute(String name, String value) {
        this.attributes.setAttribute(name, value);
    }

    @Override
    public String getAttribute(String name) {
        return (String) this.attributes.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, Object> rawAttributes = this.attributes.getAttributes();
        Map<String, String> stringAttributes = rawAttributes.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (String) entry.getValue()));
        return stringAttributes;
    }

}
