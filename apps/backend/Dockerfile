# ============================================================
# Stage 1: Build - Maven with JDK 17 (multi-stage build)
# ============================================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production jar skipping tests in batch mode
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime - Slim JRE 17 image
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S football && adduser -S football -G football
WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root
USER football

# Health check endpoint
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8000/health || exit 1

EXPOSE 8000

ENTRYPOINT ["java", "-jar", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "app.jar"]
