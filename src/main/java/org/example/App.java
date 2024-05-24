package org.example;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        Server server = new Server("localhost", 8081);
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}