# Build context: raíz del repo (no ./backend), para poder copiar data/csv
# más abajo — ver .dockerignore y render.yaml/docker-compose.yml.

# --- Etapa 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos solo el pom primero para cachear las dependencias
# (si no cambia el pom, Docker no vuelve a descargar todo en el próximo build)
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# --- Etapa 2: runtime (imagen liviana, sin Maven ni JDK completo) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# En Docker Compose local esto lo pisa el bind mount de data/csv (ver
# docker-compose.yml). En Render (sin disco persistente en el plan free)
# es la única forma de que el backend tenga los CSV al arrancar —
# quedan embebidos en la imagen, de solo lectura para ese entorno.
COPY data/csv /app/data/csv

EXPOSE 8080
# PORT lo define Render en runtime; en Compose local no está seteada,
# así que cae al default 8080 (igual que antes).
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]