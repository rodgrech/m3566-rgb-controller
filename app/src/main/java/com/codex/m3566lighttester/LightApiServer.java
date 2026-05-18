package com.codex.m3566lighttester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class LightApiServer {
    static final int PORT = 8765;

    private final M3566Lights lights = M3566Lights.getInstance();
    private ServerSocket serverSocket;
    private Thread thread;
    private volatile boolean running;

    synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), PORT));
        running = true;
        thread = new Thread(this::serveLoop, "m3566-light-api");
        thread.start();
    }

    synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
    }

    boolean isRunning() {
        return running;
    }

    private void serveLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handle(socket);
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket closeable = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(closeable.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(closeable.getOutputStream()))) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.trim().isEmpty()) {
                return;
            }

            while (true) {
                String header = reader.readLine();
                if (header == null || header.isEmpty()) {
                    break;
                }
            }

            Response response = route(requestLine);
            writer.write("HTTP/1.1 " + response.status + "\r\n");
            writer.write("Content-Type: " + response.contentType + "\r\n");
            writer.write("Access-Control-Allow-Origin: *\r\n");
            writer.write("Connection: close\r\n");
            writer.write("\r\n");
            writer.write(response.body);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Response route(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return text(400, "Bad request");
        }

        String target = parts[1];
        String path = target;
        String query = "";
        int queryIndex = target.indexOf('?');
        if (queryIndex >= 0) {
            path = target.substring(0, queryIndex);
            query = target.substring(queryIndex + 1);
        }

        Map<String, String> params = parseQuery(query);
        if ("/".equals(path) || "/help".equals(path)) {
            return text(200, help());
        }
        if ("/status".equals(path)) {
            return json(200, lights.getState().toJson());
        }
        if ("/set".equals(path)) {
            String result = lights.setRgb(flag(params.get("red")), flag(params.get("green")), flag(params.get("blue")));
            return json(200, "{\"ok\":true,\"result\":" + quote(result) + ",\"state\":" + lights.getState().toJson() + "}");
        }
        if (path.startsWith("/color/")) {
            String color = path.substring("/color/".length());
            String result = lights.setColorName(color);
            boolean ok = result.startsWith("OK ");
            return json(ok ? 200 : 400, "{\"ok\":" + ok + ",\"result\":" + quote(result) + ",\"state\":" + lights.getState().toJson() + "}");
        }
        if ("/test".equals(path)) {
            String result = lights.testSequence();
            return json(200, "{\"ok\":true,\"result\":" + quote(result) + ",\"state\":" + lights.getState().toJson() + "}");
        }

        return text(404, "Not found");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            String key = equals >= 0 ? pair.substring(0, equals) : pair;
            String value = equals >= 0 ? pair.substring(equals + 1) : "";
            result.put(decode(key).toLowerCase(Locale.US), decode(value));
        }
        return result;
    }

    private static boolean flag(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.US);
        return "1".equals(normalized) || "true".equals(normalized) || "on".equals(normalized) || "yes".equals(normalized);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static Response json(int status, String body) {
        return new Response(status, "application/json; charset=utf-8", body);
    }

    private static Response text(int status, String body) {
        return new Response(status, "text/plain; charset=utf-8", body);
    }

    private static String help() {
        return "M3566 Light API\n"
                + "GET /status\n"
                + "GET /set?red=1&green=0&blue=1\n"
                + "GET /color/red|green|blue|white|yellow|cyan|magenta|off\n"
                + "GET /test\n";
    }

    private static final class Response {
        final int status;
        final String contentType;
        final String body;

        Response(int status, String contentType, String body) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }
    }
}
