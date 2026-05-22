## Yomu Project Overview

Yomu is a gamified literacy learning platform built for the Advanced Programming group project. The application helps users read curated texts, complete quizzes, unlock achievements, track daily missions, and compete through clan-based league progression.

### Delivered Modules

- Auth and security: manual login/register, Google OAuth2, session handling, profile management, public profile, admin user management, CSRF protection, and login/register throttling.
- Reading and quiz: published text library, quiz flow with hidden-source behavior during attempts, quiz history, admin text and question management, and reading statistics API.
- Achievements: achievement unlock flow, selectable profile achievements, daily missions, mission progress tracking, and admin achievement distribution endpoints.
- League and clan: clan creation and join requests, public player profile view, multi-tier leaderboard, dynamic score modifiers, season transition, archived clan handling, and paginated leaderboard UI/API.

### Tech Stack

- Java 25
- Spring Boot + Thymeleaf
- Spring Security + Google OAuth2 client
- Spring Data JPA
- H2 database for local and staging single-app deployment
- Docker / Railway for containerized staging

## Local Setup

### Prerequisites

- JDK 25 installed
- Docker Desktop if you want to run the containerized version

### Environment Variables

Set these through `.env`, terminal environment variables, or your deployment platform.

- `DB_URL`: optional. Local default is `jdbc:h2:file:./data/yomu-db-v2;DB_CLOSE_ON_EXIT=FALSE`.
- `DB_USERNAME`: optional. Local default is `sa`.
- `DB_PASSWORD`: optional for local, required for Docker/Railway.
- `SPRING_PROFILES_ACTIVE`: use `docker` for container deployment.
- `APP_DEMO_SEED`: set to `true` to load staging/demo users, demo clan, and primary daily mission.
- `GOOGLE_CLIENT_ID`: required if Google SSO should be enabled.
- `GOOGLE_CLIENT_SECRET`: required if Google SSO should be enabled.
- `GOOGLE_OAUTH_ENABLED`: optional, defaults to `true`. Set `false` to hide Google OAuth when credentials are unavailable.
- `SESSION_TIMEOUT`: optional, defaults to `15m`.

### Run Locally with Gradle

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

The app will be available at `http://localhost:8080`.

### Run with Docker Compose

Before starting Docker Compose, set `DB_PASSWORD`.

```powershell
$env:DB_PASSWORD="replace-this-password"
docker compose up --build
```

The container uses:

- `DOCKERFILE` as the build file
- `SPRING_PROFILES_ACTIVE=docker`
- persistent H2 storage mounted at `/app/data`

## Demo Data

### Default Seed

The application always seeds published reading content and quizzes. Outside the plain `docker` profile, it also seeds the admin account below.

- Admin username: `cat`
- Admin email: `hasanul.muttaqin@ui.ac.id`
- Admin password: `pass123`

### Optional Demo Seed

Enable `APP_DEMO_SEED=true` to prepare staging/demo presentation data.

- Demo leader email: `kalfin.demo.leader@yomu.test`
- Demo leader password: `KalfinDemo1!`
- Demo member email: `kalfin.demo.member@yomu.test`
- Demo member password: `KalfinDemo2!`
- Demo clan: `Kalfin Demo Clan`

## Quality Checks

Run the full verification suite with:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

The repository includes GitHub Actions CI for:

- build and test
- JaCoCo coverage gate
- CodeQL analysis

## Monitoring and Profiling

The app includes Spring Boot Actuator and Micrometer Prometheus metrics for safe production monitoring.

Only these Actuator endpoints are exposed:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/metrics/http.server.requests`
- `/actuator/prometheus`

The app intentionally does not expose all Actuator endpoints. `/actuator/**` is protected by Spring Security and requires an admin account.

After the app receives traffic, `/actuator/metrics/http.server.requests` shows HTTP request timing metrics. `/actuator/health` should report `UP` for a healthy deployment.

Important Thymeleaf page controllers log preparation time for dashboard, list, detail, search/filter, profile, leaderboard, and reading pages. These logs help identify slow controller preparation before template rendering.

### Optional Java Flight Recorder

For short profiling sessions, run Java Flight Recorder manually. Do not enable this permanently in production.

For Gradle builds:

```bash
java -XX:StartFlightRecording=duration=60s,filename=profile.jfr -jar build/libs/yomu-0.0.1-SNAPSHOT.jar
```

The `.jfr` file can be opened with JDK Mission Control.

### Performance Review Notes

- Reading user pages already use pagination for published text lists. Keep this pattern for future large collections.
- Admin text filtering currently loads all texts before filtering in service code; consider repository-level filtering or pagination if the text catalog grows.
- Achievement pages load all achievements and categories for the page; add pagination or cached category lists if these become large.
- League pages resolve display names in batches, which avoids the most obvious repeated user lookup issue. Keep batching for future member/request views.
- Avoid adding heavy business logic to Thymeleaf templates; keep data shaping in controllers or services.
- Keep large static assets compressed and avoid embedding oversized images directly in templates.
- Current-user resolution reads account data from the database on authenticated pages. If this becomes hot, consider request-scoped caching.

## Staging and Deployment Notes

The final CD path uses GitHub Actions as the deployment gate for a single Railway app with embedded H2 persistence:

- `ci.yml` runs on pushes and pull requests.
- `.github/workflows/cd-railway.yml` deploys only after `CI Quality Gate` succeeds on the `staging` branch.
- The deploy job checks out the exact commit that passed CI, syncs production variables to Railway, deploys with `railway up`, and smoke-tests `/auth/login`.
- GitHub Secrets are used by the workflow only; they do not automatically become Railway runtime variables unless the CD workflow syncs them.

Configure these GitHub Actions secrets or variables before pushing to `staging`:

- `RAILWAY_TOKEN`: Railway Project Token for the target project/environment. Rotate this token if it was ever shared.
- `RAILWAY_SERVICE`: Railway service name for the Yomu app.
- `RAILWAY_ENVIRONMENT`: Railway environment name, usually `production`.
- `RAILWAY_PUBLIC_URL`: public Railway URL used by the smoke test, for example `https://yomu.up.railway.app`. Include `https://` for clarity; the workflow also normalizes missing schemes.
- `RAILWAY_DB_PASSWORD`: strong password for the H2 database user in production.
- `GOOGLE_CLIENT_ID`: Google OAuth client ID.
- `GOOGLE_CLIENT_SECRET`: Google OAuth client secret.
- `APP_DEMO_SEED`: optional, defaults to `true` for presentation data. Set `false` for a clean production seed.
- `SESSION_TIMEOUT`: optional, defaults to `15m`.

The workflow syncs these Railway runtime variables automatically:

- `SPRING_PROFILES_ACTIVE=docker`
- `DB_USERNAME=yomu`
- `DB_PASSWORD` from `RAILWAY_DB_PASSWORD`
- `APP_DEMO_SEED`
- `GOOGLE_OAUTH_ENABLED=true`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `APP_OAUTH2_GOOGLE_REDIRECT_URI`, derived from `RAILWAY_PUBLIC_URL`
- `SESSION_TIMEOUT`

You do not need to set `PORT`, `DB_URL`, `DB_DRIVER`, or `H2_WEB_ALLOW_OTHERS` manually for Railway. Railway provides `PORT`, while the `docker` Spring profile already configures H2 storage at `/app/data/yomu-db` and disables the H2 console.

If Google SSO is enabled, also register the Railway callback URL in Google Cloud Console:

- `https://<your-railway-domain>/login/oauth2/code/google`

## 1. Current Architecture

The architecture diagrams follow the C4 model from Module 09: Context, Container, Component, Code, and Deployment. The diagrams represent the final delivered Yomu codebase, including Auth, Reading and Quiz, Achievements, Daily Missions, League, Clan, public profile, staging seed, and cross-module integration.

### System Context Diagram
```mermaid
C4Context
title System Context Diagram for Yomu Literacy Platform

Person(learner, "Learner", "Reads, quizzes, missions, clan, profile")
Person(clan_leader, "Clan Leader", "Reviews join requests")
Person(admin, "Admin", "Manages content and seasons")

System(yomu, "Yomu Platform", "Gamified literacy platform")
System_Ext(google_oauth, "Google OAuth2", "External identity provider")

Rel(learner, yomu, "Uses app", "HTTPS")
Rel(clan_leader, yomu, "Manages clan", "HTTPS")
Rel(admin, yomu, "Admin controls", "HTTPS")
Rel(yomu, google_oauth, "Google sign-in", "OAuth2")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Container Diagram
```mermaid
C4Container
title Container Diagram for Yomu Literacy Platform

Person(learner, "Learner", "Uses web UI")
Person(clan_leader, "Clan Leader", "Manages clan")
Person(admin, "Admin", "Admin controls")
System_Ext(google_oauth, "Google OAuth2", "Identity provider")

System_Boundary(yomu_system, "Yomu Platform") {
    Container(web_app, "Spring Boot Web App", "Java 25, Spring Boot, Thymeleaf", "MVC pages, REST APIs, security, events")
    ContainerDb(database, "Embedded H2 DB", "H2 file database", "App data and demo data")
}

Rel(learner, web_app, "Learner flows", "HTTPS")
Rel(clan_leader, web_app, "Clan flows", "HTTPS")
Rel(admin, web_app, "Admin flows", "HTTPS")
Rel(web_app, google_oauth, "SSO", "OAuth2")
Rel(web_app, database, "Reads/Writes", "JPA/JDBC")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Component Diagram
```mermaid
C4Component
title Component Diagram for the Yomu Spring Boot Application

ContainerDb(database, "H2 Database", "H2", "Persistence")
System_Ext(google_oauth, "Google OAuth2", "Identity")

Container_Boundary(web_app, "Spring Boot Web Application") {
    Component(shared_web, "Shared Web Shell", "Spring MVC", "Landing, nav, error pages")
    Component(auth_component, "Auth/Profile", "Spring Security", "Login, OAuth2, roles, sessions")
    Component(reading_component, "Reading/Quiz", "MVC + REST", "Texts, quizzes, stats")
    Component(achievement_component, "Achievements", "Services + Scheduler", "Achievements and missions")
    Component(league_component, "League/Clan", "MVC + REST", "Clans, tiers, seasons")
    Component(integration_contracts, "Integration Contracts", "Java records/ports", "Events and query ports")
    Component(data_seed, "Data Seeder", "CommandLineRunner", "Base and demo data")
}

Rel(shared_web, auth_component, "Current user")
Rel(auth_component, google_oauth, "SSO")
Rel(reading_component, integration_contracts, "Publishes quiz event")
Rel(achievement_component, integration_contracts, "Listens/provides ports")
Rel(league_component, integration_contracts, "Listens/uses ports")
Rel(auth_component, database, "Reads/Writes", "JPA")
Rel(reading_component, database, "Reads/Writes", "JPA")
Rel(achievement_component, database, "Reads/Writes", "JPA")
Rel(league_component, database, "Reads/Writes", "JPA")
Rel(data_seed, database, "Seeds", "JPA")

UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

### Auth Module Component Diagram
```mermaid
C4Component
title Auth and Profile Module

ContainerDb(auth_db, "Auth Tables", "H2/JPA", "auth_users")
System_Ext(google_oauth, "Google OAuth2", "Identity")

Component(auth_ctrl, "AuthController", "MVC", "Login and register")
Component(profile_ctrl, "ProfileController", "MVC", "Profile and delete account")
Component(admin_ctrl, "AdminUserController", "MVC", "Admin user page")
Component(security, "SecurityConfig", "Spring Security", "Routes, CSRF, session")
Component(auth_service, "AuthService", "Service", "Manual auth")
Component(profile_service, "ProfileService", "Service", "Profile changes")
Component(admin_service, "AdminUserManagementService", "Service", "User moderation")
Component(oauth_service, "OAuth2LoginUserService", "Service", "SSO provisioning")
Component(current_user, "CurrentUserResolver", "Service", "Principal lookup")

Rel(auth_ctrl, auth_service, "Uses")
Rel(profile_ctrl, profile_service, "Uses")
Rel(profile_ctrl, current_user, "Resolves")
Rel(admin_ctrl, admin_service, "Uses")
Rel(security, auth_ctrl, "Guards")
Rel(oauth_service, google_oauth, "SSO")
Rel(auth_service, auth_db, "Reads/Writes")
Rel(profile_service, auth_db, "Reads/Writes")
Rel(admin_service, auth_db, "Reads/Writes")
Rel(current_user, auth_db, "Reads")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

### Reading and Quiz Module Component Diagram
```mermaid
C4Component
title Reading and Quiz Module

ContainerDb(reading_db, "Reading Tables", "H2/JPA", "texts, questions, attempts")

Component(text_ctrl, "TextController", "MVC", "Read and quiz pages")
Component(admin_text_ctrl, "AdminTextController", "MVC", "Text and question admin")
Component(text_api_ctrl, "TextApiController", "REST", "Reading stats API")
Component(text_service, "TextService", "Service", "Text CRUD and quiz grading")
Component(stats_adapter, "ReadingStatsAdapter", "Port adapter", "Stats for league")
Component(quiz_event, "QuizCompletedEvent", "Integration event", "Quiz result payload")

Rel(text_ctrl, text_service, "Uses")
Rel(admin_text_ctrl, text_service, "Uses")
Rel(text_api_ctrl, text_service, "Uses")
Rel(text_service, reading_db, "Reads/Writes")
Rel(stats_adapter, reading_db, "Reads")
Rel(text_service, quiz_event, "Publishes")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

### Achievement Module Component Diagram
```mermaid
C4Component
title Achievement and Daily Mission Module

ContainerDb(achievement_db, "Achievement Tables", "H2/JPA", "achievements, missions, progress")

Component(achievement_ctrl, "AchievementController", "MVC + REST", "Achievements and missions")
Component(achievement_service, "AchievementService", "Service", "Unlocks and display")
Component(mission_service, "DailyMissionService", "Service", "Mission progress")
Component(quiz_listener, "AchievementQuizCompletionEventListener", "Event listener", "Quiz completion")
Component(scheduler, "DailyMissionRotationScheduler", "Scheduler", "Daily rotation")
Component(profile_adapter, "AchievementProfileAdapter", "Port adapter", "Displayed badges")
Component(mission_adapter, "DailyMissionStatusAdapter", "Port adapter", "Primary mission summary")

Rel(achievement_ctrl, achievement_service, "Uses")
Rel(achievement_ctrl, mission_service, "Uses")
Rel(quiz_listener, achievement_service, "Updates")
Rel(quiz_listener, mission_service, "Updates")
Rel(scheduler, mission_service, "Rotates")
Rel(achievement_service, achievement_db, "Reads/Writes")
Rel(mission_service, achievement_db, "Reads/Writes")
Rel(profile_adapter, achievement_db, "Reads")
Rel(mission_adapter, achievement_db, "Reads")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

### League and Clan Module Component Diagram
```mermaid
C4Component
title League and Clan Module

ContainerDb(league_db, "League Tables", "H2/JPA", "clans, tiers, seasons, score events")

Component(clan_ctrl, "ClanController", "MVC", "Clan pages and leaderboard")
Component(clan_api, "ClanRestController", "REST", "Clan API")
Component(league_api, "LeagueIntegrationRestController", "REST", "Score ingest API")
Component(clan_service, "ClanService", "Service", "Clan and leaderboard logic")
Component(season_service, "LeagueSeasonService", "Service", "Season lifecycle")
Component(score_calc, "ClanScoreCalculator", "Service", "Tier scores and modifiers")
Component(quiz_listener, "LeagueQuizCompletionEventListener", "Event listener", "Quiz completion")
Component(score_strategies, "TierScoreStrategy", "Strategy pattern", "Bronze to Diamond")

Rel(clan_ctrl, clan_service, "Uses")
Rel(clan_api, clan_service, "Uses")
Rel(league_api, clan_service, "Ingests")
Rel(quiz_listener, clan_service, "Records")
Rel(clan_service, season_service, "Uses")
Rel(clan_service, score_calc, "Scores")
Rel(score_calc, score_strategies, "Selects")
Rel(clan_service, league_db, "Reads/Writes")
Rel(season_service, league_db, "Reads/Writes")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

### Quiz Completion Integration Flow
```mermaid
sequenceDiagram
    autonumber
    actor Learner
    participant TC as TextController
    participant TS as TextService
    participant QA as QuizAttemptRepo
    participant Event as QuizCompletedEvent
    participant AchL as AchievementQuizCompletionEventListener
    participant LgL as LeagueQuizCompletionEventListener
    participant Ach as AchievementService
    participant DM as DailyMissionService
    participant Clan as ClanService
    participant Season as SeasonService
    participant ScoreRepo as ScoreEventRepo

    Learner->>TC: Submit answers
    TC->>TS: submitQuiz()
    TS->>QA: Save attempt
    TS-->>Event: Publish score event
    Event-->>AchL: Notify
    AchL->>DM: Update mission
    AchL->>Ach: Unlock achievements
    Event-->>LgL: Notify
    LgL->>Clan: Record score
    Clan->>Season: Resolve season
    Clan->>ScoreRepo: Save once
```

### Deployment Diagram
```mermaid
C4Deployment
title Current Deployment Diagram for Yomu Staging

Deployment_Node(user_device, "User Device", "Desktop/Mobile") {
    Container(browser, "Web Browser", "Browser", "Accesses Yomu")
}

Deployment_Node(github, "GitHub", "Repository and Actions") {
    Container(repo, "Yomu Repository", "Git", "Source code")
    Container(ci, "GitHub Actions CI/CD", "Actions", "Build, tests, coverage, Railway deploy")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2", "Authentication")
}

Deployment_Node(app_container, "Railway App", "Docker from DOCKERFILE") {
    Container(web_app, "Spring Boot App", "Java 25", "Web and API")
    ContainerDb(h2_engine, "Embedded H2", "H2", "In-process DB")
}

Deployment_Node(volume, "Railway Volume", "/app/data") {
    ContainerDb(h2_files, "H2 Files", "File storage", "Persistent data")
}

Rel(browser, web_app, "Uses", "HTTPS")
Rel(repo, ci, "Checks and deploys", "Actions")
Rel(ci, web_app, "Deploys tested commit", "Railway CLI")
Rel(web_app, google_oauth, "SSO", "OAuth2")
Rel(web_app, h2_engine, "Uses", "JDBC")
Rel(h2_engine, h2_files, "Reads/Writes", "File I/O")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="5")
```

## 2. Future Architecture

Based on risk storming, the main future risk is the current staging database topology: H2 is simple and effective for a single-app presentation environment, but it is not suitable for horizontal scaling. The expected future architecture keeps the same module boundaries while moving persistence into a standalone database and allowing multiple stateless app instances.

### Future Deployment Diagram
```mermaid
C4Deployment
title Future Deployment Diagram for a Scalable Yomu Platform

Deployment_Node(user_device, "User Device", "Desktop/Mobile") {
    Container(browser, "Web Browser", "Browser", "Accesses Yomu")
}

Deployment_Node(edge, "Edge Layer", "Managed HTTPS") {
    Container(load_balancer, "Load Balancer", "Ingress", "Routes traffic")
}

Deployment_Node(instance_a, "App Instance A", "Docker Container") {
    Container(web_app_a, "Yomu App A", "Java 25", "Web and API")
}

Deployment_Node(instance_b, "App Instance B", "Docker Container") {
    Container(web_app_b, "Yomu App B", "Java 25", "Web and API")
}

Deployment_Node(data_layer, "Data Layer", "Managed database") {
    ContainerDb(postgres, "PostgreSQL", "Managed DB", "Durable app data")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2", "Authentication")
}

Rel(browser, load_balancer, "Uses", "HTTPS")
Rel(load_balancer, web_app_a, "Routes", "HTTP")
Rel(load_balancer, web_app_b, "Routes", "HTTP")
Rel(web_app_a, google_oauth, "SSO", "OAuth2")
Rel(web_app_b, google_oauth, "SSO", "OAuth2")
Rel(web_app_a, postgres, "Reads/Writes", "JDBC/TCP")
Rel(web_app_b, postgres, "Reads/Writes", "JDBC/TCP")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="6")
```

## 3. Explanation of Risk Storming

We used **Risk Storming** to review the delivered architecture against the final feature set, then placed the main risks on the architecture diagrams as Module 09 recommends.

1. **Identify**: the highest-risk area is persistence and availability. The current Railway setup runs one Spring Boot container and one embedded H2 file database under `/app/data`. This is excellent for a simple staging demo, but the application and database lifecycles are tightly coupled.
2. **Assess**: the risk becomes significant if Yomu gains many concurrent learners. A single app instance is a reliability bottleneck, and an embedded H2 file database cannot be safely shared by multiple app instances for horizontal scaling.
3. **Mitigate**: the future architecture moves the database to PostgreSQL and treats Spring Boot instances as stateless. This enables load balancing, independent database backups, safer scaling, and clearer operational boundaries while preserving the existing modular package structure.

## 4. Code Diagrams

### Auth and Profile Code Diagram
```mermaid
classDiagram
    class AuthUser {
        UUID id
        String username
        String email
        Long phoneNumber
        String displayName
        String password
        boolean active
        AuthRole role
        LocalDateTime createdAt
        LocalDateTime deletedAt
        updateProfile()
        deactivate()
        activate()
    }

    class AuthRole {
        <<enumeration>>
        USER
        ADMIN
    }

    class AuthService {
        registerUser()
        loginUser()
    }

    class ProfileService {
        updateProfile()
        deleteOwnAccount()
    }

    class AdminUserManagementService {
        searchUsers()
        updateUserStatus()
    }

    class CurrentUserResolver {
        resolveUser()
        resolveUsername()
    }

    AuthUser --> AuthRole
    AuthService ..> AuthUser : creates and validates
    ProfileService ..> AuthUser : updates and deactivates
    AdminUserManagementService ..> AuthUser : searches and moderates
    CurrentUserResolver ..> AuthUser : resolves principal
```

### Reading and Quiz Code Diagram
```mermaid
classDiagram
    class Text {
        Long id
        String title
        String content
        Category category
        String createdByUserId
        boolean published
        Instant createdAt
    }

    class Category {
        Long id
        String name
    }

    class Question {
        Long id
        Text text
        String question
        List~Option~ options
    }

    class Option {
        Long id
        Question question
        String text
        boolean correct
    }

    class QuizAttempt {
        Long id
        Text text
        String userId
        Double score
        Double accuracy
        Instant timestamp
    }

    class TextService {
        getAllTexts()
        getPublishedTextById()
        submitQuiz()
        publishText()
        deleteText()
        getUserReadingStats()
    }

    class QuizCompletedEvent {
        UUID eventId
        UUID userId
        UUID textId
        Long readingTextId
        double score
        double accuracy
        LocalDateTime completedAt
    }

    Category "1" <-- "many" Text
    Text "1" *-- "many" Question
    Question "1" *-- "many" Option
    Text "1" o-- "many" QuizAttempt
    TextService ..> QuizAttempt : grades and saves
    TextService ..> QuizCompletedEvent : publishes
```

### Achievement and Daily Mission Code Diagram
```mermaid
classDiagram
    class Achievement {
        Long id
        String name
        String milestone
        AchievementRequirementType requirementType
        int targetValue
    }

    class UserAchievement {
        Long id
        UUID userId
        Achievement achievement
        LocalDateTime unlockedAt
        boolean displayed
    }

    class DailyMission {
        Long id
        String title
        int targetCount
        LocalDate activeDate
        boolean primary
        Long categoryId
    }

    class UserMissionProgress {
        Long id
        UUID userId
        DailyMission mission
        int currentProgress
        boolean completed
    }

    class UserStatistic {
        Long id
        UUID userId
        int totalReadings
        double totalScore
    }

    class AchievementService {
        processQuizCompletion()
        toggleDisplayAchievement()
        getAchievementProgress()
        getAchievementDistribution()
    }

    class DailyMissionService {
        incrementProgress()
        getTodayMissions()
        createDailyMission()
        rotateDailyMissions()
    }

    Achievement "1" <-- "many" UserAchievement
    DailyMission "1" <-- "many" UserMissionProgress
    AchievementService ..> UserStatistic : updates totals
    AchievementService ..> UserAchievement : unlocks idempotently
    DailyMissionService ..> UserMissionProgress : tracks progress
```

### League and Clan Code Diagram
```mermaid
classDiagram
    class Clan {
        UUID id
        String name
        Tier tier
        UUID createdByUserId
        LocalDateTime createdAt
        boolean deleted
        LocalDateTime deletedAt
        UUID deletedByUserId
        changeTier()
        archive()
        removeAllMembers()
    }

    class ClanMember {
        UUID id
        Clan clan
        UUID userId
        ClanMemberRole role
        LocalDateTime joinedAt
    }

    class ClanJoinRequest {
        UUID id
        Clan clan
        UUID requesterUserId
        ClanJoinRequestStatus status
        UUID reviewedByUserId
        LocalDateTime reviewedAt
        approve()
        reject()
    }

    class ClanQuizScoreEvent {
        UUID id
        UUID eventId
        UUID clanId
        UUID userId
        UUID textId
        UUID seasonId
        double score
        double accuracy
        LocalDateTime completedAt
    }

    class LeagueSeason {
        UUID id
        int seasonNumber
        boolean active
        LocalDateTime startedAt
        LocalDateTime endedAt
        end()
    }

    class Tier {
        UUID id
        TierCode code
        String displayName
    }

    class ClanService {
        createClan()
        submitJoinRequest()
        reviewJoinRequest()
        recordQuizCompletion()
        getLeaderboardPage()
        endCurrentSeason()
        deleteClan()
        getPublicProfile()
    }

    class ClanScoreCalculator {
        calculate()
    }

    Tier "1" <-- "many" Clan
    Clan "1" *-- "many" ClanMember
    Clan "1" o-- "many" ClanJoinRequest
    LeagueSeason "1" <-- "many" ClanQuizScoreEvent
    ClanService ..> ClanQuizScoreEvent : records idempotent events
    ClanService ..> LeagueSeason : manages seasons
    ClanService ..> ClanScoreCalculator : computes leaderboard
```
