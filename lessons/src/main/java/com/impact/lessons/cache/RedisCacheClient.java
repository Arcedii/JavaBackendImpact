package com.impact.lessons.cache;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "impact.cache.type", havingValue = "redis")
public class RedisCacheClient implements CacheClient {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheClient.class);

    private final RedisConnectionProvider connectionProvider;

    public RedisCacheClient(RedisConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @PostConstruct
    void logBackend() {
        log.info("Impact cache backend active: Redis (RedisCacheClient), impact.cache.type=redis");
    }

    @Override
    public Optional<byte[]> get(String key) {
        try (RedisConnection conn = connectionProvider.getConnection()) {
            byte[] data = conn.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
            return Optional.ofNullable(data);
        }
    }

    @Override
    public void set(String key, byte[] value, Duration ttl) {
        try (RedisConnection conn = connectionProvider.getConnection()) {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            conn.stringCommands().set(keyBytes, value);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                conn.keyCommands().expire(keyBytes, ttl.getSeconds());
            }
        }
    }

    @Override
    public void delete(String key) {
        try (RedisConnection conn = connectionProvider.getConnection()) {
            conn.keyCommands().del(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        String pattern = prefix + "*";
        try (RedisConnection conn = connectionProvider.getConnection()) {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            try (Cursor<byte[]> cursor = conn.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    conn.keyCommands().del(cursor.next());
                }
            }
        }
    }
}
