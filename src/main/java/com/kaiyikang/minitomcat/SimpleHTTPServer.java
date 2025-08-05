package com.kaiyikang.minitomcat;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class SimpleHttpServer implements AutoCloseable, HttpHandler {

    final static Logger logger = LoggerFactory.getLogger(SimpleHttpServer.class);

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;
        try (SimpleHttpServer connector = new SimpleHttpServer(host, port)) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final HttpServer httpServer;

    public SimpleHttpServer(String host, int port) throws IOException {
        System.out.println("Initial SimpleHttpServer ...");
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

    }
}
