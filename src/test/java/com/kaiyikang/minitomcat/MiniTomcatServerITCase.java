package com.kaiyikang.minitomcat;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
public class MiniTomcatServerITCase {

    private static Thread serverThread;
    private static final int SERVER_PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + SERVER_PORT;
    private static HttpClient httpClient;

    @BeforeAll
    static void startServer() throws InterruptedException {

    }

    @AfterAll
    static void stopServer() throws InterruptedException {

    }

}
