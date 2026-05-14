package com.impact.lessons.cache;

public final class CacheRequestContext {
    private CacheRequestContext() {}

    public enum Status {
        HIT, MISS, BYPASS
    }

    private static final ThreadLocal<Status> STATUS = new ThreadLocal<>();
    private static final ThreadLocal<String> BACKEND = new ThreadLocal<>();

    public static void markHit() {
        STATUS.set(Status.HIT);
    }

    public static void markMiss() {
        STATUS.set(Status.MISS);
    }

    public static void markBypass() {
        STATUS.set(Status.BYPASS);
    }

    /** Numele clasei {@link CacheClient} activă (ex. InMemoryCacheClient, RedisCacheClient). */
    public static void setBackend(String backendSimpleClassName) {
        BACKEND.set(backendSimpleClassName);
    }

    public static Status getStatus() {
        return STATUS.get();
    }

    public static String getBackend() {
        return BACKEND.get();
    }

    public static void clear() {
        // Curățăm mereu la final de request ca să evităm "leak" între request-uri pe același thread.
        STATUS.remove();
        BACKEND.remove();
    }
}

