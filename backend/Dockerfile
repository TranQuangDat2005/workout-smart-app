# Stage 1: build JAR với Maven (repo không có mvnw)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B package -DskipTests

# Stage 2: runtime JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN adduser -D -u 1000 app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads/media && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
