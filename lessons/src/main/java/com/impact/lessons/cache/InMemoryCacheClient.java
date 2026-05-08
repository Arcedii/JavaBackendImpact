package com.impact.lessons.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "impact.cache.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryCacheClient implements CacheClient {
    private static final class Entry {
        final byte[] value;
        final long expiresAtMillis;

        Entry(byte[] value, long expiresAtMillis) {
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<byte[]> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) return Optional.empty();
        if (entry.expiresAtMillis > 0 && System.currentTimeMillis() >= entry.expiresAtMillis) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    @Override
    public void set(String key, byte[] value, Duration ttl) {
        long expiresAt = 0;
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            expiresAt = System.currentTimeMillis() + ttl.toMillis();
        }
        store.put(key, new Entry(value, expiresAt));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public void deleteByPrefix(String prefix) {
        for (String key : store.keySet()) {
            if (key.startsWith(prefix)) {
                store.remove(key);
            }
        }
    }
}

