# ───── build stage ─────
FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /workspace/server

# 1) кэшируем pom и maven wrapper
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
RUN mvn -B -ntp dependency:resolve

# 2) копируем бекенд и фронтенд
COPY src/ ./src/
COPY frontend/ ./frontend/

# 3) чистим и собираем JAR (static попадёт в BOOT-INF/classes/static)
RUN mvn -B -ntp -DskipTests clean package

# ───── runtime stage ─────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/server/target/Crocodile_Game-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
