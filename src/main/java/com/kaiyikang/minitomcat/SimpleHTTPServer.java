package com.kaiyikang.minitomcat;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

public class SimpleHttpServer implements AutoCloseable, HttpHandler {

    final static Logger logger = LoggerFactory.getLogger(SimpleHttpServer.class);

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;
        try (SimpleHttpServer connector = new SimpleHttpServer(host, port)) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final String host;
    final int port;
    final HttpServer httpServer;

    public SimpleHttpServer(String host, int port) throws IOException {

        this.host = host;
        this.port = port;

        // create() needs handle method which should be implemented in the class.
        // For the most case, we could use:
        // this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0",
        // 8080), 0);
        // this.httpServer.createContext("/", this);
        this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0, "/", this);
        this.httpServer.start();
    }

    @Override
    public void close() {
        this.httpServer.stop(3);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Load the info from the request
        String httpMethod = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String query = uri.getRawQuery();
        logger.info("{}: {}?{}", httpMethod, path, query);

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html; charset=utf-8");
        responseHeaders.set("Cache-Control", "no-cache");

        // Set 200 response
        exchange.sendResponseHeaders(200, 0);

        // Set response
        String s = "<h1>Hello, World</h1><p>" + LocalDateTime.now().withNano(0) + "</p>";
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        }

    }
}
