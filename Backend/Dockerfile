# Stage 1: Build
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

RUN chmod +x mvnw && ./mvnw package -DskipTests && mv target/crm-versatilis-*.jar target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]