package com.impact.lessons.cache;

import java.lang.reflect.Type;

public interface CacheMarshaller {
    byte[] serialize(Object value);
    <T> T deserialize(byte[] data, Type type);
}

