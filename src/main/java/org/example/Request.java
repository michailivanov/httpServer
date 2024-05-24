package org.example;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final Map<String, String> formData;
    private final Map<String, byte[]> fileData;
    private final Map<String, Object> jsonData;

    private Request(String method, String path, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.formData = new HashMap<>();
        this.fileData = new HashMap<>();
        this.jsonData = new HashMap<>();

        if (isMultipart()) {
            parseMultipartData();
        } else if (isJson()) {
            parseJsonData();
        }
    }

    public static Request parse(String requestData) {
        String[] lines = requestData.split("\r\n");
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];

        Map<String, String> headers = new HashMap<>();
        int i = 1;
        while (i < lines.length && !lines[i].isEmpty()) {
            String[] header = lines[i].split(": ");
            if (header.length == 2) {
                headers.put(header[0], header[1]);
            }
            i++;
        }

        StringBuilder bodyBuilder = new StringBuilder();
        for (int j = i + 1; j < lines.length; j++) {
            bodyBuilder.append(lines[j]).append("\r\n");
        }
        String body = bodyBuilder.toString().trim();

        return new Request(method, path, headers, body);
    }

    private void parseJsonData() {
        if (body != null && !body.isEmpty()) {
            String jsonString = body.trim();
            if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
                jsonString = jsonString.substring(1, jsonString.length() - 1);
                String[] pairs = jsonString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        Object value = parseJsonValue(keyValue[1].trim());
                        jsonData.put(key, value);
                    }
                }
            }
        }
    }

    private void parseMultipartData() {
        String boundary = headers.get("Boundary");
        String[] parts = body.split("--" + boundary);

        for (String part : parts) {
            if (part.contains("Content-Disposition")) {
                String[] lines = part.split("\r\n");
                String disposition = lines[1];
                String name = extractValue(disposition, "name");

                if (disposition.contains("filename")) {
                    String filename = extractValue(disposition, "filename");
                    String fileContent = extractFileContent(lines);
                    fileData.put(filename, fileContent.getBytes());
                } else {
                    String value = extractValue(lines);
                    formData.put(name, value);
                }
            }
        }
    }

    private String extractValue(String line, String key) {
        return line.split(key + "=\"")[1].split("\"")[0];
    }

    private String extractValue(String[] lines) {
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 3; i < lines.length; i++) {
            valueBuilder.append(lines[i]).append("\r\n");
        }
        return valueBuilder.toString().trim();
    }

    private Object parseJsonValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        } else if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        } else if (value.matches("-?\\d+(\\.\\d+)?")) {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } else {
            return value;
        }
    }

    private String extractFileContent(String[] lines) {
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 3; i < lines.length; i++) {
            contentBuilder.append(lines[i]).append("\r\n");
        }
        return contentBuilder.toString().trim();
    }



    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getJsonData() {
        return jsonData;
    }

    public Map<String, String> getFormData() {
        return formData;
    }

    public Map<String, byte[]> getFileData() {
        return fileData;
    }

    public boolean isMultipart() {
        return headers.getOrDefault("Content-Type", "").startsWith("multipart/form-data");
    }

    public boolean isJson() {
        return headers.getOrDefault("Content-Type", "").startsWith("application/json");
    }
}