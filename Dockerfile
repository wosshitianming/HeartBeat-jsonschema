# syntax=docker/dockerfile:1

# ── Stage 1: Maven build（启用 ClassFinal 字节码加密）────────────────────────
FROM maven:3.9-eclipse-temurin-8 AS build

ARG CLASSFINAL_PASSWORD=#
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
COPY heartbeat-domain/pom.xml heartbeat-domain/
COPY heartbeat-application/pom.xml heartbeat-application/
COPY heartbeat-infrastructure/pom.xml heartbeat-infrastructure/
COPY heartbeat-interfaces/pom.xml heartbeat-interfaces/
COPY heartbeat-start/pom.xml heartbeat-start/

RUN mvn dependency:go-offline -B -pl heartbeat-start -am || true

COPY heartbeat-domain heartbeat-domain
COPY heartbeat-application heartbeat-application
COPY heartbeat-infrastructure heartbeat-infrastructure
COPY heartbeat-interfaces heartbeat-interfaces
COPY heartbeat-start heartbeat-start

RUN mvn clean package -DskipTests -B -pl heartbeat-start -am -Pprotected \
    -Dclassfinal.password="${CLASSFINAL_PASSWORD}"

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre-jammy

LABEL org.opencontainers.image.title="heartbeat" \
      org.opencontainers.image.description="HeartBeat DDD Spring Boot application (ClassFinal protected)"

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 1000 heartbeat \
    && useradd -u 1000 -g heartbeat -d /app -M heartbeat \
    && chown -R heartbeat:heartbeat /app

COPY --from=build --chown=heartbeat:heartbeat /app/heartbeat-start/target/heartbeat-encrypted.jar /app/app.jar
COPY --chown=heartbeat:heartbeat scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

USER heartbeat

EXPOSE 7001

ENV SERVER_PORT=7001 \
    SPRING_PROFILES_ACTIVE=prod \
    CLASSFINAL_PASSWORD="" \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://127.0.0.1:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
