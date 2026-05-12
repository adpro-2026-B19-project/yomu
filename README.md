# yomu

## Local Development Workflow

1. Install JDK 25.
2. Set environment variables:
   - Optional: set `DB_PASSWORD`, `SESSION_TIMEOUT`, `H2_WEB_ALLOW_OTHERS`
   - Optional Google OAuth: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`
   - Google redirect URI: `http://localhost:8080/login/oauth2/code/google`
3. Run tests:
   - `./gradlew test`
4. Run application:
   - `./gradlew run`
5. Open app:
   - `http://localhost:8080`

## Docker Workflow

1. Build and start:
   - Set a database password in `.env` (`DB_PASSWORD=...`)
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
