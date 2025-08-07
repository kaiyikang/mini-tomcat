package com.kaiyikang.minitomcat.connector;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpConnector implements HttpHandler, AutoCloseable {

    @Override
    public void close() {

    }

    @Override
    public void handle(HttpExchange exchagne) throws IOException {

    }
}
