package org.example;

import java.io.IOException;

@FunctionalInterface
public interface Handler {
    void handle(Request request, Response response) throws IOException;
}