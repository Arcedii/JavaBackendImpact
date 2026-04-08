# Instrucțiuni pentru demonstrarea funcționalității aplicației prin Postman

## Pasul 1: Înregistrarea unui utilizator nou

Creăm un utilizator nou. Acest endpoint este accesibil public.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/users/register`
- **Tab-ul "Authorization":** `No Auth`
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "username": "testuser",
      "password": "password123",
      "email": "test@example.com"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și mesajul `"User registered successfully!"`.

---

## Pasul 2: Autentificare (login) și obținerea token-ului

Obținem un token JWT folosind datele utilizatorului creat.

- **Metoda:** `POST`
- **URL:** `http://localhost:8081/api/authenticate`
- **Tab-ul "Authorization":** `No Auth`
- **Tab-ul "Body"** (tip `raw`, format `JSON`):
  ```json
  {
      "username": "testuser",
      "password": "password123"
  }
  ```
- **Rezultat așteptat:** Status `200 OK` și un JSON cu token-ul.
  ```json
  {
      "jwt": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6..."
  }
  ```
- **Acțiune:** Copiați valoarea `jwt` primită. Veți avea nevoie de ea la pasul următor.

---

## Pasul 3: Accesarea unei resurse protejate

Verificăm că, având token-ul obținut, putem accesa date protejate (lista tuturor utilizatorilor).

- **Metoda:** `GET`
- **URL:** `http://localhost:8081/api/users`
- **Tab-ul "Authorization":**
  - **Type:** `Bearer Token`
  - **Token:** Inserați aici token-ul copiat la Pasul 2.
- **Rezultat așteptat:** Status `200 OK` și o listă de utilizatori în format JSON.

---

### (Opțional) Pasul 4: Verificarea securității

Puteți repeta Pasul 3, dar în tab-ul "Authorization" să selectați `No Auth`. Acest lucru va demonstra că, fără token, serverul va returna o eroare `403 Forbidden`.
