package com.kaiyikang.minitomcat.connector;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.time.Duration;

import java.util.List;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.HttpServer;

import com.kaiyikang.minitomcat.Config;
import com.kaiyikang.minitomcat.engine.HttpServletRequestImpl;
import com.kaiyikang.minitomcat.engine.HttpServletResponseImpl;
import com.kaiyikang.minitomcat.engine.ServletContextImpl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final Config config;
    final ClassLoader classLoader;
    final ServletContextImpl servletContext;
    final HttpServer httpServer;
    final Duration stopDisplay = Duration.ofSeconds(5);

    public HttpConnector(Config config, String webRoot, Executor executor, ClassLoader classloader,
            List<Class<?>> autoScannedClasses) throws IOException {

        String host = config.server.host;
        int port = config.server.port;
        logger.info("Starting mini-tomcat http server at {}:{}...", host, port);

        this.config = config;
        this.classLoader = classloader;

        // Init servlet context
        Thread.currentThread().setContextClassLoader(this.classLoader);
        ServletContextImpl ctx = new ServletContextImpl(classloader, config, webRoot);
        ctx.initialize(autoScannedClasses);
        this.servletContext = ctx;
        Thread.currentThread().setContextClassLoader(null);

        // Start http server to listen
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), config.server.backlog,
                "/", this);
        this.httpServer.setExecutor(executor);
        this.httpServer.start();
        logger.info("mini tomcat http server started at {}:{} ...", host, port);
    }

    @Override
    public void close() {
        this.servletContext.destroy();
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
