# ─────────────────────────────────────────────
# Stage 1 — Build the JAR with Maven
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy dependency manifests first for layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2 — Minimal runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the built JAR from stage 1
COPY --from=builder /app/target/food-qr-monolith-0.0.1-SNAPSHOT.jar app.jar

# Render sets PORT env var — default to 8083 for local
EXPOSE 8083

# JVM tuning: smaller heap for Render free tier (512 MB RAM)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
