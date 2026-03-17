package com.example.twitterclone.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TweetStore implements AutoCloseable {
    private final ConcurrentHashMap<String, Tweet> tweets;
    private final WriteAheadLog wal;
    private final AtomicLong tweetCount = new AtomicLong(0);

    public TweetStore(Path walPath) throws IOException {
        this.tweets = new ConcurrentHashMap<>();
        this.wal = new WriteAheadLog(walPath);
        init();
    }

    private void init() throws IOException {
        System.out.println("TweetStore: Replaying WAL from " + wal.getWalPath() + "...");
        List<Tweet> persistedTweets = wal.replay();
        for (Tweet tweet : persistedTweets) {
            tweets.put(tweet.id(), tweet);
            tweetCount.incrementAndGet();
        }
        System.out.println("TweetStore: Replay complete. Loaded " + tweetCount.get() + " tweets.");
    }

    /**
     * Stores a tweet durably and atomically.
     * First appends to WAL, then updates in-memory store.
     * @param tweet The tweet to store.
     * @return The stored tweet, or null if a tweet with the same ID already existed and was not updated.
     * @throws IOException If an I/O error occurs during WAL append.
     */
    public Tweet storeTweet(Tweet tweet) throws IOException {
        Objects.requireNonNull(tweet, "Tweet cannot be null");
        // 1. Append to WAL and force to disk (durability & atomicity)
        wal.append(tweet);

        // 2. Apply to in-memory store
        // We use putIfAbsent to ensure uniqueness and prevent overwriting unless explicitly desired.
        // For this lesson, we treat existing IDs as "already committed", so putIfAbsent is appropriate.
        Tweet existingTweet = tweets.putIfAbsent(tweet.id(), tweet);
        if (existingTweet == null) {
            tweetCount.incrementAndGet();
            return tweet;
        } else {
            // Tweet with this ID already exists.
            // For a simple demo, we'll just return the existing one.
            // In a real system, you might compare timestamps for updates or throw an error.
            System.out.println("TweetStore: Tweet with ID " + tweet.id() + " already exists. WAL entry committed, but in-memory state unchanged.");
            return existingTweet;
        }
    }

    public Tweet getTweet(String id) {
        return tweets.get(id);
    }

    public long getTweetCount() {
        return tweetCount.get();
    }

    @Override
    public void close() throws Exception {
        wal.close();
        System.out.println("TweetStore: Closed.");
    }
}
