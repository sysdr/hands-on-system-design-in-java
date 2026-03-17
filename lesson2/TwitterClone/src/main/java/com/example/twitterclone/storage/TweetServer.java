package com.example.twitterclone.storage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TweetServer implements AutoCloseable {
    private final int port;
    private final TweetStore tweetStore;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService virtualThreadExecutor;

    public TweetServer(int port, Path walPath) throws IOException {
        this.port = port;
        this.tweetStore = new TweetStore(walPath);
        // Using Project Loom's Virtual Threads for high concurrency
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("TweetServer: Started on port " + port + ". Initial tweet count: " + tweetStore.getTweetCount());
        System.out.println("TweetServer: Waiting for client connections...");

        // Start a separate thread for graceful shutdown listener
        Thread shutdownHook = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("nType 'SHUTDOWN' and press Enter to stop the server gracefully.");
                while (running) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if ("SHUTDOWN".equalsIgnoreCase(line)) {
                            System.out.println("TweetServer: SHUTDOWN command received. Initiating graceful shutdown...");
                            stop();
                            break;
                        }
                    }
                    Thread.sleep(100); // Prevent busy-waiting
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error in shutdown listener: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
        shutdownHook.setDaemon(true); // Allow JVM to exit if main threads finish
        shutdownHook.start();


        while (running) {
            try {
                Socket clientSocket = serverSocket.accept(); // Blocking call
                if (running) { // Check 'running' again after accept, in case shutdown was initiated
                    virtualThreadExecutor.submit(() -> handleClient(clientSocket));
                } else {
                    clientSocket.close(); // Reject new connections if shutting down
                }
            } catch (IOException e) {
                if (running) { // Only log error if not during intentional shutdown
                    System.err.println("TweetServer: Error accepting client connection: " + e.getMessage());
                }
            }
        }
        System.out.println("TweetServer: Main server loop terminated.");
    }

    private void handleClient(Socket clientSocket) {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        System.out.println("TweetServer: Client connected: " + clientInfo);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("TweetServer: Received from " + clientInfo + ": " + line);
                if (line.startsWith("POST /tweet ")) {
                    String content = line.substring("POST /tweet ".length()).trim();
                    String tweetId = UUID.randomUUID().toString(); // Generate unique ID
                    Tweet newTweet = new Tweet(tweetId, content, Instant.now());
                    try {
                        tweetStore.storeTweet(newTweet);
                        out.println("OK: Tweet stored with ID " + tweetId);
                        System.out.println("TweetServer: Stored: " + newTweet);
                    } catch (IOException e) {
                        out.println("ERROR: Failed to store tweet: " + e.getMessage());
                        System.err.println("TweetServer: Error storing tweet: " + e.getMessage());
                    }
                } else if (line.startsWith("GET /tweet/")) {
                    String id = line.substring("GET /tweet/".length()).trim();
                    Tweet tweet = tweetStore.getTweet(id);
                    if (tweet != null) {
                        out.println("OK: " + tweet.content() + " (ID: " + tweet.id() + ")");
                    } else {
                        out.println("ERROR: Tweet with ID " + id + " not found.");
                    }
                } else if ("GET /metrics".equals(line) || "GET /count".equals(line)) {
                    out.println("OK: tweet_count=" + tweetStore.getTweetCount());
                } else if ("SHUTDOWN".equalsIgnoreCase(line.trim())) {
                    out.println("OK: Shutting down.");
                    stop();
                    break;
                } else {
                    out.println("ERROR: Unknown command. Use 'POST /tweet <content>' or 'GET /tweet/<id>' or 'GET /metrics'");
                }
            }
        } catch (IOException e) {
            System.err.println("TweetServer: Error handling client " + clientInfo + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("TweetServer: Client disconnected: " + clientInfo);
            } catch (IOException e) {
                System.err.println("TweetServer: Error closing client socket: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // This will unblock serverSocket.accept()
            }
        } catch (IOException e) {
            System.err.println("TweetServer: Error closing server socket: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        stop();
        virtualThreadExecutor.shutdown();
        if (!virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("TweetServer: Force shutting down virtual thread executor.");
            virtualThreadExecutor.shutdownNow();
        }
        tweetStore.close();
        System.out.println("TweetServer: Shut down gracefully.");
    }

    public static void main(String[] args) throws Exception {
        Path walPath = Paths.get("./data/wal/tweets.log");
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try (TweetServer server = new TweetServer(port, walPath)) {
            server.start();
        }
    }
}
