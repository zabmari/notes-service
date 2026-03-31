# 📝 Notes App – REST API

Notes App is a backend application built with Spring Boot that allows users to authenticate, create and manage notes, track history, restore revisions, and enforce role‑based access control.

## 💻 Technologies Used
* Java 21
* Spring Boot 3.x
* Spring Security (JWT)
* Spring Data JPA
* Hibernate Envers
* MySQL
* Bucket4j
* Docker Compose
* Lombok

## 🛠️ Base URL
All endpoints are available under:
`http://localhost:8081`

## 🔐 Authentication
| Endpoint | Method | Description |
| :--- | :--- | :--- |
| /auth/register | POST | Register a new user |
| /auth/login | POST | Authenticate and login |

## 📝 Notes Management
| Endpoint | Method | Description |
| :--- | :--- | :--- |
| /items | GET | Get all notes (role‑based) |
| /items | POST | Create a new note |
| /items/{id} | GET | Get note by ID |
| /items/{id} | PATCH | Update note (optimistic locking) |
| /items/{id} | DELETE | Soft delete a note |
| /items/{id}/history | GET | Get note revision history (Envers) |
| /items/{id}/restore/{rev} | POST | Restore note from revision |

## 👥 User Roles
* OWNER – Full access to own notes
* EDITOR – Can edit notes shared with them
* VIEWER – Read‑only access

## 🚀 How to Run with Docker
Make sure Docker is installed and running.

### 🧾 Clone the repository
    git clone <REPOSITORY_URL>
    cd notes-app

### 🧱 Build and run containers
    docker-compose up --build

App will be available at: http://localhost:8081

## 💻 How to Run Locally (without Docker)
1. Install and run MySQL
2. Configure application.properties
3. Run the app:
   mvn spring-boot:run

## ⚙️ Application Configuration

### 📛 Application Name
    spring.application.name=notes-service

### 🗄️ Database Configuration
    spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/notes}
    spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
    spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}

### 🧩 JPA / Hibernate
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

### 🔐 JWT Configuration
    jwt.secret=UltraSecureKey_ForJWT_TokenSigning_2026!
    jwt.expiration=3600

### 🚦 Rate Limiting (Bucket4j)
    rate-limit.login.capacity=5
    rate-limit.login.duration-seconds=60

### 🌐 Application Port
    server.port=8080

Docker maps it to: 8081 → 8080

## 🛡️ Authentication Header
All protected endpoints require:
`Authorization: Bearer <JWT_TOKEN>`