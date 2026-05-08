package com.impact.lessons.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheClient {
    Optional<byte[]> get(String key);
    void set(String key, byte[] value, Duration ttl);
    void delete(String key);
    void deleteByPrefix(String prefix);
}

