# Tutorial B: Visualizing and Architectural Risk

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





    .