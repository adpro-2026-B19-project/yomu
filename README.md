# Tutorial B: Visualizing and Architectural Risk

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

### Context Diagram
```mermaid
C4Context
title System Context Diagram for Yomu Literacy Platform

Person(student, "Student User", "A student who uses the platform to read texts and take quizzes.")
Person(admin, "Admin User", "An administrator who manages texts and questions.")
System(yomu, "Yomu Platform", "Provides reading materials, quizzes, and tracks user progress.")
System_Ext(google_oauth, "Google OAuth2", "Provides user authentication and identity.")

Rel(student, yomu, "Reads texts, takes quizzes", "HTTPS")
Rel(admin, yomu, "Manages content", "HTTPS")
Rel(yomu, google_oauth, "Authenticates users via", "OAuth2")
```

### Container Diagram
```mermaid
C4Container
title Container Diagram for Yomu Literacy Platform

Person(student, "Student User", "Reads texts and takes quizzes")
Person(admin, "Admin User", "Manages content")
System_Ext(google_oauth, "Google OAuth2", "User authentication")

System_Boundary(yomu_system, "Yomu Platform") {
    Container(web_app, "Web Application", "Java, Spring Boot", "Handles API requests, business logic, and serves Thymeleaf views.")
    ContainerDb(database, "Embedded Database", "H2", "Stores reading history, quizzes, and texts.")
}

Rel(student, web_app, "Visits and uses", "HTTPS")
Rel(admin, web_app, "Manages content via", "HTTPS")
Rel(web_app, google_oauth, "Authenticates via", "HTTPS/OAuth2")
Rel(web_app, database, "Reads/Writes", "JDBC")
```

### Deployment Diagram
```mermaid
C4Deployment
title Deployment Diagram for Yomu Literacy Platform

Deployment_Node(user_device, "User Device", "Desktop/Mobile") {
    Container(browser, "Web Browser", "Chrome, Firefox, Safari", "Accesses the platform")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2 Service", "Provides authentication")
}

Deployment_Node(docker_host, "Docker Host", "Server/Railway") {
    Deployment_Node(docker_container, "Docker Container", "yomu-app") {
        Container(web_app, "Spring Boot Web App", "Java 25", "Serves application")
        ContainerDb(h2_db, "Embedded DB", "H2", "Processes database transactions")
    }
    Deployment_Node(docker_volume, "Docker Volume", "yomu_data") {
        ContainerDb(data_file, "Database Files", ".db", "Persists data")
    }
}

Rel(browser, web_app, "Makes API calls to", "HTTPS")
Rel(web_app, google_oauth, "Authenticates users", "HTTPS")
Rel(web_app, h2_db, "Uses embedded engine", "Internal")
Rel(h2_db, data_file, "Reads/Writes data", "File I/O")
```

## 2. Future Architecture

Based on the Risk Storming exercise, we've designed a future architecture to address scalability constraints. The monolithic approach with an embedded H2 database is replaced with load-balanced application instances and a robust standalone database.

### Future Deployment Diagram
```mermaid
C4Deployment
title Future Deployment Diagram for Yomu Literacy Platform

Deployment_Node(user_device, "User Device", "Desktop/Mobile") {
    Container(browser, "Web Browser", "Chrome, Firefox, Safari", "Accesses the platform")
}

Deployment_Node(google, "Google Cloud", "External") {
    System_Ext(google_oauth, "Google OAuth2 Service", "Provides authentication")
}

Deployment_Node(cloud, "Cloud Infrastructure", "AWS/Railway") {
    Deployment_Node(lb_node, "Load Balancer", "Nginx") {
        Container(lb, "Load Balancer", "Nginx", "Distributes traffic")
    }
    
    Deployment_Node(app_servers, "Application Cluster", "Docker Swarm/K8s") {
        Deployment_Node(app_instance_1, "Instance 1", "Docker Container") {
            Container(web_app_1, "Spring Boot Web App", "Java 25", "Serves application")
        }
        Deployment_Node(app_instance_n, "Instance N", "Docker Container") {
            Container(web_app_n, "Spring Boot Web App", "Java 25", "Serves application")
        }
    }
    
    Deployment_Node(db_server, "Database Server", "Docker Container") {
        ContainerDb(postgres, "PostgreSQL Database", "PostgreSQL 15", "Stores structured data reliably")
    }
}

Rel(browser, lb, "Makes API calls to", "HTTPS")
Rel(lb, web_app_1, "Routes traffic", "HTTP")
Rel(lb, web_app_n, "Routes traffic", "HTTP")
Rel(web_app_1, google_oauth, "Authenticates users", "HTTPS")
Rel(web_app_n, google_oauth, "Authenticates users", "HTTPS")
Rel(web_app_1, postgres, "Reads/Writes", "JDBC/TCP")
Rel(web_app_n, postgres, "Reads/Writes", "JDBC/TCP")
```

## 3. Explanation of Risk Storming

We applied the **Risk Storming** technique to collaboratively identify, assess, and mitigate architectural risks in the current Yomu Platform. 

1. **Identify**: We mapped out the architecture and identified that the embedded **H2 Database** residing within a Docker container and relying on a Docker volume for persistence is a significant technical debt. It presents a major **Scalability Risk** because multiple instances of the app cannot safely access the same embedded database simultaneously without lock contention or corruption. Furthermore, a single container is a **Single Point of Failure (Reliability Risk)**.
2. **Assess**: Given that our platform anticipates a growing number of students taking reading quizzes simultaneously, the inability to scale horizontally (adding more instances) poses a critical risk to performance and availability.
3. **Mitigate**: We decided to mitigate this by designing a **Future Architecture** that decouples the database from the application container. We will migrate from H2 to a standalone **PostgreSQL** database. This decoupling allows us to introduce a **Load Balancer** and spin up multiple instances of the Spring Boot application (horizontal scaling), thus eliminating the single point of failure and ensuring the system can handle increased traffic securely.

## 4. Individual Work (Reading & Quiz Module - Nisrina Alya Nabilah 2406425924)

### Component Diagram (Reading Module)
```mermaid
C4Component
title Component Diagram for Reading & Quiz Module

Container_Boundary(reading_module, "Reading & Quiz Module") {
    Component(text_api_ctrl, "TextApiController", "Spring REST Controller", "Provides user reading stats API")
    Component(text_ctrl, "TextController", "Spring MVC Controller", "Handles text reading and quiz submissions")
    Component(admin_text_ctrl, "AdminTextController", "Spring MVC Controller", "Manages text creation and publishing")
    
    Component(text_service, "TextService", "Spring Service", "Business logic for text management and quiz grading")
    
    Component(text_repo, "TextRepository", "Spring Data JPA", "Data access for Texts")
    Component(question_repo, "QuestionRepository", "Spring Data JPA", "Data access for Questions")
    Component(quiz_attempt_repo, "QuizAttemptRepository", "Spring Data JPA", "Data access for QuizAttempts")
}

ContainerDb(database, "Database", "H2", "Stores Reading Data")

Rel(text_api_ctrl, text_service, "Uses")
Rel(text_ctrl, text_service, "Uses")
Rel(admin_text_ctrl, text_service, "Uses")

Rel(text_service, text_repo, "Reads/Writes")
Rel(text_service, question_repo, "Reads/Writes")
Rel(text_service, quiz_attempt_repo, "Reads/Writes")

Rel(text_repo, database, "JDBC")
Rel(question_repo, database, "JDBC")
Rel(quiz_attempt_repo, database, "JDBC")
```

### Code Diagram (Class Diagram)
```mermaid
classDiagram
    class Text {
        -Long id
        -String title
        -String content
        -Category category
        -String authorId
        -boolean published
        -LocalDateTime createdAt
        +publish()
    }
    
    class Question {
        -Long id
        -Text text
        -String content
        -List~Option~ options
    }
    
    class Option {
        -Long id
        -Question question
        -String content
        -boolean correct
    }
    
    class QuizAttempt {
        -Long id
        -Text text
        -String userId
        -double score
        -double accuracy
        -Instant timestamp
    }
    
    class TextService {
        +getAllTexts() List~Text~
        +getTextById(Long id) Text
        +createText(String title, String content, Long categoryId, String userId) Text
        +submitQuiz(Long textId, String userId, Map formData) QuizAttempt
        +publishText(Long textId)
        +getUserReadingStats(String userId) UserReadingStatResponse
    }
    
    Text "1" *-- "many" Question : contains
    Question "1" *-- "many" Option : has
    Text "1" o-- "many" QuizAttempt : attempted in
    TextService ..> Text : manages
    TextService ..> QuizAttempt : evaluates
```
