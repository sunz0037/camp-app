# AGENTS.md

## Cursor Cloud specific instructions

### Project overview

Campus OA System (校园OA系统) — a Spring Boot 3.2.0 (Java 21) backend REST API for campus administration. See `README.md` for feature list and project structure.

### Running the application

Use the `dev` Spring profile to run against an in-memory H2 database instead of the remote MySQL instance:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

The server starts on **port 8080**. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:campus_oa`, user: `sa`, no password).

### Building and testing

```bash
./gradlew build          # compile + package (no test sources exist yet)
./gradlew bootRun        # run (add SPRING_PROFILES_ACTIVE=dev for H2)
```

There are currently no automated tests in `src/test/`. The project has no linter configuration beyond the Java compiler.

### Key caveats

- **Gradle wrapper URL**: The original `gradle-wrapper.properties` pointed to a local macOS path (`file:///Applications/gradle/...`). It has been fixed to download from `https://services.gradle.org/distributions/gradle-8.5-bin.zip`.
- **Student entity mapping**: `Student.java` had duplicate column mappings for `class_id` and `grade_id` (both a `@ManyToOne @JoinColumn` and a plain `@Column`). Fixed by adding `insertable = false, updatable = false` to the `@Column` fields.
- **H2 dev profile**: `application-dev.properties` overrides datasource and JPA dialect settings to use H2 in MySQL compatibility mode. The default profile in `application.properties` still targets the remote MySQL server.
- **No frontend**: The Vue 3 frontend is in a separate repository. CORS is configured for `localhost:3000`.
