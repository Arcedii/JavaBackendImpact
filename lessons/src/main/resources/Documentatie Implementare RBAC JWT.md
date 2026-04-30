# Documentatie tehnica: RBAC, JWT si gestionarea globala a exceptiilor

## Scop

Aplicatia a fost extinsa pentru a:
- identifice rolul utilizatorului direct din JWT la fiecare request;
- aplice control de acces pe baza de roluri (RBAC);
- returneze raspunsuri JSON clare pentru erori de securitate si erori functionale.

---

## 1. RBAC + JWT

### 1.1 Date incluse in token

La autentificare si refresh se genereaza un `accessToken` care contine:
- `sub` (username),
- `userId`,
- `role` (normalizat cu prefix `ROLE_`),
- optional date personale (`firstName`, `lastName`, `birthDate`).

Exemplu payload:
```json
{
  "userId": 1,
  "role": "ROLE_ADMIN",
  "firstName": "Ion",
  "lastName": "Popescu",
  "sub": "ion.popescu",
  "iat": 1714473000,
  "exp": 1714559400
}
```

### 1.2 Extragerea rolului din token

In `JwtUtil` exista metoda `extractRole()`:
- citeste claim-ul `role`;
- daca rolul nu are prefix `ROLE_`, il adauga automat;
- rezultatul este folosit de filtrul de securitate.

### 1.3 Setarea autoritatilor in contextul Spring Security

In `JwtRequestFilter`:
1. se extrage token-ul din header-ul `Authorization: Bearer ...`;
2. se extrag `username` si `role` din JWT;
3. se valideaza token-ul;
4. rolul este transformat in `SimpleGrantedAuthority`;
5. autenticarea este setata in `SecurityContextHolder`.

Astfel, Spring Security cunoaste drepturile utilizatorului curent pentru fiecare request.

---

## 2. Restrictii de acces

### 2.1 Configurare la nivel de traseu (`SecurityConfig`)

- endpoint-uri publice:
  - `POST /api/authenticate`
  - `POST /api/users/register`
- endpoint-uri admin-only:
  - `/admin/**` necesita `hasRole('ADMIN')`
- restul endpoint-urilor necesita autentificare.

### 2.2 Configurare la nivel de metoda (`@PreAuthorize`)

Exemple implementate:
- `@PreAuthorize("hasRole('ADMIN')")` pentru listarea tuturor utilizatorilor;
- `@PreAuthorize("hasAnyRole('USER', 'ADMIN')")` pentru actualizare date personale;
- endpoint admin dedicat: `GET /admin/health`;
- endpoint admin pentru management roluri: `POST /admin/users/{userId}/role`.

### 2.3 Atribuire roluri de catre administrator

A fost introdus endpoint-ul:
- `POST /admin/users/{userId}/role`
- acces: doar `ROLE_ADMIN`
- body: `{ "role": "ADMIN" }` sau `{ "role": "USER" }`

Comportament:
- sistemul normalizeaza automat valorile (`ADMIN` -> `ROLE_ADMIN`);
- sunt permise doar `ROLE_USER` si `ROLE_ADMIN`;
- rolul utilizatorului este inlocuit cu noul rol selectat;
- utilizatorul trebuie sa se reautentifice pentru a primi token cu rolul actualizat.

---

## 3. Bootstrap admin la startup (Liquibase)

Pentru a elimina problema "primul admin", a fost adaugat un changeset Liquibase care:
- creeaza tabelele `roles` si `user_roles` daca lipsesc;
- insereaza rolul `ROLE_ADMIN` daca nu exista;
- insereaza utilizatorul `admin` daca nu exista;
- leaga utilizatorul `admin` de `ROLE_ADMIN`.

Credentiale implicite:
- `username`: `admin`
- `password`: `password`
- `email`: `admin@impact.local`

Note:
- datele sunt idempotente (nu se dubleaza la restart);
- `spring.jpa.hibernate.ddl-auto` este setat pe `update` ca sa nu se mai stearga tabelele la fiecare pornire.

---

## 4. Exception Handling global

### 4.1 Format unic de eroare

Clasa `ErrorResponse` defineste formatul standard:
```json
{
  "code": "ACCESS_DENIED",
  "message": "Access denied",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

Campuri:
- `code` - cod aplicativ de eroare;
- `message` - mesaj explicit pentru client;
- `timestamp` - momentul producerii erorii (UTC).

### 4.2 Coduri de eroare (`ErrorCode`)

Au fost definite codurile:
- `AUTH_FAILED` -> `401 Unauthorized`
- `ACCESS_DENIED` -> `403 Forbidden`
- `USER_NOT_FOUND` -> `404 Not Found`
- `TOKEN_EXPIRED` -> `401 Unauthorized`

### 4.3 Exceptie custom (`ApiException`)

`ApiException` extinde `RuntimeException` si transporta:
- `ErrorCode`,
- mesaj custom optional.

Este folosita in servicii pentru erori de business cunoscute.

### 4.4 Tratare centrala (`GlobalExceptionHandler`)

`@ControllerAdvice` intercepteaza exceptiile aplicatiei si returneaza `ErrorResponse`:
- `ApiException`
- `ExpiredJwtException`
- `BadCredentialsException`
- `UsernameNotFoundException`
- fallback pentru exceptii neprevazute (`INTERNAL_ERROR`).

### 4.5 Tratare erori de securitate la nivel de filtru

Pentru erori generate in lantul de securitate:
- `RestAuthenticationEntryPoint` -> raspuns `401` (`AUTH_FAILED`);
- `RestAccessDeniedHandler` -> raspuns `403` (`ACCESS_DENIED`).

Acest lucru elimina raspunsurile implicite Spring greu de folosit de frontend.

---

## 5. Beneficii pentru client (frontend/Postman)

- raspunsurile de eroare au format predictibil;
- codurile de eroare sunt stabile si usor de mapat in UI;
- accesul este controlat consecvent pe roluri;
- token-ul contine explicit rolul, deci debugging-ul este mai simplu.

---

## 6. Fisiere cheie modificate

- `config/jwt/JwtUtil.java`
- `config/jwt/JwtRequestFilter.java`
- `config/SecurityConfig.java`
- `controller/UserController.java`
- `controller/AdminController.java`
- `dto/AssignRoleRequest.java`
- `db/changelog/changeset/bootstrap-admin.yaml`
- `db/changelog/db.changelog-master.yaml`
- `application.yaml`
- `exception/ErrorResponse.java`
- `exception/ErrorCode.java`
- `exception/ApiException.java`
- `exception/GlobalExceptionHandler.java`
- `exception/RestAuthenticationEntryPoint.java`
- `exception/RestAccessDeniedHandler.java`

