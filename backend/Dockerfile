FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build --chown=app:app /workspace/target/faculty-evaluation-common-0.1.0.jar app.jar
EXPOSE 8080
USER app
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD sh -c 'wget -qO- http://127.0.0.1:8080/api/health || exit 1'
ENTRYPOINT ["java","-jar","/app/app.jar"]
