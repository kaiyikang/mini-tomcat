package com.kaiyikang.minitomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.sun.net.httpserver.HttpServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.kaiyikang.minitomcat.engine.HttpServletRequestImpl;
import com.kaiyikang.minitomcat.engine.HttpServletResponseImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpServer httpServer;

    public HttpConnector() throws IOException {
        String host = "0.0.0.0";
        int port = 8080;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0, "/", this);
        this.httpServer.start();
        logger.info("mini tomcat http server started at {} : {} ...", host, port);
    }

    @Override
    public void close() {
        this.httpServer.stop(3);
    }

    @Override
    public void handle(HttpExchange exchagne) throws IOException {
        logger.info("{}: {}?{}", exchagne.getRequestMethod(), exchagne.getRequestURI().getPath(),
                exchagne.getRequestURI().getRawQuery());
        var adaptor = new HttpExchangeAdapter(exchagne);
        var request = new HttpServletRequestImpl(adaptor);
        var response = new HttpServletResponseImpl(adaptor);
        try {
            process(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String name = request.getParameter("name");
        String html = "<h1>Hello " + (name == null ? "world" : name) + "</h1>";

        response.setContentType("text/html");
        try (PrintWriter pw = response.getWriter()) {
            pw.write(html);
        }

    }
}
