# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build the JAR (skip tests for faster deploys)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Run ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/target/ai-execution-system-1.0.0.jar app.jar

# Railway injects PORT automatically; Spring Boot reads it via server.port=${PORT}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
