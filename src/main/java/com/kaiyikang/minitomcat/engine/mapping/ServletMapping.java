package com.kaiyikang.minitomcat.engine.mapping;

import jakarta.servlet.Servlet;

public class ServletMapping extends AbstractMapping {

    final Servlet servlet;

    public ServletMapping(String urlPattern, Servlet servlet) {
        super(urlPattern);
        this.servlet = servlet;
    }

}
