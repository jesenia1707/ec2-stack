# syntax=docker/dockerfile:1
# =====================================================================
# BFF Coffee Flower + productor Kafka (Spring Boot 3.4.4, Java 21).
# Se buildea en el propio EC2, igual que los consumers del stack.
#
#   build   -> eclipse-temurin:21-jdk-alpine (~190MB, capa descartada)
#   runtime -> eclipse-temurin:21-jre-alpine (~95MB, solo JRE)
# =====================================================================

# ---------------------------------------------------- build (JDK completo)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Primero los archivos de build para aprovechar la cache de capas de Docker
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY src/ src/

RUN ./gradlew bootJar --no-daemon

# ---------------------------------------------------- runtime (solo JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage para convivir bien con otros contenedores en un EC2 chico
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
