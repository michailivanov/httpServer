package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {
    private final String host;
    private final int port;
    private final Map<String, Handler> handlers = new HashMap<>();

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void registerHandler(String path, String method, Handler handler) {
        handlers.put(method + " " + path, handler);
    }

    public void registerDefaultHandlers() {
        registerHandler("/", "GET", (req, res) -> res.sendText(200, "Received GET request"));
        registerHandler("/", "POST", (req, res) -> {
            if (req.isJson()) {
                res.sendJson(200, req.getJsonData());
            } else if (req.isMultipart()) {
                res.sendMultipart(200, req.getFormData(), req.getFileData());
            } else {
                res.sendText(200, "Received POST request with body: " + req.getBody());
            }
        });
        registerHandler("/", "PUT", (req, res) -> {
            if (req.isJson()) {
                res.sendJson(200, req.getJsonData());
            } else if (req.isMultipart()) {
                res.sendMultipart(200, req.getFormData(), req.getFileData());
            } else {
                res.sendText(200, "Received PUT request with body: " + req.getBody());
            }
        });
        registerHandler("/", "PATCH", (req, res) -> {
            if (req.isJson()) {
                res.sendJson(200, req.getJsonData());
            } else if (req.isMultipart()) {
                res.sendMultipart(200, req.getFormData(), req.getFileData());
            } else {
                res.sendText(200, "Received PATCH request with body: " + req.getBody());
            }
        });
        registerHandler("/", "DELETE", (req, res) -> res.sendText(200, "Received DELETE request"));
    }

    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            clientChannel.close();
            return;
        }

        buffer.flip();
        String requestData = new String(buffer.array(), 0, bytesRead);
        Request request = Request.parse(requestData);
        Response response = new Response(clientChannel);

        String handlerKey = request.getMethod() + " " + request.getPath();
        Handler handler = handlers.get(handlerKey);

        if (handler != null) {
            handler.handle(request, response);
        } else {
            response.sendText(404, "Not Found");
        }
    }

    public void startServer() throws IOException {
        registerDefaultHandlers();

        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(key, selector);
                    } else if (key.isReadable()) {
                        handleRequest(key);
                    }
                }
            }
        }
    }

    private void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    public Map<String, Handler> getHandlers() {
        return handlers;
    }
}