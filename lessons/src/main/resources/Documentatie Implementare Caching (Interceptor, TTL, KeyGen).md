# Documentație tehnică: Caching (Interceptor/Decorator), TTL, Key Generator, Marshalling

## Scop

În acest урок am introdus **un mecanism de caching** care poate accelera endpoint-urile “read-heavy” (de exemplu listări), fără să “murdărim” business-logic cu `if (cache...)`.

Mai important: caching-ul este aplicat **automat**, în jurul metodelor, folosind **AOP (Aspect Oriented Programming)**. Asta înseamnă că:

- în `UserService` (sau alte servicii) scrii normal codul de business
- iar caching-ul se execută “înainte/după” metoda reală, într-un “interceptor” (aspect)

Concret, în proiect există următoarele piese:

- **Marshalling** (serializare/deserializare)
- **Interceptor / Decorator** (AOP) care aplică caching automat
- **TTL (Time-To-Live)** pentru expirarea datelor
- **Key Generator** pentru chei unice și stabile

În plus, am adăugat un semnal vizibil în Postman:

- header de răspuns: `**X-Impact-Cache: HIT`** sau `**X-Impact-Cache: MISS**`

---

## 1) Configurare rapidă (simplă)

### 1.1 Mod implicit: `memory` (fără Redis, fără Docker)

În `application.yaml` există setarea:

- `impact.cache.type: memory`

Acest mod folosește un cache in-memory (în aplicație). Este cel mai simplu pentru laborator/lecție:

- nu depinde de Redis
- arată clar principiile (TTL + chei + interceptor)

---

## 2) Cum funcționează caching-ul “pe românește”, pas cu pas

Imaginează-ți un endpoint care apelează `UserService.getAllUsers()`. Fără caching, de fiecare dată:

1. intră requestul
2. se apelează metoda
3. se citește din DB
4. se construiesc DTO-urile
5. se returnează răspunsul

Cu caching-ul nostru, “în fața” metodei stă `ImpactCacheAspect`, care face asta:

1. **Calculează cheia** (key) pentru metoda apelată
2. **Caută în cache**: există deja date pentru cheia aia?
   - dacă da → returnează datele din cache (HIT), fără DB
   - dacă nu → execută metoda reală (MISS), apoi pune rezultatul în cache cu TTL

Cheia idee: caching-ul e implementat “în jurul metodei”, nu în interiorul ei.

---

## 3) Blocurile conceptuale implementate în cod (cu exemple de cod)

### 2.1 Marshalling (Serializare / Deserializare)

**De ce există:** cache-ul stochează `byte[]`/string, dar aplicația lucrează cu obiecte (DTO, liste).

În proiect:

- `CacheMarshaller` definește contractul:
  - `serialize(Object) -> byte[]`
  - `deserialize(byte[], Type) -> T`
- `JacksonCacheMarshaller` implementează contractul folosind `ObjectMapper`.

Detaliu important:

- Deserializarea folosește `**Type`** (nu doar `Class`) ca să funcționeze corect cu tipuri generice precum `List<UserDto>`.

Cod relevant (`JacksonCacheMarshaller`):

```java
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
```

### 2.2 Key Generator

**De ce există:** cache-ul are nevoie de chei unice și predictibile, bazate pe parametrii metodei.

În proiect:

- `CacheKeyGenerator` generează chei în forma:
  - `prefix:arg1:arg2:...`
  - dacă cheia ar deveni prea lungă → folosește fallback `sha256`.

Ce face exact:

- dacă metoda **nu are argumente** → cheia devine `prefix:static`
- dacă are argumente → fiecare argument e normalizat:
  - `null` → `"null"`
  - se elimină spațiile multiple (`"a   b"` devine `"a b"`)
  - se taie la max 80 caractere (ca să nu explodeze cheia)
- dacă cheia finală depășește 256 caractere → se folosește `sha256`

Cod relevant (`CacheKeyGenerator`):

```java
public interface CacheKeyGenerator {
    String generate(String prefix, Object[] args);

    static CacheKeyGenerator defaultGenerator() {
        return new DefaultCacheKeyGenerator();
    }

    final class DefaultCacheKeyGenerator implements CacheKeyGenerator {
        @Override
        public String generate(String prefix, Object[] args) {
            if (args == null || args.length == 0) {
                return prefix + ":static";
            }
            StringBuilder sb = new StringBuilder(prefix);
            for (Object arg : args) {
                sb.append(':').append(normalize(arg));
            }
            String key = sb.toString();
            if (key.length() <= 256) {
                return key;
            }
            return prefix + ":sha256:" + sha256Hex(key);
        }

        private static String normalize(Object arg) {
            if (arg == null) return "null";
            String s = String.valueOf(arg);
            s = s.replaceAll("\\s+", " ").trim();
            return s.length() > 80 ? s.substring(0, 80) : s;
        }

        private static String sha256Hex(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(hashed);
            } catch (Exception e) {
                return Integer.toHexString(input.hashCode());
            }
        }
    }
}
```

Exemple conceptuale:

- `users:list:static`
- `user_profile:45`
- `products_list:category=shoes:page=2:size=20`

### 2.3 TTL (Time-To-Live)

**De ce există:** să nu livrăm date vechi. Cache-ul trebuie să expire automat.

În proiect:

- `@ImpactCacheable(ttlSeconds = 300)` setează TTL per metodă (ex: 5 minute).
- În `InMemoryCacheClient` TTL se verifică la `get`.

Cum e implementat în `InMemoryCacheClient`:

- la `set(key, value, ttl)` se calculează `expiresAtMillis = now + ttl`
- la `get(key)`:
  - dacă a expirat → intrarea se șterge și se consideră MISS
  - dacă nu a expirat → se returnează valoarea

Cod relevant (`InMemoryCacheClient`):

```java
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
}
```

### 2.4 Interceptor/Decorator (AOP)

**De ce există:** caching-ul trebuie aplicat “din afară”, fără să modificăm logica internă a serviciilor.

În proiect:

- `@ImpactCacheable` – marchează metodele care se cache-uiesc
- `@ImpactCacheEvict` – marchează metodele care invalidează cache-ul
- `ImpactCacheAspect` – “interceptorul” care:
  1. generează cheia
  2. caută în cache
  3. dacă găsește → returnează din cache (**HIT**)
  4. dacă nu găsește → execută metoda reală, apoi salvează în cache (**MISS**)

#### 2.4.1 `@ImpactCacheable` (ce înseamnă)

`@ImpactCacheable` are 2 câmpuri:

- `prefix()` – “numele” logic al cache-ului (ex: `users:list`)
- `ttlSeconds()` – cât timp ținem intrarea în cache

Cod:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImpactCacheable {
    String prefix();
    long ttlSeconds() default 300;
}
```

#### 2.4.2 `@ImpactCacheEvict` (ce înseamnă)

`@ImpactCacheEvict` e pentru metode care **modifică date**. După ce metoda rulează cu succes, noi “curățăm” cache-ul.

- `allEntries = true` înseamnă “șterge TOT ce începe cu prefix-ul acesta” (ex: toate cheile `users:list:*`)
- `allEntries = false` înseamnă “șterge doar cheia calculată din argumente”

Cod:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImpactCacheEvict {
    String prefix();
    boolean allEntries() default false;
}
```

#### 2.4.3 `ImpactCacheAspect` – logica reală (HIT / MISS / PUT / EVICT)

Acesta e “motorul” caching-ului. Are două interceptări:

- una pentru `@ImpactCacheable`
- una pentru `@ImpactCacheEvict`

Cod (varianta din proiect, cu explicații în text):

```java
@Aspect
@Component
public class ImpactCacheAspect {
    private final CacheClient cacheClient;
    private final CacheMarshaller marshaller;
    private final CacheKeyGenerator keyGenerator;

    public ImpactCacheAspect(CacheClient cacheClient, CacheMarshaller marshaller) {
        this.cacheClient = cacheClient;
        this.marshaller = marshaller;
        this.keyGenerator = CacheKeyGenerator.defaultGenerator();
    }

    @Around("@annotation(com.impact.lessons.cache.ImpactCacheable)")
    public Object aroundCacheable(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        ImpactCacheable ann = method.getAnnotation(ImpactCacheable.class);

        // Dacă metoda întoarce void, nu are sens să cache-uim.
        Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
        if (returnType == Void.TYPE) {
            return pjp.proceed();
        }

        // 1) key
        String key = keyGenerator.generate(ann.prefix(), pjp.getArgs());

        // 2) get
        Optional<byte[]> cached = cacheClient.get(key);
        if (cached.isPresent()) {
            CacheRequestContext.markHit();
            Type genericReturnType = method.getGenericReturnType();
            return marshaller.deserialize(cached.get(), genericReturnType);
        }

        // 3) MISS -> rulează metoda reală
        CacheRequestContext.markMiss();
        Object result = pjp.proceed();

        // 4) PUT (doar dacă nu e null)
        if (result != null) {
            cacheClient.set(key, marshaller.serialize(result), Duration.ofSeconds(ann.ttlSeconds()));
        }
        return result;
    }

    @Around("@annotation(com.impact.lessons.cache.ImpactCacheEvict)")
    public Object aroundEvict(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        ImpactCacheEvict ann = method.getAnnotation(ImpactCacheEvict.class);

        // Întâi executăm metoda (scrierea în DB).
        Object result = pjp.proceed();

        // Abia după aceea curățăm cache-ul.
        if (ann.allEntries()) {
            cacheClient.deleteByPrefix(ann.prefix() + ":");
        } else {
            String key = keyGenerator.generate(ann.prefix(), pjp.getArgs());
            cacheClient.delete(key);
        }
        return result;
    }
}
```

Observație importantă: la `@ImpactCacheEvict` noi facem `proceed()` întâi și **evict după**. Asta e intenționat: dacă metoda eșuează (excepție), nu vrei să golești cache-ul “degeaba”.

---

## 4) Observabilitate: cum apare `X-Impact-Cache: HIT|MISS` în răspuns

Avem o variabilă “per request” în `CacheRequestContext`, implementată cu `ThreadLocal`. Aspectul o setează:

- la HIT → `CacheRequestContext.markHit()`
- la MISS → `CacheRequestContext.markMiss()`

Iar la final de request, `CacheStatusHeaderFilter` citește statusul și pune header-ul pe response:

```java
@Component
public class CacheStatusHeaderFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Impact-Cache";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            CacheRequestContext.Status status = CacheRequestContext.getStatus();
            if (status != null) {
                response.setHeader(HEADER_NAME, status.name());
            }
            CacheRequestContext.clear();
        }
    }
}
```

---

## 5) Unde am aplicat caching în proiect (exemplu clar)

În `UserService`:

- `getAllUsers()` este cache-uit:
  - `@ImpactCacheable(prefix = "users:list", ttlSeconds = 300)`
- La modificări care pot schimba lista (consistență), cache-ul este invalidat:
  - `createUser(...)` → `@ImpactCacheEvict(prefix = "users:list", allEntries = true)`
  - `updatePersonalData(...)` → idem
  - `assignRoleToUser(...)` → idem

Idee: **listările se cache-uiesc, modificările dau “evict”**.

Cod exemplu din proiect:

```java
@Transactional
@ImpactCacheEvict(prefix = "users:list", allEntries = true)
public void createUser(CreateUserRequest request) {
    // ... scriere în DB ...
}

@ImpactCacheable(prefix = "users:list", ttlSeconds = 300)
public List<UserDto> getAllUsers() {
    return userRepository.findAll().stream()
            .map(user -> new UserDto(user.getId(), user.getUsername()))
            .collect(Collectors.toList());
}
```

---

## 6) Cum testăm ușor în Postman (fără “bătăi de cap”)

### 4.1 Test HIT/MISS

Pe endpoint-ul cache-uit (ex. `GET /api/users`):

1. primul request:
  - răspuns header: `X-Impact-Cache: MISS`
  - tipic e mai lent (DB + mapping + cache write)
2. al doilea request imediat:
  - răspuns header: `X-Impact-Cache: HIT`
  - tipic e mai rapid (fără DB)

### 4.2 Test TTL (expirare)

1. faci request (cache se umple)
2. aștepți ~5 minute (TTL=300 sec)
3. faci request din nou:
  - ar trebui să fie `MISS` (cache expirat)

### 4.3 Test invalidare (evict)

1. `GET /api/users` (creează cache)
2. `POST /api/users/register` (invalidează cache)
3. `GET /api/users`:
  - `MISS` (se reconstruiește cache)

---

## 7) Fișiere cheie introduse/modificate

### Cache (nou)

- `cache/ImpactCacheable.java`
- `cache/ImpactCacheEvict.java`
- `cache/ImpactCacheAspect.java`
- `cache/CacheKeyGenerator.java`
- `cache/CacheMarshaller.java`
- `cache/JacksonCacheMarshaller.java`
- `cache/CacheClient.java`
- `cache/InMemoryCacheClient.java` (default)

### Observabilitate (Postman)

- `cache/CacheRequestContext.java`
- `cache/CacheStatusHeaderFilter.java` → setează header-ul `X-Impact-Cache`

### Config / build

- `pom.xml` (AOP)
- `application.yaml` (`impact.cache.type`)

---

## 8) Concluzie (de reținut pentru elevi)

Caching-ul “corect” are 5 piese:

- **marshalling** (object <-> bytes)
- **interceptor/decorator** (fără `if` în servicii)
- **TTL** (date proaspete)
- **key generator** (chei stabile, fără coliziuni)

În proiect, elevii pot învăța principiul pe **in-memory cache**.