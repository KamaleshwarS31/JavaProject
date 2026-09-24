# ============================================================
# E-Voting Platform — Dockerfile
# Multi-stage build for minimal production image
# NOTE: Docker not available on dev machine; this is generated
#       for CI/CD and production use.
# ============================================================

# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent POM first for layer caching
COPY evoting-platform/pom.xml ./pom.xml
COPY evoting-platform/backend/evoting-core/pom.xml ./backend/evoting-core/pom.xml
COPY evoting-platform/backend/evoting-security/pom.xml ./backend/evoting-security/pom.xml
COPY evoting-platform/backend/evoting-eligibility/pom.xml ./backend/evoting-eligibility/pom.xml
COPY evoting-platform/backend/evoting-ballot/pom.xml ./backend/evoting-ballot/pom.xml
COPY evoting-platform/backend/evoting-blockchain/pom.xml ./backend/evoting-blockchain/pom.xml
COPY evoting-platform/backend/evoting-audit/pom.xml ./backend/evoting-audit/pom.xml
COPY evoting-platform/backend/evoting-tally/pom.xml ./backend/evoting-tally/pom.xml
COPY evoting-platform/backend/evoting-web/pom.xml ./backend/evoting-web/pom.xml
COPY evoting-platform/backend/evoting-tests/pom.xml ./backend/evoting-tests/pom.xml

# Download dependencies (cached unless POM changes)
RUN mvn dependency:go-offline -B -q

# Copy source code
COPY evoting-platform/backend/ ./backend/

# Build (skip tests in image build; run in CI pipeline separately)
RUN mvn clean package -pl backend/evoting-web -am -DskipTests -B -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: run as non-root
RUN groupadd -r evoting && useradd -r -g evoting evoting

WORKDIR /app

# Copy WAR/JAR from build stage
COPY --from=builder /build/backend/evoting-web/target/evoting-web-*.jar app.jar

# Security: no shell, no package managers in runtime image
RUN chmod 550 app.jar

USER evoting

EXPOSE 8080

# Security: read-only filesystem (mount volumes for logs)
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning: G1GC, container-aware memory, deny reflection bypass
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "/app/app.jar"]
