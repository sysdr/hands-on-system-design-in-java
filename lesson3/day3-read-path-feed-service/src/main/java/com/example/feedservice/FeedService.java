package com.example.feedservice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// --- Mock Services for demonstration ---
record Post(String postId, String userId, String content, Instant timestamp) {}
record User(String userId, String username) {}

class MockPostService {
    private final Map<String, List<Post>> userPosts = new ConcurrentHashMap<>();

    public MockPostService() {
        // Populate some mock data
        userPosts.put("userA", List.of(
            new Post("p1", "userA", "Hello world!", Instant.parse("2023-01-01T10:00:00Z")),
            new Post("p2", "userA", "Another day, another post.", Instant.parse("2023-01-01T11:00:00Z")),
            new Post("p6", "userA", "Just published a new article!", Instant.parse("2023-01-01T15:30:00Z"))
        ));
        userPosts.put("userB", List.of(
            new Post("p3", "userB", "Learning distributed systems.", Instant.parse("2023-01-01T10:30:00Z")),
            new Post("p4", "userB", "Virtual Threads are awesome!", Instant.parse("2023-01-01T11:15:00Z")),
            new Post("p7", "userB", "Building the read path.", Instant.parse("2023-01-01T14:00:00Z"))
        ));
        userPosts.put("userC", List.of(
            new Post("p5", "userC", "Just had coffee.", Instant.parse("2023-01-01T09:45:00Z"))
        ));
    }

    List<Post> getPostsByUserId(String userId, int limit) {
        // Simulate network latency and data retrieval
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println(STR."[MockPostService] Fetched posts for {userId}");
        return userPosts.getOrDefault(userId, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(Post::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}

class MockFollowerService {
    private final Map<String, List<String>> userFollows = new ConcurrentHashMap<>();

    public MockFollowerService() {
        userFollows.put("viewer1", List.of("userA", "userB", "userC"));
        userFollows.put("viewer2", List.of("userA"));
    }

    List<String> getFollowedUsers(String userId) {
        // Simulate network latency
        try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println(STR."[MockFollowerService] Fetched followed users for {userId}");
        return userFollows.getOrDefault(userId, Collections.emptyList());
    }
}
// --- End Mock Services ---

public class FeedService {
    private final MockFollowerService followerService;
    private final MockPostService postService;
    private final ExecutorService virtualThreadExecutor; // Executor for Virtual Threads

    // Assignment: Add a cache for followed users
    private final Map<String, List<String>> followedUsersCache = new ConcurrentHashMap<>();
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);

    public FeedService(MockFollowerService followerService, MockPostService postService) {
        this.followerService = followerService;
        this.postService = postService;
        // Project Loom: Create an ExecutorService backed by Virtual Threads
        // This is crucial for efficient I/O-bound fan-out
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public List<String> getFollowedUsersCached(String viewerId) {
        List<String> followedUsers = followedUsersCache.get(viewerId);
        if (followedUsers == null) {
            cacheMisses.incrementAndGet();
            System.out.println(STR."[{viewerId}] Cache miss for followed users. Fetching from FollowerService.");
            followedUsers = followerService.getFollowedUsers(viewerId);
            followedUsersCache.put(viewerId, followedUsers); // Put in cache
        } else {
            cacheHits.incrementAndGet();
            System.out.println(STR."[{viewerId}] Cache hit for followed users.");
        }
        return followedUsers;
    }

    public int getCacheHits() { return cacheHits.get(); }
    public int getCacheMisses() { return cacheMisses.get(); }

    public void clearFollowedUsersCache() {
        System.out.println("[FeedService] Clearing followed users cache.");
        followedUsersCache.clear();
    }

    public List<Post> getFeed(String viewerId, int limit) {
        long startTime = System.currentTimeMillis();
        System.out.println(STR."n[{viewerId}] Fetching feed...");

        // 1. Get followed users (using our new cached method)
        List<String> followedUsers = getFollowedUsersCached(viewerId);
        System.out.println(STR."[{viewerId}] Followed: {followedUsers}");

        // 2. Concurrently fetch posts for each followed user using Virtual Threads
        List<Future<List<Post>>> futures = new ArrayList<>();
        for (String followedUserId : followedUsers) {
            futures.add(virtualThreadExecutor.submit(() -> {
                // The thread name will often indicate it's a virtual thread, e.g., "VirtualThread[...]"
                System.out.println(STR."[{viewerId}] Submitting post fetch for {followedUserId} on {Thread.currentThread().getName()}");
                return postService.getPostsByUserId(followedUserId, 10); // Get top 10 posts per user
            }));
        }

        // 3. Aggregate results
        List<Post> allPosts = new ArrayList<>();
        for (Future<List<Post>> future : futures) {
            try {
                // .get() blocks the current thread (which is likely a Virtual Thread itself)
                // but efficiently yields the underlying platform thread while waiting for I/O.
                allPosts.addAll(future.get());
            } catch (Exception e) {
                System.err.println(STR."Error fetching posts for a user: {e.getMessage()}");
                // In a real system, you might log this, return what you have, or use a fallback.
            }
        }

        // 4. Sort and limit
        List<Post> sortedFeed = allPosts.stream()
                .sorted(Comparator.comparing(Post::timestamp).reversed()) // Newest first
                .limit(limit)
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        System.out.println(STR."[{viewerId}] Feed generated in {endTime - startTime} ms. Total posts: {sortedFeed.size()}");
        return sortedFeed;
    }

    public void shutdown() {
        System.out.println("[FeedService] Shutting down virtual thread executor.");
        virtualThreadExecutor.shutdown();
    }

    public static void main(String[] args) {
        FeedService feedService = new FeedService(new MockFollowerService(), new MockPostService());

        // Test Viewer 1 feed - first call (cache miss)
        System.out.println("n--- Viewer 1 Feed (First Call - Cache Miss) ---");
        List<Post> viewer1Feed1 = feedService.getFeed("viewer1", 5);
        viewer1Feed1.forEach(System.out::println);

        // Test Viewer 1 feed - second call (cache hit)
        System.out.println("n--- Viewer 1 Feed (Second Call - Cache Hit) ---");
        List<Post> viewer1Feed2 = feedService.getFeed("viewer1", 5);
        viewer1Feed2.forEach(System.out::println);

        // Clear cache and test Viewer 1 again (simulating cache invalidation)
        feedService.clearFollowedUsersCache();
        System.out.println("n--- Viewer 1 Feed (After Cache Clear - Cache Miss) ---");
        List<Post> viewer1Feed3 = feedService.getFeed("viewer1", 5);
        viewer1Feed3.forEach(System.out::println);

        // Test Viewer 2 feed
        System.out.println("n--- Viewer 2 Feed ---");
        List<Post> viewer2Feed = feedService.getFeed("viewer2", 5);
        viewer2Feed.forEach(System.out::println);

        feedService.shutdown();
    }
}
