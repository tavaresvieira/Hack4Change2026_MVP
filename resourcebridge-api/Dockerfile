# ============================
# ETAPA 1 — Build do frontend
# ============================
FROM node:20 AS frontend-build

WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ .
RUN npm run build


# ============================
# ETAPA 2 — Build do backend
# ============================
FROM maven:3.9.8-eclipse-temurin-21 AS backend-build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src

RUN chmod +x mvnw

# Build do backend
RUN ./mvnw -B -DskipTests package

# Copia o frontend para dentro do JAR (static/)
RUN mkdir -p target/classes/static
COPY --from=frontend-build /frontend/build/ ./target/classes/static/


# ============================
# ETAPA 3 — Runtime
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=backend-build /app/target/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]