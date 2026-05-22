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

## Staging and Deployment Notes

This repository is ready for single-app staging deployment using Railway plus embedded H2 persistence, but the GitHub Actions CD workflow is still a dummy placeholder. That means:

- `ci.yml` will run automatically on pushes and pull requests.
- `.github/workflows/cd-dummy.yml` does not deploy to Railway or any live environment.
- actual staging deployment will happen automatically only if your Railway project is already connected to this repository and tracking the branch you push.

For Railway staging, configure these variables on the platform:

- `SPRING_PROFILES_ACTIVE=docker`
- `DB_PASSWORD=your-strong-password`
- `APP_DEMO_SEED=true` for presentation-ready demo data
- `GOOGLE_CLIENT_ID=your-google-client-id` if Google SSO is required
- `GOOGLE_CLIENT_SECRET=your-google-client-secret` if Google SSO is required
- `GOOGLE_OAUTH_ENABLED=false` if you want staging to run without Google SSO

If Google SSO is enabled, also register the Railway callback URL in Google Cloud Console:

- `https://<your-railway-domain>/login/oauth2/code/google`

## 1. Current Architecture

The architecture diagrams follow the C4 model from Module 09: Context, Container, Component, Code, and Deployment. The diagrams represent the final delivered Yomu codebase, including Auth, Reading and Quiz, Achievements, Daily Missions, League, Clan, public profile, staging seed, and cross-module integration.

### System Context Diagram
```mermaid
C4Context
title System Context Diagram for Yomu Literacy Platform

Person(learner, "Learner", "Reads texts, completes quizzes, tracks achievements and daily missions, joins clans, and views public profiles.")
Person(clan_leader, "Clan Leader", "A learner who creates a clan, reviews join requests, and may archive their clan.")
Person(admin, "Admin", "Manages users, reading content, quiz questions, achievements, daily missions, and league seasons.")

System(yomu, "Yomu Platform", "Gamified literacy learning platform for reading, quizzes, achievements, daily missions, and clan-based leagues.")
System_Ext(google_oauth, "Google OAuth2", "External identity provider for Google sign-in.")

Rel(learner, yomu, "Uses learning, quiz, achievement, mission, clan, leaderboard, and profile features", "HTTPS")
Rel(clan_leader, yomu, "Manages clan membership and clan lifecycle", "HTTPS")
Rel(admin, yomu, "Uses administrative controls", "HTTPS")
Rel(yomu, google_oauth, "Authenticates Google users and merges matching accounts", "OAuth2/HTTPS")
```

### Container Diagram
```mermaid
C4Container
title Container Diagram for Yomu Literacy Platform

Person(learner, "Learner", "Uses the web interface")
Person(clan_leader, "Clan Leader", "Manages clan flows")
Person(admin, "Admin", "Manages content and system controls")
System_Ext(google_oauth, "Google OAuth2", "External identity provider")

System_Boundary(yomu_system, "Yomu Platform") {
    Container(web_app, "Spring Boot Web Application", "Java 25, Spring Boot, Thymeleaf, Spring Security", "Serves MVC pages and REST APIs, enforces role-based access, publishes quiz completion events, and coordinates all modules.")
    ContainerDb(database, "Embedded H2 Database", "H2 file database", "Stores users, reading content, quiz attempts, achievements, missions, clans, seasons, and leaderboard score events.")
}

Rel(learner, web_app, "Reads texts, submits quizzes, tracks progress, joins clans", "HTTPS")
Rel(clan_leader, web_app, "Creates clans, reviews requests, archives clans", "HTTPS")
Rel(admin, web_app, "Admin CRUD and season transition actions", "HTTPS")
Rel(web_app, google_oauth, "Delegates Google sign-in", "OAuth2/HTTPS")
Rel(web_app, database, "Reads and writes application state", "JPA/JDBC")
```

### Component Diagram
```mermaid
C4Component
title Component Diagram for the Yomu Spring Boot Application

ContainerDb(database, "H2 Database", "H2", "Application persistence")
System_Ext(google_oauth, "Google OAuth2", "External identity provider")

Container_Boundary(web_app, "Spring Boot Web Application") {
    Component(shared_web, "Shared Web Shell", "Spring MVC", "Landing page, global navigation model, and error page handling.")
    Component(auth_component, "Auth and Profile Module", "Spring Security + Services", "Manual auth, Google OAuth2, session hardening, profile update, account deletion, admin user management, role guards, and throttling.")
    Component(reading_component, "Reading and Quiz Module", "Spring MVC/REST + Services", "Published reading list, text detail, hidden-source quiz flow, admin text/question management, quiz history, pagination, and reading stats.")
    Component(achievement_component, "Achievement and Daily Mission Module", "Spring Services + Scheduler", "Achievement unlocks, displayed achievements, daily mission progress, admin distribution, primary mission summary, and scheduled rotation.")
    Component(league_component, "League and Clan Module", "Spring MVC/REST + Services", "Clan creation, join requests, public profiles, tiered leaderboards, score strategies, buff/debuff, season transitions, archive flow, and pagination.")
    Component(integration_contracts, "Integration Contracts", "Java records and ports", "QuizCompletedEvent, ReadingStatsPort, AchievementProfilePort, and DailyMissionStatusPort.")
    Component(data_seed, "Data Seeder", "CommandLineRunner", "Seeds published texts, quizzes, admin account, optional demo users, daily mission, and demo clan.")
}

Rel(shared_web, auth_component, "Resolves current user for navigation")
Rel(auth_component, google_oauth, "Authenticates via")
Rel(reading_component, integration_contracts, "Publishes QuizCompletedEvent")
Rel(achievement_component, integration_contracts, "Consumes quiz events and provides achievement/mission ports")
Rel(league_component, integration_contracts, "Consumes quiz events and calls reading/achievement/mission ports")
Rel(auth_component, database, "Reads/Writes", "JPA")
Rel(reading_component, database, "Reads/Writes", "JPA")
Rel(achievement_component, database, "Reads/Writes", "JPA")
Rel(league_component, database, "Reads/Writes", "JPA")
Rel(data_seed, database, "Seeds demo and base data", "JPA")
```

### Quiz Completion Integration Flow
```mermaid
sequenceDiagram
    autonumber
    actor Learner
    participant TextController
    participant TextService
    participant QuizAttemptRepository
    participant QuizCompletedEvent
    participant AchievementListener as AchievementQuizCompletionEventListener
    participant LeagueListener as LeagueQuizCompletionEventListener
    participant AchievementService
    participant DailyMissionService
    participant ClanService
    participant LeagueSeasonService
    participant ClanQuizScoreEventRepository

    Learner->>TextController: Submit quiz answers
    TextController->>TextService: submitQuiz(textId, userId, answers)
    TextService->>QuizAttemptRepository: Save QuizAttempt(score, accuracy)
    TextService-->>QuizCompletedEvent: Publish eventId, userId, textId, score, accuracy
    QuizCompletedEvent-->>AchievementListener: Deliver event
    AchievementListener->>DailyMissionService: incrementProgress(userId, readingTextId)
    AchievementListener->>AchievementService: processQuizCompletion(userId, score, completedAt)
    QuizCompletedEvent-->>LeagueListener: Deliver event
    LeagueListener->>ClanService: recordQuizCompletion(payload)
    ClanService->>LeagueSeasonService: getOrCreateActiveSeason()
    ClanService->>ClanQuizScoreEventRepository: Save idempotent score event
```

### Deployment Diagram
```mermaid
C4Deployment
title Current Deployment Diagram for Yomu Staging

Deployment_Node(user_device, "User Device", "Desktop/Mobile Browser") {
    Container(browser, "Web Browser", "Chrome, Firefox, Safari", "Accesses Yomu")
}

Deployment_Node(github, "GitHub", "Repository and Actions") {
    Container(repo, "Yomu Repository", "Git", "Stores source code and workflow definitions")
    Container(ci, "GitHub Actions CI", "Actions", "Runs build, tests, JaCoCo, and CodeQL")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2 Service", "Provides Google authentication")
}

Deployment_Node(railway, "Railway", "Single service staging environment") {
    Deployment_Node(app_container, "Docker Container", "Built from DOCKERFILE") {
        Container(web_app, "Spring Boot Web App", "Java 25", "Serves MVC pages and REST APIs")
        ContainerDb(h2_engine, "Embedded H2 Engine", "H2", "Runs inside the application process")
    }
    Deployment_Node(volume, "Persistent Volume", "/app/data") {
        ContainerDb(h2_files, "H2 Database Files", "File storage", "Persists staging data across restarts")
    }
}

Rel(browser, web_app, "Uses", "HTTPS")
Rel(repo, ci, "Triggers checks", "GitHub Actions")
Rel(repo, web_app, "Auto-deploys when Railway tracks the pushed branch", "GitHub integration")
Rel(web_app, google_oauth, "Authenticates users", "OAuth2/HTTPS")
Rel(web_app, h2_engine, "Uses embedded database", "JDBC")
Rel(h2_engine, h2_files, "Reads/Writes", "File I/O")
```

## 2. Future Architecture

Based on risk storming, the main future risk is the current staging database topology: H2 is simple and effective for a single-app presentation environment, but it is not suitable for horizontal scaling. The expected future architecture keeps the same module boundaries while moving persistence into a standalone database and allowing multiple stateless app instances.

### Future Deployment Diagram
```mermaid
C4Deployment
title Future Deployment Diagram for a Scalable Yomu Platform

Deployment_Node(user_device, "User Device", "Desktop/Mobile Browser") {
    Container(browser, "Web Browser", "Chrome, Firefox, Safari", "Accesses Yomu")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2 Service", "Provides authentication")
}

Deployment_Node(cloud, "Cloud Platform", "Railway/AWS/GCP") {
    Deployment_Node(edge, "Edge Layer", "Managed HTTPS endpoint") {
        Container(load_balancer, "Load Balancer", "Managed ingress", "Routes traffic to healthy app instances")
    }

    Deployment_Node(app_cluster, "Application Cluster", "Stateless containers") {
        Deployment_Node(instance_a, "App Instance A", "Docker Container") {
            Container(web_app_a, "Yomu Spring Boot App", "Java 25", "Serves web and API traffic")
        }
        Deployment_Node(instance_b, "App Instance B", "Docker Container") {
            Container(web_app_b, "Yomu Spring Boot App", "Java 25", "Serves web and API traffic")
        }
    }

    Deployment_Node(data_layer, "Data Layer", "Managed database") {
        ContainerDb(postgres, "PostgreSQL Database", "PostgreSQL", "Stores durable application data independently from app instances")
    }
}

Rel(browser, load_balancer, "Uses", "HTTPS")
Rel(load_balancer, web_app_a, "Routes requests", "HTTP")
Rel(load_balancer, web_app_b, "Routes requests", "HTTP")
Rel(web_app_a, google_oauth, "Authenticates users", "OAuth2/HTTPS")
Rel(web_app_b, google_oauth, "Authenticates users", "OAuth2/HTTPS")
Rel(web_app_a, postgres, "Reads/Writes", "JDBC/TCP")
Rel(web_app_b, postgres, "Reads/Writes", "JDBC/TCP")
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
        double score
        double accuracy
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
        Long id
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
