package com.impact.lessons.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "impact.cache.type", havingValue = "redis")
public class SpringRedisConnectionProvider implements RedisConnectionProvider {
    private final RedisConnectionFactory connectionFactory;

    public SpringRedisConnectionProvider(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public RedisConnection getConnection() {
        return connectionFactory.getConnection();
    }
}
