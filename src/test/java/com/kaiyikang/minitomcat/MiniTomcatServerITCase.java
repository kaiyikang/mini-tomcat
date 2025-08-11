package com.kaiyikang.minitomcat;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.http.HttpClient;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
public class MiniTomcatServerITCase {

    private static Thread serverThread;
    private static final int SERVER_PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + SERVER_PORT;
    private static HttpClient httpClient;

    @BeforeAll
    static void startServer() throws Exception {

        serverThread = new Thread(() -> {
            try {
                Start.main(null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to start server in thread", e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServerReady();

        httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5))
                .build();

    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
    }

    @Test
    void testIndexPageIsServed() throws IOException, InterruptedException {

    }

    private static void waitForServerReady() throws InterruptedException {
        long timeoutMillis = 1000;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {

            try (Socket socket = new Socket("localhost", SERVER_PORT)) {
                System.out.println("Server is ready on port " + SERVER_PORT);
                return;
            } catch (ConnectException e) {
                Thread.sleep(200);
            } catch (IOException e) {
                fail("An unexpected IO error occurred while waiting for the server", e);
            }

        }
        fail("Server did not start within " + timeoutMillis + "ms.");
    }

}
