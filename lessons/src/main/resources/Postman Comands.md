# Ghid Postman (RBAC + JWT + Exception Handling)

Acest document descrie pașii practici pentru testarea implementării curente:

- autentificare JWT,
- roluri (`ROLE_USER`, `ROLE_ADMIN`),
- endpoint-uri protejate,
- răspunsuri JSON standardizate pentru erori (`401/403/404`).

---

## 1) Cont admin creat automat la startup (Liquibase)

La pornirea aplicatiei se creeaza automat un cont admin implicit (daca nu exista deja):

- **username:** `admin`
- **password:** `password`
- **email:** `admin@impact.local`
- **rol:** `ROLE_ADMIN`

Acest cont este folosit pentru primul login de administrator.

---

## 2) Login și obținere token-uri

### 2.1 Login ca ADMIN (bootstrap)

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/authenticate`
- **Authorization:** `No Auth`
- **Body (raw / JSON):**

```json
{
  "username": "admin",
  "password": "password"
}
```

**Rezultat așteptat:** `200 OK` + `accessToken` cu `ROLE_ADMIN`.

### 2.2 Login utilizator normal

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/authenticate`
- **Authorization:** `No Auth`
- **Body (raw / JSON):**

```json
{
  "username": "john.doe",
  "password": "password123"
}
```

**Rezultat așteptat:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "refreshToken": "uuid-token...."
}
```

Salvati `accessToken` si `refreshToken` pentru pasii urmatori.

---

## 3) Inregistrare utilizator (public)

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/users/register`
- **Authorization:** `No Auth`
- **Body (raw / JSON):**

```json
{
  "username": "john.doe",
  "password": "password123",
  "email": "john.doe@example.com"
}
```

**Rezultat așteptat:** `200 OK`

---

## 4) Verificare payload JWT (rol inclus)

Deschideti [jwt.io](https://jwt.io/) si inserati `accessToken`.

In payload trebuie sa existe cel putin:

```json
{
  "userId": 1,
  "role": "ROLE_USER",
  "sub": "john.doe",
  "iat": 1714473000,
  "exp": 1714559400
}
```

> Observatie: rolul este stocat cu prefixul `ROLE_`, conform Spring Security.

---

## 5) Endpoint protejat pentru utilizator logat

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/users/me/personal-data`
- **Authorization:** `Bearer Token` (`accessToken`)
- **Body (raw / JSON):**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "birthDate": "1992-08-12"
}
```

**Rezultat așteptat:** `200 OK` + datele salvate.

---

## 6) Endpoint admin-only (`/admin/`**)

Endpoint disponibil:

- **Metoda:** `GET`
- **URL:** `http://localhost:8081/admin/health`

### 6.1 Cu token de `ROLE_ADMIN`

**Rezultat așteptat:** `200 OK`

```json
"Admin access granted"
```

### 6.2 Cu token de `ROLE_USER`

**Rezultat așteptat:** `403 Forbidden`

```json
{
  "code": "ACCESS_DENIED",
  "message": "Access denied",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

---

## 7) Atribuire rol utilizator (doar ADMIN)

Acum exista endpoint dedicat pentru schimbarea rolului unui utilizator.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/admin/users/{userId}/role`
- **Exemplu URL:** `http://localhost:8081/admin/users/2/role`
- **Authorization:** `Bearer Token` (token de `ROLE_ADMIN`)
- **Body (raw / JSON):**

```json
{
  "role": "ADMIN"
}
```

Valori permise pentru `role`:

- `USER`
- `ADMIN`
- `ROLE_USER`
- `ROLE_ADMIN`

**Rezultat așteptat:** `200 OK`

```json
"Rolul a fost actualizat cu succes"
```

Dupa schimbarea rolului, utilizatorul tinta trebuie sa faca login din nou pentru a primi JWT cu rolul nou.

---

## 8) Refresh token

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/refresh`
- **Authorization:** `No Auth`
- **Body (raw / JSON):**

```json
{
  "refreshToken": "inserați_refresh_token_aici"
}
```

**Rezultat așteptat:** `200 OK` + un nou `accessToken`.

---

## 9) Teste de eroare (format JSON unificat)

### 9.1 Fara token pe endpoint securizat

Ex: `GET /api/users`

```json
{
  "code": "AUTH_FAILED",
  "message": "Authentication failed",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

### 9.2 Token expirat

```json
{
  "code": "TOKEN_EXPIRED",
  "message": "JWT token has expired",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

### 9.3 Utilizator inexistent

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

### 9.4 Rol invalid la endpoint-ul de admin

```json
{
  "code": "AUTH_FAILED",
  "message": "Role must be USER or ADMIN",
  "timestamp": "2026-04-30T10:00:00Z"
}
```

---

## 10) Cache (GET `/api/users`) — header-e în Postman

După request, deschide tab-ul **Headers** la răspuns (nu Body). Vei vedea:

| Header | Exemplu | Semnificație |
|--------|---------|--------------|
| `X-Impact-Cache` | `HIT` / `MISS` | rezultatul din cache sau recalculat |
| `X-Impact-Cache-Backend` | `InMemoryCacheClient` sau `RedisCacheClient` | ce implementare rulează (`impact.cache.type` memory vs redis) |

**Notă:** `GET /api/users` necesită token **ADMIN** (`Authorization: Bearer ...`).
