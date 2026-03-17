package com.example.feedservice;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class FeedServer {
    private final FeedService feedService;
    private final int port;
    private final AtomicInteger feedRequests = new AtomicInteger(0);
    private volatile boolean running = true;

    public FeedServer(FeedService feedService, int port) {
        this.feedService = feedService;
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[FeedServer] Listening on port " + port);
            while (running) {
                Socket client = serverSocket.accept();
                Thread.ofVirtual().start(() -> handle(client));
            }
        }
    }

    private void handle(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String requestLine = in.readLine();
            if (requestLine == null) return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String path = parts[1].split("\\?")[0];
            String query = parts[1].contains("?") ? parts[1].substring(parts[1].indexOf('?') + 1) : "";
            String line; while ((line = in.readLine()) != null && !line.isEmpty()) { /* skip headers */ }
            if ("/feed".equals(path)) {
                String viewerId = "viewer1";
                for (String pair : query.split("&")) {
                    if (pair.startsWith("viewerId=")) {
                        viewerId = URLDecoder.decode(pair.substring("viewerId=".length()), StandardCharsets.UTF_8);
                        break;
                    }
                }
                feedRequests.incrementAndGet();
                var posts = feedService.getFeed(viewerId, 10);
                out.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK: posts=" + posts.size());
            } else if ("/metrics".equals(path)) {
                int req = feedRequests.get();
                int hits = feedService.getCacheHits();
                int misses = feedService.getCacheMisses();
                out.println("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK: feed_requests=" + req + " cache_hits=" + hits + " cache_misses=" + misses);
            } else {
                out.println("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\nUnknown path");
            }
        } catch (Exception e) {
            System.err.println("[FeedServer] " + e.getMessage());
        }
    }

    public void stop() { running = false; }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        FeedService svc = new FeedService(new MockFollowerService(), new MockPostService());
        FeedServer server = new FeedServer(svc, port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(); svc.shutdown(); }));
        server.start();
    }
}
