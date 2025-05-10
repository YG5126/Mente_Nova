# --- Стадия 1: Сборка проекта ---
FROM maven:3.9.9-eclipse-temurin-23 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests
RUN mkdir -p /app/javafx \
    && find /root/.m2/repository/org/openjfx -name "*-linux.jar" -exec cp {} /app/javafx/ \;
RUN apt-get update && apt-get install -y x11-apps
# --- Стадия 2: Среда выполнения ---
FROM openjdk:23-jdk-slim
RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    libfontconfig1 \
    libgtk-3-0 \
    libxxf86vm1 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/mente_nova-0.0.1-SNAPSHOT.jar mente_nova.jar
COPY --from=builder /app/javafx /app/javafx
ENTRYPOINT ["java", \
    "-Dprism.verbose=true", \
    "-Dprism.order=sw", \
    "-Djavafx.verbose=true", \
    "--module-path", "/app/javafx", \
    "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base", \
    "-jar", "mente_nova.jar"]