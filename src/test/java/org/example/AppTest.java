package org.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class AppTest {

    private static Server server;
    private static Handler mockHandler;
    private static ExecutorService executor;

    @BeforeAll
    public static void setUp() throws InterruptedException {
        server = new Server("localhost", 8081);
        server.registerDefaultHandlers();
        mockHandler = mock(Handler.class);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                server.startServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1000);
    }

    @Test
    public void testRegisterHandler() {
        server.registerHandler("/test", "GET", mockHandler);
        Handler handler = server.getHandlers().get("GET /test");
        assertNotNull(handler);
        assertEquals(mockHandler, handler);
    }

    @Test
    public void testDefaultHandlers() {
        server.registerDefaultHandlers();

        Handler getHandler = server.getHandlers().get("GET /");
        assertNotNull(getHandler);

        Handler postHandler = server.getHandlers().get("POST /");
        assertNotNull(postHandler);

        Handler putHandler = server.getHandlers().get("PUT /");
        assertNotNull(putHandler);

        Handler patchHandler = server.getHandlers().get("PATCH /");
        assertNotNull(patchHandler);

        Handler deleteHandler = server.getHandlers().get("DELETE /");
        assertNotNull(deleteHandler);
    }

    @Test
    public void testHandleGetNotFound() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            os.write("GET /notfound HTTP/1.1\r\n\r\n".getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                response.append(line).append("\r\n");
            }

            String responseStr = response.toString();
            System.out.println(responseStr);
            assertTrue(responseStr.contains("HTTP/1.1 404 Not Found"));
        }
    }

    @Test
    public void testHandleGetRequest() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            os.write("GET / HTTP/1.1\r\n\r\n".getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String statusLine = reader.readLine();

            assertTrue(statusLine.contains("HTTP/1.1 200 OK"));
        }
    }

    @Test
    public void testHandlePostRequestWithJson() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            String jsonData = "{\"message\":\"Hello World\"}";
            String request = "POST / HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + jsonData.length() + "\r\n" +
                    "\r\n" +
                    jsonData;

            os.write(request.getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String statusLine = reader.readLine();

            assertTrue(statusLine.contains("HTTP/1.1 200 OK"));
        }
    }

    @Test
    public void testHandlePutMultipartFormDataRequest() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            String body = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"text\"\r\n\r\n" +
                    "Hello World\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                    "Content-Type: text/plain\r\n\r\n" +
                    "This is a test file.\r\n" +
                    "--" + boundary + "--";

            String headers = "PUT / HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n" +
                    "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n";

            os.write(headers.getBytes(StandardCharsets.UTF_8));
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String statusLine = reader.readLine();

            assertTrue(statusLine.contains("HTTP/1.1 200 OK"));
        }
    }

    @Test
    public void testHandlePatchRequest() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            String patchData = "Hello World";
            String request = "PATCH / HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Content-Length: " + patchData.length() + "\r\n" +
                    "\r\n" +
                    patchData;

            os.write(request.getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String statusLine = reader.readLine();

            assertTrue(statusLine.contains("HTTP/1.1 200 OK"));
        }
    }


    @Test
    public void testHandleDeleteRequest() throws IOException {
        try (Socket clientSocket = new Socket("localhost", 8081)) {
            OutputStream os = clientSocket.getOutputStream();
            os.write("DELETE / HTTP/1.1\r\n\r\n".getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String statusLine = reader.readLine();

            assertTrue(statusLine.contains("HTTP/1.1 200 OK"));
        }
    }
}