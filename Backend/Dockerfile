# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies before copying source
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src

RUN mvn package -DskipTests --no-transfer-progress && mv target/crm-versatilis-*.jar target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]