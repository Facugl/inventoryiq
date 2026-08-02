# --- Etapa 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos solo el pom primero para cachear las dependencias
# (si no cambia el pom, Docker no vuelve a descargar todo en el próximo build)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Etapa 2: runtime (imagen liviana, sin Maven ni JDK completo) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]