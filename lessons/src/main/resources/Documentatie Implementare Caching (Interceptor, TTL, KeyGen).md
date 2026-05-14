# Documentație: implementarea caching-ului în proiectul „lessons”

Acest document descrie **ce am introdus în cod**, **cum funcționează** și **cum se testează**, pe înțelesul unei persoane care știe Java/Spring la nivel de laborator.

---

## 1. Ce problemă rezolvăm

Endpoint-urile care **citesc mult** din baza de date (ex.: listă de utilizatori) pot fi lente dacă rulează aceeași interogare la fiecare request.

**Caching-ul** păstrează rezultatul unei metode într-un depozit rapid (memorie sau Redis), astfel încât următoarele apeluri cu aceiași parametri pot returna răspunsul **fără** să mai lovească baza de date.

**Cerință de design:** nu vrem să umplem serviciile cu `if (există în cache) { ... }`. De aceea caching-ul este aplicat **în jurul** metodei, prin **AOP** (un aspect care interceptează apelul).

---

## 2. Ce am implementat (rezumat)


| Piesă                                            | Rol                                                                                     |
| ------------------------------------------------ | --------------------------------------------------------------------------------------- |
| `**@ImpactCacheable`**                           | Marchează o metodă a cărei **valoare returnată** se poate citi din cache (GET logic).   |
| `**@ImpactCacheEvict`**                          | După o metodă care **modifică date**, golește intrările din cache (consistență).        |
| `**ImpactCacheAspect`**                          | Aspect AOP: calculează cheia, citește/scrie cache, marchează HIT/MISS, execută evict.   |
| `**CacheClient**` + implementări                 | Abstracție peste stocare: `**InMemoryCacheClient**` sau `**RedisCacheClient**`.         |
| `**CacheMarshaller` / `JacksonCacheMarshaller**` | Transformă obiecte ↔ `byte[]` (Jackson).                                                |
| `**CacheKeyGenerator**`                          | Construiește chei stabile din `prefix` + argumente metodei.                             |
| `**CacheRequestContext**`                        | Ține pe thread starea **HIT/MISS** și numele backend-ului (pentru header-e).            |
| `**CacheResponseHeadersAdvice`**                 | Pune în răspuns header-ele **înainte** de serializarea JSON (important pentru Postman). |
| `**CacheStatusHeaderFilter`**                    | La final de request **curăță** `ThreadLocal`-ul (fără scurgeri între request-uri).      |
| **Config `impact.cache.type`**                   | Comută între `**memory**` și `**redis**` (Memurai/Redis pe `localhost:6379`).           |
| **Loguri (`slf4j`)**                             | La pornire: ce backend e activ; la DEBUG: HIT/MISS/PUT/EVICT cu numele backend-ului.    |


**Nu folosim** adnotările standard Spring `@Cacheable` / `@CacheEvict` din modulul Spring Cache; avem **mecanism propriu**, dar ideile sunt aceleași.

---

## 3. Dependențe și configurare

### 3.1. Maven (`pom.xml`)

- `**spring-boot-starter-aop`** — pentru `@Aspect` și `@Around`.
- `**spring-boot-starter-data-redis**` — pentru conexiune Redis când `impact.cache.type: redis`.

### 3.2. `application.yaml`

- `**impact.cache.type**`
  - `**memory**` — totul rămâne în JVM (`ConcurrentHashMap`), nu ai nevoie de Redis.
  - `**redis**` — datele merg în **Memurai/Redis**; trebuie server pornit pe `spring.data.redis.host` / `port` (implicit `localhost:6379`).

Spring creează **un singur** bean `CacheClient` activ (condiționat de proprietate).

### 3.3. Teste automate (`src/test/resources/application.properties`)

Pentru `mvn test` **nu** cerem Redis: există `impact.cache.type=memory` și excludere auto-config Redis, ca contextul Spring să pornească fără Memurai.

---

## 4. Cum arată fluxul unei cereri (GET cache-uit)

Exemplu: `UserController` apelează `UserService.getAllUsers()` (metodă marcată cu `@ImpactCacheable`).

1. **Înainte** de corpul metodei, `**ImpactCacheAspect`** calculează **cheia** (`CacheKeyGenerator`).
2. Citește din `**CacheClient.get(key)`**:
  - dacă există valoare validă (neexpirată) → deserializare → return; `**CacheRequestContext.markHit()**`;
  - altfel → `**markMiss()**`, rulează metoda reală (DB), serializare, `**CacheClient.set(..., ttl)**`.
3. La scrierea răspunsului JSON, `**CacheResponseHeadersAdvice**` citește `CacheRequestContext` și setează header-ele (vezi secțiunea 7).
4. La final, `**CacheStatusHeaderFilter**` apelează `**CacheRequestContext.clear()**`.

**Evict:** pentru metode cu `@ImpactCacheEvict`, aspectul face `**proceed()`** (scriere DB) **mai întâi**; doar dacă reușește, șterge din cache (`delete` sau `deleteByPrefix`). Astfel nu pierzi cache-ul dacă tranzacția eșuează.

---

## 5. Chei (Key Generator)

Reguli practice (în `CacheKeyGenerator`):

- Fără argumente → cheia este `**prefix:static`** (ex. `users:list:static`).
- Cu argumente → `**prefix:val1:val2:...**` (argumentele sunt normalizate: spații, max 80 caractere).
- Dacă cheia devine prea lungă (> 256 caractere) → variantă `**prefix:sha256:...**`.

**Important:** două apeluri cu aceiași parametri trebuie să producă **aceeași** cheie.

---

## 6. TTL (timp de viață)

TTL-ul este setat **per metodă** în `@ImpactCacheable(ttlSeconds = ...)`.

- **Memory:** la `set` se salvează momentul expirării; la `get`, dacă a expirat, intrarea se șterge și se tratează ca „lipsă” (comportament MISS la următorul nivel).
- **Redis:** după `SET`, clientul aplică `**EXPIRE`** pe aceeași cheie (secunde).

---

## 7. Header-e în Postman (răspuns, nu request)

După trimiterea request-ului, în Postman deschide tab-ul **Headers** la **Response** (nu la request).


| Header                       | Exemple                                    | Semnificație                                                                            |
| ---------------------------- | ------------------------------------------ | --------------------------------------------------------------------------------------- |
| `**X-Impact-Cache`**         | `HIT` / `MISS`                             | `HIT` = răspunsul a venit din cache; `MISS` = s-a recalculat și s-a (re)scris cache-ul. |
| `**X-Impact-Cache-Backend**` | `InMemoryCacheClient` / `RedisCacheClient` | Ce implementare `CacheClient` rulează acum.                                             |


**De ce nu punem header-ele doar într-un `Filter`?**  
Pentru `@RestController`, corpul JSON este scris adesea **înainte** ca `Filter`-ul să poată seta header-e în `finally`; răspunsul poate fi deja „committed”. De aceea header-ele se pun în `**ResponseBodyAdvice.beforeBodyWrite`**. Filtrul rămâne util pentru **curățarea** `ThreadLocal`.

---

## 8. Loguri (consolă / IDE)

- **INFO la pornire:** mesaje din `InMemoryCacheClient` / `RedisCacheClient` și din `ImpactCacheAspect` (ce client e legat).
- **DEBUG în timpul rulării:** în `ImpactCacheAspect` — HIT/MISS/PUT/EVICT cu `backend=...`.

Nivelul poate fi reglat în `application.yaml` sub `logging.level.com.impact.lessons.cache`.

---

## 9. Unde e folosit în aplicație (exemplu real)

În `**UserService`**:

- `**getAllUsers()**` — `@ImpactCacheable(prefix = "users:list", ttlSeconds = 300)`.
- `**createUser**`, `**updatePersonalData**`, `**assignRoleToUser**` — `@ImpactCacheEvict(prefix = "users:list", allEntries = true)` ca lista din cache să nu rămână în urmă față de DB.

În `**UserController**`, listarea este expusă ca `**GET /api/users**`, dar este protejată: ai nevoie de **JWT cu rol `ADMIN`** (`@PreAuthorize`).

---

## 10. Cum testezi în Postman (pași scurți)

1. **Login admin** (cont bootstrap din Liquibase — vezi `Postman Comands.md`): `POST http://localhost:8081/api/authenticate`.
2. `**GET http://localhost:8081/api/users`** cu header `Authorization: Bearer <token>`.
3. Repetă imediat același GET:
  - primul răspuns: de obicei `**X-Impact-Cache: MISS**`;
  - al doilea: de obicei `**X-Impact-Cache: HIT**`.
4. **Evict:** `POST /api/users/register` (sau alt flux care lovește o metodă cu `@ImpactCacheEvict`), apoi din nou `GET /api/users` → așteptat `**MISS`**.
5. **TTL:** așteaptă `ttlSeconds` (ex. 300 secunde), apoi `GET` → așteptat `**MISS`**.

---

## 11. Redis / Memurai (Windows)

Memurai este compatibil protocol Redis, de obicei pe `**127.0.0.1:6379**`.

1. Pornește serviciul Memurai.
2. Setează în `application.yaml`: `**impact.cache.type: redis**` și verifică `spring.data.redis.*`.
3. Repornește aplicația. În Postman, `**X-Impact-Cache-Backend**` trebuie să fie `**RedisCacheClient**`.

Pentru depanare în dev poți folosi `redis-cli` (`PING`, `KEYS users:list*`, `MONITOR` — cu grijă, `MONITOR` e zgomotos).

---

## 12. Legătura cu „Spring Cache” (examene) — ce face fiecare adnotare

În **Spring Framework**, modulul **Spring Cache** oferă adnotări care lucrează cu un **`CacheManager`** (cache în memorie, Redis etc., după configurare). **În proiectul nostru** nu folosim aceste adnotări, dar la examen trebuie să știi **ce face fiecare** și **cum se mapează** la `@ImpactCacheable` / `@ImpactCacheEvict`.

---

### 12.1. `@Cacheable` (Spring)

**Ce face:** marchează o metodă a cărei **valoare returnată** poate fi citită din cache.

**Pași tipici:**

1. Spring calculează o **cheie** (nume cache + parametri sau `keyGenerator`).
2. Dacă pentru cheie **există** valoare în cache → returnează din cache și **sari peste** execuția metodei (în varianta clasică).
3. Dacă **nu există** → execută metoda, pune rezultatul în cache (TTL dacă e configurat la nivel de cache), returnează rezultatul.

**Parametri des întâlniți:** `cacheNames` / `value`, `key`, `keyGenerator`, opțional `condition` / `unless`.

**La noi:** `@ImpactCacheable` + `ImpactCacheAspect` — același tip de idee: **get din cache → la miss `proceed()` + set** cu `ttlSeconds`.

---

### 12.2. `@CacheEvict` (Spring)

**Ce face:** marchează o metodă la care vrei să **ștergi** intrări din cache (invalidare), ca să nu mai servești date **depășite** după ce ai modificat baza de date.

**Când se folosește:** după insert/update/delete sau orice operație care face ca răspunsurile cache-uite anterior să fie **false**.

**Opțiuni uzuale:**

- **`allEntries = true`** — golește **tot** cache-ul indicat (toate cheile din acel „nume”).
- **`key`** — ștergi **o** intrare anume.
- **`beforeInvocation`** — evict **înainte** de metodă (implicit e după; la noi mereu **după** `proceed()` reușit).

**La noi:** `@ImpactCacheEvict` + aspect: **întâi** rulează metoda (DB), **apoi** `delete` sau `deleteByPrefix` după `prefix` și `allEntries`.

---

### 12.3. `@CachePut` (Spring)

**Ce face:** **execută mereu** metoda (nu sare peste ea din cauza cache-ului) și, cu rezultatul obținut, **scrie/actualizează** mereu cache-ul.

**Diferență clară față de `@Cacheable`:**

| | `@Cacheable` | `@CachePut` |
|---|--------------|-------------|
| Rulează metoda dacă există în cache? | De obicei **nu** | **Da**, mereu |
| Scrie în cache după execuție? | Da (la miss) | **Da**, mereu |

**Exemplu de intenție:** „Vreau mereu ultima valoare calculată în cache, chiar dacă exista deja ceva acolo.”

**La noi:** nu avem `@ImpactCachePut`. Un **refresh** apropiat: apel care dă **MISS** (cheie absentă sau expirată) → metoda rulează → rezultatul se scrie în cache. Dar dacă e **HIT**, metoda **nu** rulează — deci **nu** e identic cu `@CachePut`.

---

### 12.4. `@Caching` (Spring)

**Ce face:** permite pe **aceeași metodă** să grupezi **mai multe** acțiuni: mai multe `@Cacheable`, `@CacheEvict`, `@CachePut` într-un singur loc (un array de operații).

**Exemplu de idee:** „La această metodă: șterge din cache `users` și `stats`, dar salvează rezultatul în `lastResult`.”

**La noi:** nu există o singură adnotare echivalentă. Gruparea se face practic prin **mai multe metode** (citire cu `@ImpactCacheable`, scrieri cu `@ImpactCacheEvict`) sau prin extinderea aspectului — în lecție nu e implementat `@Caching`.

---

### 12.5. Tabel rezumat (Spring → Impact)

| Spring | Pe scurt | La noi |
|--------|----------|--------|
| `@Cacheable` | Citește cache; la miss execută metoda și pune rezultatul. | `@ImpactCacheable` + `ImpactCacheAspect` |
| `@CacheEvict` | Șterge din cache (invalidare). | `@ImpactCacheEvict` + `ImpactCacheAspect` |
| `@CachePut` | Execută mereu metoda și rescrie cache-ul. | Fără adnotare dedicată; similar parțial doar la flux MISS + scriere |
| `@Caching` | Mai multe operații de cache pe o metodă. | Fără echivalent; mai multe metode / adnotări |

---

### 12.6. Invalidare la modificare (în proiect)

Metodele care **scriu** în DB și afectează lista afișată (`createUser`, `updatePersonalData`, `assignRoleToUser`) au `@ImpactCacheEvict(prefix = "users:list", allEntries = true)` — se șterg toate cheile care încep cu `users:list:`, ca următorul `getAllUsers` să nu folosească o listă veche.

---

### 12.7. TTL diferit per „cache” (în proiect)

În Spring Cache, TTL-ul e adesea la **nivel de configurație** a cache-ului. **La noi** e **per metodă**: `@ImpactCacheable(..., ttlSeconds = 300)` vs altă metodă cu `ttlSeconds = 60` — fără schimbări suplimentare de cod.

---

## 13. Lista fișierelor relevante (pachet `com.impact.lessons.cache`)

- `ImpactCacheable.java`, `ImpactCacheEvict.java`
- `ImpactCacheAspect.java`
- `CacheClient.java`
- `InMemoryCacheClient.java`
- `RedisConnectionProvider.java`, `SpringRedisConnectionProvider.java`, `RedisCacheClient.java`
- `CacheMarshaller.java`, `JacksonCacheMarshaller.java`
- `CacheKeyGenerator.java`
- `CacheRequestContext.java`
- `CacheResponseHeadersAdvice.java`
- `CacheStatusHeaderFilter.java`

**Config:** `application.yaml`, `pom.xml`  
**Exemplu de utilizare:** `UserService.java`  
**Teste:** `src/test/resources/application.properties`

---

## 14. Concluzie

Am introdus un **caching declarativ** (adnotări proprii + AOP), cu **TTL**, **chei deterministe**, **marshalling JSON**, **două backend-uri** (memorie / Redis), **observabilitate** în Postman (header-e) și **loguri** pentru depanare. Comutarea între moduri se face prin `**impact.cache.type`**, fără să rescrii logica din servicii.

---

## 15. Explicație

### De ce avem nevoie de cache?

Dacă la **fiecare** cerere pentru **lista utilizatorilor** mergem în **bază de date**, construim lista și trimitem răspunsul, sub sarcină mare totul devine **lent** și baza e **solicitată** inutil.

**Cache-ul** e ca o **raftă rapidă**: o dată „costisitor” calculăm răspunsul, îl **punem pe raft**. La următoarea cerere **identică**, îl **luăm de pe raft** și **nu mai interogăm** baza. Asta înseamnă răspuns mai rapid și mai puțină presiune pe DB.

### Cum am făcut fără să încurcăm serviciul cu `if (cache...)`

Nu am scris în `UserService` o grămadă de condiții.

Am pus **etichete** pe metode:

- `**@ImpactCacheable`** — „rezultatul **acestei** metode se poate salva în cache” (ex.: lista utilizatorilor).
- `**@ImpactCacheEvict`** — „**după** această metodă trebuie să **golesc** cache-ul”, pentru că în baza de date **s-a schimbat** ceva (înregistrare user, roluri etc.).

Un alt fișier, `**ImpactCacheAspect`**, e ca un **paznic la ușa** metodei (AOP):

1. Calculează o **cheie** din prefix + argumente (la listă fără parametri e ceva de forma `users:list:static`).
2. Caută în depozit:
  - **găsește** → returnează din cache; în header apare `**HIT`** („am luat de pe raft”).
  - **nu găsește** → rulează metoda reală (DB), pune rezultatul în cache cu **TTL**; apare `**MISS`** („nu era pe raft, tocmai l-am pus”).

Logica de business rămâne în serviciu; cache-ul e **în jurul** ei.

### Două „rafturi” (unde ținem datele)

În `application.yaml`, `**impact.cache.type`**:

- `**memory**` — raftul e **în memoria aplicației** (JVM). Simplu la laborator, **nu** ai nevoie de Redis. Minus: la **restart** aplicației raftul se golește; dacă ai **mai multe instanțe**, fiecare are propriul raft.
- `**redis`** (Memurai e compatibil Redis) — raftul e **un server separat**. Util când vrei **același** cache pentru mai multe copii ale aplicației. Trebuie ca **Memurai/Redis să ruleze** (de obicei `localhost:6379`).

În Postman, la **răspuns**, uită-te la `**X-Impact-Cache-Backend`**: îți spune ce „raft” a rulat — `InMemoryCacheClient` sau `RedisCacheClient`.

### Ce înseamnă header-ele (tab **Headers** la **Response**, nu la request)

- `**X-Impact-Cache: MISS`** — răspunsul **nu** a venit din cache (de obicei primul request după start sau după ce s-a invalidat cache-ul).
- `**X-Impact-Cache: HIT`** — răspunsul **a venit** din cache (de obicei al doilea request identic imediat).

**TTL** (`ttlSeconds` pe `@ImpactCacheable`) — după câte **secunde** intrarea de pe raft „expiră” singură, ca să nu dăm la infinit o listă veche.

**Evict** — când cineva **scrie** în DB (ex. `register`), ștergem cache-ul listei, ca următorul `GET` să fie din nou `**MISS`** și să citească date **proaspete**.

### De ce nu punem header-ele doar într-un filtru la final?

Pentru `@RestController`, JSON-ul poate fi deja **trimis** către client, iar atunci e **prea târziu** să mai adaugi header-e în `finally` la filtru. De aceea header-ele se pun în `**ResponseBodyAdvice`** (înainte de scrierea body-ului). Filtrul de la final doar **curăță** variabilele pe thread, ca să nu se amestece între cereri.

### O singură propoziție de reținut

**Cache = memorăm temporar un răspuns scump; HIT = l-am luat de pe raft; MISS = l-am recalculat și l-am pus pe raft; evict = am uitat raftul pentru că datele din DB s-au schimbat; comutatorul raftului = `impact.cache.type`.**