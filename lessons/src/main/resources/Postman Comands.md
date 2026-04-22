# Ghid complet pentru testarea API-ului prin Postman

Acest ghid descrie ciclul complet de testare: de la înregistrarea utilizatorului până la verificarea token-ului JWT cu date personale.

---

### Pasul 1: Înregistrarea unui utilizator nou

Mai întâi, vom crea un utilizator nou. Acest endpoint este accesibil public.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/users/register`
- **Tab-ul "Authorization":** `No Auth`
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "username": "john.doe",
      "password": "password123",
      "email": "john.doe@example.com"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și mesajul `"User registered successfully!"`.

---

### Pasul 2: Prima autentificare pentru a obține token-ul

Ne vom autentifica pentru a obține primul nostru `accessToken`. La această etapă, token-ul **nu va conține** încă date personale.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/authenticate`
- **Tab-ul "Authorization":** `No Auth`
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "username": "john.doe",
      "password": "password123"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și un JSON cu `accessToken` și `refreshToken`.
  ```json
  {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "a1b2c3d4-e5f6..."
  }
  ```
- **Acțiune:** Copiați valoarea `accessToken`. Veți avea nevoie de ea la pasul următor.

---

### Pasul 3: Adăugarea datelor personale ale utilizatorului

Folosim token-ul obținut pentru a accesa endpoint-ul care salvează datele personale ale utilizatorului.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/users/me/personal-data`
- **Tab-ul "Authorization":**
  - **Type:** `Bearer Token`
  - **Token:** Inserați aici `accessToken`-ul de la **Pasul 2**.
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "firstName": "John",
      "lastName": "Doe",
      "birthDate": "1992-08-12"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și un răspuns cu datele salvate.

---

### Pasul 4: Autentificare repetată pentru a obține token-ul cu date

Acesta este un pas **cheie**. Pentru ca datele personale să apară în token, trebuie să-l generăm din nou (printr-o nouă autentificare).

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/authenticate`
- **Tab-ul "Body"** (folosiți aceleași date ca la Pasul 2):
  ```json
  {
      "username": "john.doe",
      "password": "password123"
  }
  ```
- **Rezultat așteptat:** Veți primi un **NOU** `accessToken`.
- **Acțiune:** Copiați acest **nou** `accessToken`.

---

### Pasul 5: Verificarea conținutului token-ului

Să verificăm că noul token conține într-adevăr toate datele.

1.  Deschideți în browser site-ul **[jwt.io](https://jwt.io/)**.
2.  Inserați **noul `accessToken`** (de la **Pasul 4**) în câmpul din stânga, "Encoded".
3.  Uitați-vă în partea dreaptă, la secțiunea **PAYLOAD: DATA**.

Ar trebui să vedeți acolo toate datele utilizatorului și rolul său:
```json
{
  "userId": 1,
  "role": "ROLE_USER",
  "firstName": "John",
  "lastName": "Doe",
  "birthDate": "1992-08-12",
  "sub": "john.doe",
  "iat": 1678889900,
  "exp": 1678976300
}
```
Dacă vedeți aceste câmpuri, înseamnă că totul funcționează corect.

---

### Pasul 6: Reînnoirea token-ului prin Refresh Token

Să verificăm că `refreshToken` funcționează și returnează un `accessToken` cu datele complete.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/refresh`
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "refreshToken": "inserați_aici_refreshToken_de_la_Pasul_2_sau_4"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și un nou `accessToken`. Îl puteți verifica și pe acesta pe `jwt.io` pentru a vă asigura că conține toate datele.
