# Build JAR Spring Boot
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B package -DskipTests

# Runtime
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S stowflow \
    && adduser -S stowflow -G stowflow
WORKDIR /app

COPY --from=build /app/target/stowflow-api-*.jar app.jar
RUN chown -R stowflow:stowflow /app

USER stowflow

EXPOSE 8000
HEALTHCHECK --interval=10s --timeout=5s --retries=12 --start-period=60s \
  CMD curl -fsS http://localhost:8000/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
