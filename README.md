# 📝 Notes App – REST API

Notes App is a backend application built with Spring Boot that allows users to authenticate, create and manage notes,
track history, and enforce resource-level access control.

## 🚀 Key Architectural Decisions

* **Java 21 & Virtual Threads**: Optimized for high concurrency using `spring.threads.virtual.enabled=true`. This allows
  the application to handle a large number of concurrent requests with minimal overhead.
* **Resource-Level Authorization (ABAC)**: Implemented custom security checks to verify if a logged-in user is an *
  *OWNER**, **EDITOR**, or **VIEWER** for a specific note ID before granting access.
* **Soft Delete & Data Integrity**: Deletions are logical (deleted = true). The application uses Hibernate
  @SQLRestriction
  to automatically filter out deleted items from all queries and history views.
* **Hibernate Envers**: Used for automated auditing of the `Item` entity. A custom `RevisionListener` captures the
  `changedBy` (username) information from the Security Context for every change.
* **Optimistic Locking**: Managed via `@Version` field in the `Item` entity to prevent concurrent write conflicts. The
  API returns `HTTP 409 Conflict` if a version mismatch occurs.
* **Rate Limiting**: Implemented for the `/login` endpoint using **Bucket4j** (in-memory) to prevent brute-force
  attacks, with configurable limits in `application.properties`.

## 💻 Technologies Used

* **Java 21**, **Spring Boot 3.x**, **Spring Security (JWT)**
* **Hibernate Envers**, **MySQL**, **Bucket4j**, **Testcontainers**, **Lombok**

## 🛠️ Base URL

* **Local (Maven)**: `http://localhost:8080`
* **Docker Compose**: `http://localhost:8081`

## 🔐 Authentication

| Endpoint    | Method | Description                                        |
|:------------|:-------|:---------------------------------------------------|
| `/register` | POST   | Register a new user account                        |
| `/login`    | POST   | Authenticate and get JWT (Rate Limited: 5 req/min) |

## 📝 Notes Management

| Endpoint                     | Method | Description                                      |
|:-----------------------------|:-------|:-------------------------------------------------|
| `/items`                     | GET    | Get all notes accessible to the user             |
| `/items`                     | POST   | Create a new note (User becomes OWNER)           |
| `/items/{id}`                | PATCH  | Update note content (Requires `version` in body) |
| `/items/{id}`                | DELETE | Soft delete a note (OWNER only)                  |
| `/items/{id}/history`        | GET    | Get full revision history (Envers Audit)         |
| `/items/{id}/share`          | POST   | Grant access to another user (VIEWER/EDITOR)     |
| `/items/{id}/share/{userId}` | DELETE | Revoke user access to the note                   |

## 👥 User Roles

* **OWNER** – Full control (read, edit, soft-delete, manage permissions).
* **EDITOR** – Permission to read and modify content.
* **VIEWER** – Read‑only access to the shared note.

---

## 🚀 How to Run

### Option 1: Docker Compose (Recommended)

1. Make sure Docker is installed and running.
2. Build and run using the following command:

   docker-compose up --build

3. The app will be available at: `http://localhost:8081`

### Option 2: Locally (without Docker)

1. Ensure **MySQL** is running and create a database named `notes`.
2. Configure your credentials in `src/main/resources/application.properties`.
3. Run the application using Maven:

   mvn spring-boot:run

4. The app will be available at: `http://localhost:8080`

---

## ⚙️ Application Configuration

Key settings in `application.properties`:

* `rate-limit.login.capacity=5` – Max login attempts per minute.
* `rate-limit.login.duration-seconds=60` – Rate limit time window.
* `jwt.secret` – Key used for token signing.
* `jwt.expiration=3600` – Token validity in seconds.

---

## 🧪 Testing Strategy

Run the test suite using the command:

    mvn test

* **Unit Tests**: Focused on `SecurityService` (permission logic) and business rules using Mockito.
* **Integration Tests**: Validates the full Envers flow (create -> edit -> check history) and Bucket4j IP-blocking using
  a real MySQL container via **Testcontainers**.

---

## 🛡️ Authentication Header

Required for all `/items/**` endpoints:

`Authorization: Bearer <JWT_TOKEN>`