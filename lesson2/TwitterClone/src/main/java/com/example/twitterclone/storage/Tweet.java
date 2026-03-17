package com.example.twitterclone.storage;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public record Tweet(String id, String content, Instant timestamp) implements Serializable {
    public Tweet {
        Objects.requireNonNull(id, "Tweet ID cannot be null");
        Objects.requireNonNull(content, "Tweet content cannot be null");
        Objects.requireNonNull(timestamp, "Tweet timestamp cannot be null");
    }

    @Override
    public String toString() {
        return "Tweet[id='" + id + "', content='" + (content.length() > 30 ? content.substring(0, 27) + "..." : content) + "', timestamp=" + timestamp + "]";
    }
}
