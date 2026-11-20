# Match this to <java.version> in your pom.xml
ARG JAVA_VERSION=21

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper and pom first, so downloaded libraries are cached between builds
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /workspace/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]