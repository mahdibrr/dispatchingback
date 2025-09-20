# Dockerfile (Maven + Java 21)

# ===== Build =====
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -U -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Ensure Spring Boot repackaging runs so the produced jar is executable.
# Some projects don't bind the repackage goal to the package phase; running
# the `spring-boot:repackage` goal ensures the jar contains the proper
# manifest/start-class.
RUN mvn -q -U -DskipTests package spring-boot:repackage

# ===== Runtime =====
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
WORKDIR /app
# Copy any produced jar (handles non-SNAPSHOT names too).
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
