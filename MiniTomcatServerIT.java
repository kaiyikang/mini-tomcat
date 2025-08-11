import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Deprecated
public class MiniTomcatServerIT {

    private static Thread serverThread;
    private static final int SERVER_PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + SERVER_PORT;
    private static HttpClient httpClient;

    @BeforeAll
    static void startServer() throws InterruptedException {
        // 1. Start the server in a background thread
        serverThread = new Thread(() -> MiniTomcatServer.main(null));
        serverThread.setDaemon(true); // Optional: allows JVM to exit if test runner fails
        serverThread.start();

        // 2. Poll the server to wait until it's ready (the robust way)
        waitForServerReady();

        // 3. Initialize a shared HttpClient instance
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        // 4. Cleanly shut down the server thread after all tests are done
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            // Wait for the thread to die to ensure resources are released
            serverThread.join(2000);
        }
    }

    @Test
    void testIndexPageIsServed() throws IOException, InterruptedException {
        // Arrange: Create a request for the root path
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/"))
                .build();

        // Act: Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert: Check the status code and content
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Welcome to the Index Page!"), "Body should contain index page content");
    }

    @Test
    void testHelloPageIsServed() throws IOException, InterruptedException {
        // Arrange: Create a request for the /hello path
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/hello"))
                .build();

        // Act: Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert: Check the status code and content
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Hello from the Servlet!"), "Body should contain hello servlet content");
    }

    /**
     * A robust utility method to wait for the server to be listening on its port.
     * It replaces the fragile Thread.sleep().
     */
    private static void waitForServerReady() throws InterruptedException {
        long timeoutMillis = 10000; // 10-second timeout
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try (Socket socket = new Socket("localhost", SERVER_PORT)) {
                // If the connection succeeds, the server is up.
                System.out.println("Server is ready on port " + SERVER_PORT);
                return;
            } catch (ConnectException e) {
                // Connection refused, server is not ready yet. Wait and retry.
                Thread.sleep(200); // Poll every 200ms
            } catch (IOException e) {
                // Other IO exceptions
                fail("An unexpected IO error occurred while waiting for the server", e);
            }
        }
        fail("Server did not start within " + timeoutMillis + "ms.");
    }
}
