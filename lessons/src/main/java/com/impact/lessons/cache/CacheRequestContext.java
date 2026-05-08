package com.impact.lessons.cache;

public final class CacheRequestContext {
    private CacheRequestContext() {}

    public enum Status {
        HIT, MISS, BYPASS
    }

    private static final ThreadLocal<Status> STATUS = new ThreadLocal<>();

    public static void markHit() {
        STATUS.set(Status.HIT);
    }

    public static void markMiss() {
        STATUS.set(Status.MISS);
    }

    public static void markBypass() {
        STATUS.set(Status.BYPASS);
    }

    public static Status getStatus() {
        return STATUS.get();
    }

    public static void clear() {
        STATUS.remove();
    }
}

