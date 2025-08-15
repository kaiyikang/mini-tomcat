package com.kaiyikang.minitomcat.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class SessionManager {

    final Logger logger = LoggerFactory.getLogger(getClass());
    final ServletContextImpl servletContext;
    // final Map<String, HttpSessionImple> sessions = new ConcurrentHashMap<>();
    final int inactiveInterval;

    public SessionManager(ServletContextImpl servletContext, int interval) {
        this.servletContext = servletContext;
        this.inactiveInterval = interval;
    }
}
