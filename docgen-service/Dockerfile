# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
# Corporate networks: if Maven Central / releases.aspose.com are not reachable, copy your
# settings.xml (Artifactory/Nexus mirror) to /root/.m2/settings.xml here.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:17-jre-jammy
# Fonts matter: Aspose lays out pages with the fonts it finds. Templates use Arial/Calibri-metric
# fonts; Liberation and Carlito are metric-compatible substitutes. For pixel-identical output with
# Word, copy your licensed corporate fonts into /usr/share/fonts/corporate (or set DOCGEN_FONTS_DIR).
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl fontconfig fonts-liberation fonts-liberation2 \
      fonts-dejavu-core fonts-crosextra-carlito fonts-crosextra-caladea \
 && fc-cache -f \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --home /app docgen

WORKDIR /app
COPY --from=build /src/target/docgen-service.jar app.jar
USER 10001
EXPOSE 8080

# Container-aware heap; Aspose layout of large documents is memory-hungry, keep headroom.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
