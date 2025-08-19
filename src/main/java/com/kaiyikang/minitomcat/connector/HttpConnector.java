package com.kaiyikang.minitomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EventListener;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.kaiyikang.minitomcat.engine.HttpServletRequestImpl;
import com.kaiyikang.minitomcat.engine.HttpServletResponseImpl;
import com.kaiyikang.minitomcat.engine.ServletContextImpl;
import com.kaiyikang.minitomcat.engine.filter.HelloFilter;
import com.kaiyikang.minitomcat.engine.filter.LogFilter;
import com.kaiyikang.minitomcat.engine.servlet.IndexServlet;
import com.kaiyikang.minitomcat.engine.servlet.LoginServlet;
import com.kaiyikang.minitomcat.engine.servlet.LogoutServlet;
import com.kaiyikang.minitomcat.engine.servlet.HelloServlet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final ServletContextImpl servletContext;
    final HttpServer httpServer;
    final Duration stopDisplay = Duration.ofSeconds(5);

    public HttpConnector() throws IOException {
        // Define the classes, filters
        List<Class<?>> definedClasses = List.of(
                IndexServlet.class,
                HelloServlet.class,
                LoginServlet.class,
                LogoutServlet.class);
        List<Class<?>> definedFilters = List.of(
                LogFilter.class,
                HelloFilter.class);

        // Initialize the servlets with classes
        this.servletContext = new ServletContextImpl();
        this.servletContext.initServlets(definedClasses);
        this.servletContext.initFilters(definedFilters);

        // Define Listener
        List<Class<? extends EventListener>> definedListeners = List.of(
                HelloHttpSessionAttributeListener.class,
                HelloHttpSessionListener.class,
                HelloServletContextAttributeListener.class,
                HelloServletContextListener.class,
                HelloServletRequestAttributeListener.class,
                HelloServletRequestListener.class);
        for (var listener : definedListeners) {
            this.servletContext.addListener(listener);
        }

        // Start Http Server
        String host = "0.0.0.0";
        int port = 8080;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0, "/", this);
        this.httpServer.start();
        logger.info("mini tomcat http server started at {}:{} ...", host, port);
    }

    @Override
    public void close() {
        this.httpServer.stop((int) this.stopDisplay.toSeconds());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Where each request and response occurs
        logger.info("{}: {}?{}", exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getRawQuery());
        var adaptor = new HttpExchangeAdapter(exchange);
        var response = new HttpServletResponseImpl(adaptor);
        var request = new HttpServletRequestImpl(this.servletContext, adaptor, response);
        try {
            this.servletContext.process(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            response.cleanup();
        }
    }

}
