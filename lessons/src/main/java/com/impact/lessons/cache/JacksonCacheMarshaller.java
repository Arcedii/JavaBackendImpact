package com.impact.lessons.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

@Component
public class JacksonCacheMarshaller implements CacheMarshaller {
    private final ObjectMapper objectMapper;

    public JacksonCacheMarshaller(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize cache value", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Type type) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(data, javaType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize cache value", e);
        }
    }
}

