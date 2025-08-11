package com.kaiyikang.minitomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.kaiyikang.minitomcat.engine.HttpServletRequestImpl;
import com.kaiyikang.minitomcat.engine.HttpServletResponseImpl;
import com.kaiyikang.minitomcat.engine.ServletContextImpl;
import com.kaiyikang.minitomcat.engine.servlet.IndexServlet;
import com.kaiyikang.minitomcat.engine.servlet.HelloServlet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpServer httpServer;
    final ServletContextImpl servletContext;
    final Duration stopDisplay = Duration.ofSeconds(5);

    public HttpConnector() throws IOException {
        // Define the classes
        List<Class<? extends HttpServlet>> definedClasses = List.of(IndexServlet.class, HelloServlet.class);

        // Initialize the servlets with claess
        this.servletContext = new ServletContextImpl();
        this.servletContext.initialize(definedClasses);

        // Start Http Server
        String host = "0.0.0.0";
        int port = 8080;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0, "/", this);
        this.httpServer.start();
        logger.info("mini tomcat http server started at {} : {} ...", host, port);
    }

    @Override
    public void close() {
        this.httpServer.stop((int) this.stopDisplay.toSeconds());
    }

    @Override
    public void handle(HttpExchange exchagne) throws IOException {
        logger.info("{}: {}?{}", exchagne.getRequestMethod(), exchagne.getRequestURI().getPath(),
                exchagne.getRequestURI().getRawQuery());
        var adaptor = new HttpExchangeAdapter(exchagne);
        var request = new HttpServletRequestImpl(adaptor);
        var response = new HttpServletResponseImpl(adaptor);
        try {
            this.servletContext.process(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
