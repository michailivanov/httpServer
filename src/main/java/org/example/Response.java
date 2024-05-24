package org.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class Response {
    private final SocketChannel clientChannel;

    public Response(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public void sendJson(int statusCode, Map<String, Object> jsonData) throws IOException {
        String jsonBody = buildJsonBody(jsonData);
        String response = buildResponse(statusCode, "application/json", jsonBody);
        sendResponse(response);
        clientChannel.close();
    }

    public void sendText(int statusCode, String responseBody) throws IOException {
        String response = buildResponse(statusCode, "text/plain", responseBody);
        sendResponse(response);
        clientChannel.close();
    }

    public void sendMultipart(int statusCode, Map<String, String> formData, Map<String, byte[]> fileData) throws IOException {
        String responseBody = buildMultipartBody(formData, fileData);
        String response = buildResponse(statusCode, "text/plain", responseBody);
        sendResponse(response);
        clientChannel.close();
    }

    private String buildResponse(int statusCode, String contentType, String responseBody) {
        String statusText = getStatusText(statusCode);
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + responseBody.getBytes().length + "\r\n" +
                "\r\n" +
                responseBody;
    }

    private void sendResponse(String response) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        clientChannel.write(buffer);
        clientChannel.close();
    }

    private String buildJsonBody(Map<String, Object> jsonData) {
        StringBuilder jsonBuilder = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : jsonData.entrySet()) {
            jsonBuilder.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                jsonBuilder.append("\"").append(value).append("\"");
            } else {
                jsonBuilder.append(value);
            }
            jsonBuilder.append(",");
        }
        if (jsonBuilder.length() > 1) {
            jsonBuilder.setLength(jsonBuilder.length() - 1);
        }
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private String buildMultipartBody(Map<String, String> formData, Map<String, byte[]> fileData) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Received form data: ");
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            bodyBuilder.append(entry.getKey()).append("= ").append(entry.getValue());
        }
        for (Map.Entry<String, byte[]> entry : fileData.entrySet()) {
            bodyBuilder.append("\nReceived file: ").append(entry.getKey())
                    .append(", Content: ").append(new String(entry.getValue()));
        }
        return bodyBuilder.toString();
    }

    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown";
        }
    }
}