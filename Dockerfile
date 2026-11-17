# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
# Descarga el wrapper y las dependencias en una capa propia (se reutiliza mientras no cambie build.gradle)
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home sga && mkdir -p /app/imagenes && chown -R sga /app
COPY --from=build --chown=sga /workspace/build/libs/app.jar app.jar
USER sga
# Render gratuito tiene 512 MB: heap limitado, recolector serial y pilas pequeñas. Hora de Colombia (regla 15).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Duser.timezone=America/Bogota"
ENV SPRING_PROFILES_ACTIVE=prod
# Render inyecta PORT; el servidor lo lee (server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
