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
- H2 TCP server is exposed on port `9092` for IDE access.
- IntelliJ DB URL (Docker): `jdbc:h2:tcp://localhost:9092//app/data/yomu-db`
