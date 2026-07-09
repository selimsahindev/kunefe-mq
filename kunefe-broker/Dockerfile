# Stage 1: Build
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :kunefe-broker:bootJar -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/kunefe-broker/build/libs/*.jar app.jar

EXPOSE 6565

ENTRYPOINT ["java", "-jar", "app.jar"]