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
