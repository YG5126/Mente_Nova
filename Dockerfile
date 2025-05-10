# Этап сборки
FROM maven:3.9.9-eclipse-temurin-23 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests
# Удаляем установку x11-apps, так как она не нужна для сборки
RUN mkdir -p /app/javafx \
    && find /root/.m2/repository/org/openjfx -name "*-linux.jar" -exec cp {} /app/javafx/ \;

# Этап выполнения
FROM openjdk:23-jdk-slim
# Установка зависимостей для JavaFX и Xvfb (для headless-режима)
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
    xvfb \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/mente_nova-0.0.1-SNAPSHOT.jar mente_nova.jar
COPY --from=builder /app/javafx /app/javafx
# Создание скрипта для поддержки Xvfb (опционально)
RUN echo '#!/bin/sh' > /app/run.sh \
    && echo 'if [ "$HEADLESS" = "true" ]; then' >> /app/run.sh \
    && echo '  Xvfb :99 -screen 0 1024x768x16 &' >> /app/run.sh \
    && echo '  export DISPLAY=:99' >> /app/run.sh \
    && echo 'fi' >> /app/run.sh \
    && echo 'java -Dprism.order=sw -Djavafx.verbose=true --module-path /app/javafx --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -jar mente_nova.jar' >> /app/run.sh \
    && chmod +x /app/run.sh
ENTRYPOINT ["/app/run.sh"]