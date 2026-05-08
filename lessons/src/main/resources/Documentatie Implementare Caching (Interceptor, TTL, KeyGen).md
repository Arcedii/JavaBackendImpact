# Documentație tehnică: Caching (Interceptor/Decorator), TTL, Key Generator, Marshalling

## Scop

În acest урок am introdus **un mecanism de caching** care poate accelera endpoint-urile “read-heavy” (de exemplu listări), fără să “murdărim” business-logic cu `if (cache...)`.

Concret, în proiect au apărut următoarele blocuri conceptuale:

- **Connection Provider** (pentru Redis, opțional)
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

### 1.2 Mod opțional: `redis` (doar dacă ai Redis instalat local)

Schimbi:

- `impact.cache.type: redis`

și lași setările:

- `spring.data.redis.host`
- `spring.data.redis.port`

Notă: Redis nu e necesar ca să înțelegi lecția; este “backend-ul” real al cache-ului.

---

## 2) Blocurile conceptuale implementate în cod

### 2.1 Connection Provider (pentru Redis)

**De ce există:** conexiunile către Redis trebuie gestionate centralizat (pool/factory).

În proiect:

- `SpringRedisConnectionProvider` expune conexiuni prin `RedisConnectionFactory`.

Rol conceptual:

- ascunde detaliile de conectare
- oferă o singură intrare pentru acces la Redis

### 2.2 Marshalling (Serializare / Deserializare)

**De ce există:** cache-ul stochează `byte[]`/string, dar aplicația lucrează cu obiecte (DTO, liste).

În proiect:

- `CacheMarshaller` definește contractul:
  - `serialize(Object) -> byte[]`
  - `deserialize(byte[], Type) -> T`
- `JacksonCacheMarshaller` implementează contractul folosind `ObjectMapper`.

Detaliu important:

- Deserializarea folosește `**Type`** (nu doar `Class`) ca să funcționeze corect cu tipuri generice precum `List<UserDto>`.

### 2.3 Key Generator

**De ce există:** cache-ul are nevoie de chei unice și predictibile, bazate pe parametrii metodei.

În proiect:

- `CacheKeyGenerator` generează chei în forma:
  - `prefix:arg1:arg2:...`
  - dacă cheia ar deveni prea lungă → folosește fallback `sha256`.

Exemple conceptuale:

- `users:list:static`
- `user_profile:45`
- `products_list:category=shoes:page=2:size=20`

### 2.4 TTL (Time-To-Live)

**De ce există:** să nu livrăm date vechi. Cache-ul trebuie să expire automat.

În proiect:

- `@ImpactCacheable(ttlSeconds = 300)` setează TTL per metodă (ex: 5 minute).
- În `InMemoryCacheClient` TTL se verifică la `get`.
- În `RedisCacheClient` TTL se aplică prin `EXPIRE`.

### 2.5 Interceptor/Decorator (AOP)

**De ce există:** caching-ul trebuie aplicat “din afară”, fără să modificăm logica internă a serviciilor.

În proiect:

- `@ImpactCacheable` – marchează metodele care se cache-uiesc
- `@ImpactCacheEvict` – marchează metodele care invalidează cache-ul
- `ImpactCacheAspect` – “interceptorul” care:
  1. generează cheia
  2. caută în cache
  3. dacă găsește → returnează din cache (**HIT**)
  4. dacă nu găsește → execută metoda reală, apoi salvează în cache (**MISS**)

---

## 3) Unde am aplicat caching în proiect (exemplu clar)

În `UserService`:

- `getAllUsers()` este cache-uit:
  - `@ImpactCacheable(prefix = "users:list", ttlSeconds = 300)`
- La modificări care pot schimba lista (consistență), cache-ul este invalidat:
  - `createUser(...)` → `@ImpactCacheEvict(prefix = "users:list", allEntries = true)`
  - `updatePersonalData(...)` → idem
  - `assignRoleToUser(...)` → idem

Idee: **listările se cache-uiesc, modificările dau “evict”**.

---

## 4) Cum testăm ușor în Postman (fără “bătăi de cap”)

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

## 5) Fișiere cheie introduse/modificate

### Cache (nou)

- `cache/ImpactCacheable.java`
- `cache/ImpactCacheEvict.java`
- `cache/ImpactCacheAspect.java`
- `cache/CacheKeyGenerator.java`
- `cache/CacheMarshaller.java`
- `cache/JacksonCacheMarshaller.java`
- `cache/CacheClient.java`
- `cache/InMemoryCacheClient.java` (default)

### Redis (opțional)

- `cache/RedisConnectionProvider.java`
- `cache/SpringRedisConnectionProvider.java`
- `cache/RedisCacheClient.java`

### Observabilitate (Postman)

- `cache/CacheRequestContext.java`
- `cache/CacheStatusHeaderFilter.java` → setează header-ul `X-Impact-Cache`

### Config / build

- `pom.xml` (AOP + Redis starter)
- `application.yaml` (`impact.cache.type`, `spring.data.redis.`*)

---

## 6) Concluzie (de reținut pentru elevi)

Caching-ul “corect” are 5 piese:

- **conexiune** (dacă e Redis)
- **marshalling** (object <-> bytes)
- **interceptor/decorator** (fără `if` în servicii)
- **TTL** (date proaspete)
- **key generator** (chei stabile, fără coliziuni)

În proiect, elevii pot învăța principiul pe **in-memory cache**, apoi pot comuta ușor pe Redis când sunt gata.