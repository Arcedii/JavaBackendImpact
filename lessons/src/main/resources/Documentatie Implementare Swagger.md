# Documentație: implementarea Swagger (OpenAPI) în proiectul „lessons”

Acest fișier descrie **doar** integrarea Swagger din proiectul nostru: ce am adăugat, cum funcționează și cum se testează. Pentru cache, JWT detaliat sau Postman pas cu pas, vezi codul sursă și `Postman Comands.md`.

---

## 1. Ce este Swagger?

**Swagger** este un set de instrumente pentru **documentarea** și **testarea** API-urilor REST din browser, fără Postman.

În proiectul nostru (Spring Boot **3.2**) nu folosim biblioteca veche **Springfox**. Folosim **springdoc-openapi**, care generează automat specificația **OpenAPI 3** din controller-e și DTO-uri.


| Ce obții                                               | URL (aplicația rulează pe portul **8081**)                                     |
| ------------------------------------------------------ | ------------------------------------------------------------------------------ |
| **Swagger UI** — interfață vizuală, buton „Try it out” | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **OpenAPI JSON** — schema completă (export, integrări) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)         |


---

## 2. Ce am implementat în proiect (rezumat)


| Piesă                        | Locație                            | Rol                                                                      |
| ---------------------------- | ---------------------------------- | ------------------------------------------------------------------------ |
| Dependență **springdoc**     | `pom.xml`                          | UI + generare OpenAPI                                                    |
| `**OpenApiConfig`**          | `config/OpenApiConfig.java`        | Titlu API, contact, schema JWT **Bearer**                                |
| **Permisiuni Security**      | `config/SecurityConfig.java`       | Swagger UI și `/v3/api-docs` accesibile **fără** login                   |
| **Setări UI**                | `application.yaml` → `springdoc.`* | Căi, sortare, Try it out                                                 |
| **Adnotări pe controller-e** | `controller/`*                     | Grupuri (`@Tag`), descrieri (`@Operation`), JWT (`@SecurityRequirement`) |


Constanta partajată pentru securitate: `OpenApiConfig.BEARER_AUTH` = `"bearerAuth"`.

---

## 3. Dependența Maven

În `lessons/pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

Pachetul include atât generarea documentației (`/v3/api-docs`), cât și interfața **Swagger UI**.

---

## 4. Configurarea OpenAPI — `OpenApiConfig.java`

Clasa declară un bean `OpenAPI` cu:

- **Info:** titlu `Impact Lessons API`, descriere, versiune `v1`, contact Impact Academy;
- **Security scheme `bearerAuth`:** tip HTTP, scheme `bearer`, format `JWT`;
- **Descriere scheme:** tokenul se obține de la `POST /api/authenticate`.

**Important:** nu punem securitate globală pe tot API-ul. Endpoint-urile **publice** (login, register) rămân fără lacăt în UI; endpoint-urile protejate sunt marcate explicit cu `@SecurityRequirement(name = "bearerAuth")` pe controller sau metodă.

---

## 5. Spring Security — acces la Swagger UI

În `SecurityConfig`, înainte de `anyRequest().authenticated()`:

```java
.requestMatchers(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**"
).permitAll()
```

Fără aceste reguli, browserul primește **401** la încărcarea UI-ului, deoarece aplicația folosește JWT pe toate celelalte rute.

**Swagger UI ≠ API protejat:** UI-ul e public; apelurile „Try it out” către `/api/`** respectă în continuare JWT și rolurile (`@PreAuthorize`).

---

## 6. Setări în `application.yaml`

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method    # sortare operații: GET, POST, ...
    tags-sorter: alpha           # tag-uri în ordine alfabetică
    try-it-out-enabled: true     # butonul „Try it out” activ
  show-actuator: false           # nu afișăm endpoint-uri Actuator
```

### Alte opțiuni utile (nu sunt în proiect acum, dar le poți adăuga)


| Proprietate                  | Exemplu                         | Efect                                       |
| ---------------------------- | ------------------------------- | ------------------------------------------- |
| `springdoc.packages-to-scan` | `com.impact.lessons.controller` | Scanează doar anumite pachete               |
| `springdoc.paths-to-match`   | `/api/**`, `/admin/**`          | Exclude alte căi din documentație           |
| `springdoc.api-docs.enabled` | `false`                         | Dezactivează JSON (recomandat în producție) |


---

## 7. Adnotări pe controller-e (implementarea noastră)

Folosim pachetul `io.swagger.v3.oas.annotations.*`.


| Adnotare                                    | Unde             | Ce face în UI                               |
| ------------------------------------------- | ---------------- | ------------------------------------------- |
| `@Tag(name, description)`                   | Clasă controller | Secțiune / grup de endpoint-uri             |
| `@Operation(summary, description)`          | Metodă           | Titlu și text la expandarea operației       |
| `@SecurityRequirement(name = "bearerAuth")` | Metodă sau clasă | Afișează lacăt; cere token la **Authorize** |


### 7.1. `AuthenticationController` — tag **Autentificare**


| Metodă | Cale                | JWT în Swagger | Rol real (Security)          |
| ------ | ------------------- | -------------- | ---------------------------- |
| POST   | `/api/authenticate` | Nu             | Public                       |
| POST   | `/api/refresh`      | Nu             | Autentificat (token în body) |


### 7.2. `UserController` — tag **Utilizatori**


| Metodă | Cale                          | JWT în Swagger    | Rol (`@PreAuthorize`) |
| ------ | ----------------------------- | ----------------- | --------------------- |
| POST   | `/api/users/register`         | Nu                | Public                |
| GET    | `/api/users`                  | Da (`bearerAuth`) | `ADMIN`               |
| POST   | `/api/users/me/personal-data` | Da                | `USER` sau `ADMIN`    |


La `GET /api/users`, răspunsul poate include header-e de cache (`X-Impact-Cache`, `X-Impact-Cache-Backend`) — acestea **nu** sunt configurate în Swagger; vin din implementarea de caching, nu din springdoc.

### 7.3. `AdminController` — tag **Admin**

La nivel de clasă: `@SecurityRequirement(name = "bearerAuth")` — toate operațiile cer JWT în UI.


| Metodă | Cale                         | Rol     |
| ------ | ---------------------------- | ------- |
| GET    | `/admin/health`              | `ADMIN` |
| POST   | `/admin/users/{userId}/role` | `ADMIN` |


### 7.4. Ce nu apare explicit în adnotări

- `**HelloController`** (`GET /`) — springdoc îl poate include automat în listă; nu are `@Tag` / `@Operation` (nu face parte din lecția Swagger API REST).
- **Scheme JSON** pentru body/response — generate automat din DTO-uri (`CreateUserRequest`, `UserDto`, `AuthenticationRequest`, `AuthenticationResponse`, etc.).

---

## 8. Flux de testare în Swagger UI (pas cu pas)

1. Pornește aplicația (`LessonsApplication`, port **8081**).
2. Deschide: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
3. Secțiunea **Autentificare** → `POST /api/authenticate` → **Try it out**:
  ```json
   {
     "username": "admin",
     "password": "password"
   }
  ```
   (cont admin creat de Liquibase la startup)
4. Din răspuns, copiază câmpul `**accessToken**` (nu `refreshToken` pentru Authorize).
5. Apasă butonul **Authorize** (sus-dreapta) → lipește **doar** token-ul (fără prefix `Bearer` ; springdoc îl adaugă).
6. Testează operații cu lacăt, ex.:
  - **GET /api/users** — necesită token **ADMIN**;
  - **POST /api/users/me/personal-data** — token USER sau ADMIN;
  - **GET /admin/health** — token ADMIN.

Dacă primești **401** sau **403**, tokenul lipsește, e expirat sau rolul nu e suficient — la fel ca în Postman.

### Înregistrare utilizator nou (fără JWT)

`POST /api/users/register` → body exemplu:

```json
{
  "username": "user1",
  "email": "user1@test.local",
  "password": "password"
}
```

---

## 9. Relația Swagger ↔ JWT ↔ roluri

```
Browser (Swagger UI)  →  permitAll pentru /swagger-ui*, /v3/api-docs*
Try it out /api/*     →  JwtRequestFilter + @PreAuthorize (ca Postman)
```

Swagger **nu înlocuiește** securitatea: doar documentează ce header trebuie trimis (`Authorization: Bearer <accessToken>`) pentru endpoint-urile marcate cu `@SecurityRequirement`.

---

## 10. Fișiere modificate pentru Swagger


| Fișier                                     | Modificare                                            |
| ------------------------------------------ | ----------------------------------------------------- |
| `pom.xml`                                  | dependență `springdoc-openapi-starter-webmvc-ui`      |
| `config/OpenApiConfig.java`                | bean `OpenAPI` + schema `bearerAuth`                  |
| `config/SecurityConfig.java`               | `permitAll` pentru căile Swagger                      |
| `application.yaml`                         | bloc `springdoc:`                                     |
| `controller/AuthenticationController.java` | `@Tag`, `@Operation`                                  |
| `controller/UserController.java`           | `@Tag`, `@Operation`, `@SecurityRequirement` selectiv |
| `controller/AdminController.java`          | `@Tag`, `@SecurityRequirement` la clasă, `@Operation` |


---

## 11. Lecție — cele 3 întrebări din curs

1. **Ce este Swagger?** — Documentație și testare interactivă a API REST; la noi = OpenAPI 3 + springdoc + Swagger UI.
2. **Cum l-am integrat în backend?** — Dependență Maven, `OpenApiConfig`, excepții în `SecurityConfig`, adnotări pe controller-e, YAML.
3. **Cum se configurează și ce opțiuni are?** — În principal `springdoc.`* din `application.yaml`; opțional scan pachete, dezactivare în producție.

---

## 12. Producție (recomandare)

În mediu live, de obicei **dezactivezi** documentația publică:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

sau folosești profil Spring `dev` / `prod` astfel încât Swagger să ruleze doar la dezvoltare.