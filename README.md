# yomu

## Local Development Workflow

1. Install JDK 25.
2. Run tests:
   - `./gradlew test`
3. Run application:
   - `./gradlew run`
4. Open app:
   - `http://localhost:8080`

## Docker Workflow

1. Build and start:
   - Set a database password:
     - PowerShell: `$env:DB_PASSWORD="replace-with-strong-password"`
   - `docker compose -f compose.yml up --build -d`
2. See logs:
   - `docker compose -f compose.yml logs -f app`
3. Stop containers:
   - `docker compose -f compose.yml down`
4. Stop and remove volume (reset DB):
   - `docker compose -f compose.yml down -v`

## Notes

- Docker runtime uses Spring profile `docker`.
- Docker profile config is in `src/main/resources/application-docker.properties`.
- H2 data persists in docker volume `yomu_data`.
- H2 TCP server is disabled by default in Docker (`app.h2.tcp.enabled=false`).
- Session inactivity timeout defaults to `15m` and can be overridden with `SESSION_TIMEOUT`.

## Kalfin Milestone 25% (Interaksi & Liga)

- Requires authenticated session.
- Web:
  - `GET /clans` or `GET /interaction` -> clan list page.
  - `POST /clans` -> create clan from form.
- API:
  - `GET /api/clans` -> list all clans.
  - `POST /api/clans` -> create clan (JSON body: `{"name":"..."}`).

## Nisrina Alya Milestone 25% (Bacaan & Kuis)

- Endpoint GET Daftar Bacaan - TextController.java
- Endpoint GET Detail Teks	- TextController.java
- Endpoint POST Buat Teks (Admin) - AdminTextController.java
- Entitas Database (Text, Kategori, Soal)	- Text.java, Question.java
- Halaman Thymeleaf (List & Detail)	- texts.html, text-detail.html
- Minimal 1 Unit Test - QuestionTest.java, ReadingIntegrationTest.java

Web (Frontend MVC):
- GET /texts $\rightarrow$ Halaman daftar semua bacaan (Reading List).
- GET /texts/{id} $\rightarrow$ Halaman detail untuk membaca satu teks spesifik.
- GET /admin/texts/create $\rightarrow$ Halaman Form Admin untuk membuat teks & soal kuis baru.
- POST /admin/texts $\rightarrow$ Proses menyimpan data Teks, Kategori, Soal, dan Opsi Jawaban dari form.

Service Logic (Backend):
- createText(...) $\rightarrow$ Logika transaksional menyimpan Teks + Kategori + UserID.
- getAllTexts() $\rightarrow$ Mengambil semua teks yang statusnya published.