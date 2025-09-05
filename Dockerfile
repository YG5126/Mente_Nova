FROM maven:3.9.9-eclipse-temurin-23 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY javafx-sdk-24.0.1 ./javafx-sdk-24.0.1

RUN mvn clean package -DskipTests

FROM openjdk:23-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libgl1 \
    libglu1-mesa \
    libglx0 \
    libxext6 \
    libxi6 \
    libxrender1 \
    libxrandr2 \
    libxtst6 \
    libgtk-3-0 \
    libgbm1 \
    libasound2 \
    libstdc++6 \
    libx11-6 \
    libx11-xcb1 \
    x11-apps \
    xvfb \
    libx11-dev \
    libxrender-dev \
    libxtst-dev \
    libxi-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/mente_nova-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/javafx-sdk-24.0.1 /opt/javafx-sdk

CMD ["sh", "-c", "Xvfb :99 -screen 0 1024x768x16 & export DISPLAY=:99 && java --module-path /opt/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar app.jar"]

# java --module-path ../javafx-sdk-24.0.1/lib --add-modules javafx.controls,javafx.fxml -jar mente_nova-0.0.1-SNAPSHOT.jar