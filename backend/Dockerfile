FROM gradle:9.6.1-jdk21 AS build

WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle --no-daemon dependencies
COPY src ./src
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre

RUN groupadd --system kitradar && useradd --system --gid kitradar kitradar
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER kitradar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
