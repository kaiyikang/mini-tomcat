package com.kaiyikang.minitomcat.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse {

    final HttpExchange exchange;
    byte[] requestBody;

    public HttpExchangeAdapter(HttpExchange httpExchange) {
        this.exchange = httpExchange;
    }

    @Override
    public URI getRequestURI() {
        return this.exchange.getRequestURI();
    }

    @Override
    public String getRequestMethod() {
        return this.exchange.getRequestMethod();
    }

    @Override
    public Headers getRequestHeaders() {
        return this.exchange.getRequestHeaders();
    }

    @Override
    public Headers getResponseHeaders() {
        return this.exchange.getResponseHeaders();
    }

    @Override
    public OutputStream getResponseBody() {
        return this.exchange.getResponseBody();
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        this.exchange.sendResponseHeaders(rCode, responseLength);
    }

    @Override
    public byte[] getRequestBody() throws IOException {
        if (this.requestBody == null) {
            try (InputStream input = this.exchange.getRequestBody()) {
                this.requestBody = input.readAllBytes();
            }
        }
        return this.requestBody;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return this.exchange.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return this.exchange.getLocalAddress();
    }
}
