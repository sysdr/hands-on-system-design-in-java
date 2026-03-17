package com.example.twitterclone.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WriteAheadLog implements Closeable {
    private final Path walPath;
    private FileChannel fileChannel;
    private final Object writeLock = new Object(); // For sequential writes

    public WriteAheadLog(Path walPath) throws IOException {
        this.walPath = Objects.requireNonNull(walPath, "WAL path cannot be null");
        Files.createDirectories(walPath.getParent()); // Ensure parent directory exists
        this.fileChannel = FileChannel.open(walPath,
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.WRITE,
                                            StandardOpenOption.APPEND);
    }

    /**
     * Appends a tweet to the WAL and forces it to disk.
     * Ensures atomicity and durability for this operation.
     * @param tweet The tweet to append.
     * @throws IOException If an I/O error occurs.
     */
    public void append(Tweet tweet) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(tweet);
        }
        byte[] tweetBytes = bos.toByteArray();

        synchronized (writeLock) {
            // Write length of the data first (4 bytes)
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            lengthBuffer.putInt(tweetBytes.length);
            lengthBuffer.flip();
            fileChannel.write(lengthBuffer);

            // Write the tweet data
            ByteBuffer dataBuffer = ByteBuffer.wrap(tweetBytes);
            fileChannel.write(dataBuffer);

            // Crucially, force the data to physical disk
            // true means also force metadata updates (e.g., file size)
            fileChannel.force(true);
        }
    }

    /**
     * Replays the WAL to reconstruct the state.
     * @return A list of tweets found in the log.
     * @throws IOException If an I/O error occurs.
     */
    public List<Tweet> replay() throws IOException {
        List<Tweet> tweets = new ArrayList<>();
        // Reopen channel for reading from the beginning
        try (FileChannel readChannel = FileChannel.open(walPath, StandardOpenOption.READ)) {
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            while (readChannel.read(lengthBuffer) != -1) {
                lengthBuffer.flip();
                if (lengthBuffer.remaining() < 4) {
                    // Incomplete length prefix, likely corrupted or partial write
                    System.err.println("WAL replay: Incomplete length prefix. Stopping replay.");
                    break;
                }
                int dataLength = lengthBuffer.getInt();
                lengthBuffer.clear();

                if (dataLength <= 0) {
                    System.err.println("WAL replay: Invalid data length (" + dataLength + "). Stopping replay.");
                    break;
                }

                ByteBuffer dataBuffer = ByteBuffer.allocate(dataLength);
                int bytesRead = 0;
                while (bytesRead < dataLength) {
                    int n = readChannel.read(dataBuffer);
                    if (n == -1) {
                        System.err.println("WAL replay: Unexpected EOF while reading tweet data. Stopping replay.");
                        break;
                    }
                    bytesRead += n;
                }
                if (bytesRead < dataLength) { // Check if we read all expected bytes
                     System.err.println("WAL replay: Incomplete tweet data read. Stopping replay.");
                     break;
                }
                dataBuffer.flip();

                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataBuffer.array()))) {
                    tweets.add((Tweet) ois.readObject());
                } catch (ClassNotFoundException e) {
                    throw new IOException("Failed to deserialize tweet from WAL", e);
                }
            }
        }
        return tweets;
    }

    public Path getWalPath() {
        return walPath;
    }

    @Override
    public void close() throws IOException {
        synchronized (writeLock) {
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close();
            }
        }
    }
}
