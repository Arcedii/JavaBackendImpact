package com.impact.lessons.cache;

import org.springframework.data.redis.connection.RedisConnection;

public interface RedisConnectionProvider {
    RedisConnection getConnection();
}

