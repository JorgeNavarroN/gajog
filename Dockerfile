# Etapa 1: Compilación con Gradle y JDK 27
FROM eclipse-temurin:27-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen ligera de ejecución
FROM eclipse-temurin:27-jre
WORKDIR /app

ENV TZ=America/Lima
RUN groupadd -r gajog && useradd -r -g gajog gajog

COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R gajog:gajog /app

USER gajog
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Duser.timezone=America/Lima", "-jar", "app.jar"]