# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21.0.8_9-jdk-alpine AS build

WORKDIR /workspace
COPY . .

ARG GRADLE_PROJECT
ARG APP_DIRECTORY
RUN test -n "$GRADLE_PROJECT" && test -n "$APP_DIRECTORY" \
    && ./gradlew --no-daemon --stacktrace "$GRADLE_PROJECT:bootJar" -x test \
    && find "$APP_DIRECTORY/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp '{}' /tmp/application.jar \; \
    && test -s /tmp/application.jar

FROM eclipse-temurin:21.0.8_9-jre-alpine

RUN addgroup -S -g 10001 easy1auth && adduser -S -D -H -u 10001 -G easy1auth easy1auth
WORKDIR /opt/easy1auth
COPY --from=build --chown=10001:10001 /tmp/application.jar ./application.jar

USER 10001:10001
EXPOSE 18848 18850
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/tmp", "-jar", "/opt/easy1auth/application.jar"]
