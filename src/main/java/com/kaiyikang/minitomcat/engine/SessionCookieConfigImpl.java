package com.kaiyikang.minitomcat.engine;

import java.util.Map;

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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public void setDomain(String domain) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDomain'");
    }

    @Override
    public String getDomain() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDomain'");
    }

    @Override
    public void setPath(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPath'");
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    @Override
    public void setComment(String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setComment'");
    }

    @Override
    public String getComment() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getComment'");
    }

    @Override
    public void setHttpOnly(boolean httpOnly) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHttpOnly'");
    }

    @Override
    public boolean isHttpOnly() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isHttpOnly'");
    }

    @Override
    public void setSecure(boolean secure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSecure'");
    }

    @Override
    public boolean isSecure() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSecure'");
    }

    @Override
    public void setMaxAge(int maxAge) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMaxAge'");
    }

    @Override
    public int getMaxAge() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMaxAge'");
    }

    @Override
    public void setAttribute(String name, String value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    @Override
    public String getAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttribute'");
    }

    @Override
    public Map<String, String> getAttributes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributes'");
    }

}
