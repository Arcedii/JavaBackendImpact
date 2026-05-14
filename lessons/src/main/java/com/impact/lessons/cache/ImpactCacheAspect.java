package com.impact.lessons.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;

@Aspect
@Component
public class ImpactCacheAspect {
    private static final Logger log = LoggerFactory.getLogger(ImpactCacheAspect.class);
    private final CacheClient cacheClient;
    private final CacheMarshaller marshaller;
    private final CacheKeyGenerator keyGenerator;
    /** Nume scurt pentru loguri: InMemoryCacheClient vs RedisCacheClient */
    private final String cacheBackendName;

    public ImpactCacheAspect(CacheClient cacheClient, CacheMarshaller marshaller) {
        this.cacheClient = cacheClient;
        this.marshaller = marshaller;
        this.keyGenerator = CacheKeyGenerator.defaultGenerator();
        this.cacheBackendName = cacheClient.getClass().getSimpleName();
        log.info("ImpactCacheAspect wired with cache client: {}", cacheBackendName);
    }

    @Around("@annotation(com.impact.lessons.cache.ImpactCacheable)")
    public Object aroundCacheable(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        ImpactCacheable ann = method.getAnnotation(ImpactCacheable.class);

        Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
        if (returnType == Void.TYPE) {
            // "void" nu are rezultat care să merite cache-uit.
            return pjp.proceed();
        }

        CacheRequestContext.setBackend(cacheBackendName);

        String key = keyGenerator.generate(ann.prefix(), pjp.getArgs());
        Optional<byte[]> cached = cacheClient.get(key);
        if (cached.isPresent()) {
            CacheRequestContext.markHit();
            log.debug("cache HIT backend={} key={}", cacheBackendName, key);
            Type genericReturnType = method.getGenericReturnType();
            // Folosim genericReturnType ca să deserializăm corect tipuri gen List<UserDto>.
            return marshaller.deserialize(cached.get(), genericReturnType);
        }

        CacheRequestContext.markMiss();
        log.debug("cache MISS backend={} key={}", cacheBackendName, key);
        Object result = pjp.proceed();
        if (result != null) {
            // Nu cache-uim null ca să evităm "negative caching" neintenționat.
            cacheClient.set(key, marshaller.serialize(result), Duration.ofSeconds(ann.ttlSeconds()));
            log.debug("cache PUT backend={} key={} ttlSeconds={}", cacheBackendName, key, ann.ttlSeconds());
        }
        return result;
    }

    @Around("@annotation(com.impact.lessons.cache.ImpactCacheEvict)")
    public Object aroundEvict(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        ImpactCacheEvict ann = method.getAnnotation(ImpactCacheEvict.class);

        CacheRequestContext.setBackend(cacheBackendName);

        Object result = pjp.proceed();

        if (ann.allEntries()) {
            // Ștergere "în masă" pe prefix: ex. users:list:*.
            cacheClient.deleteByPrefix(ann.prefix() + ":");
            log.debug("cache EVICT backend={} prefix={}*", cacheBackendName, ann.prefix());
        } else {
            String key = keyGenerator.generate(ann.prefix(), pjp.getArgs());
            cacheClient.delete(key);
            log.debug("cache EVICT backend={} key={}", cacheBackendName, key);
        }

        return result;
    }
}

