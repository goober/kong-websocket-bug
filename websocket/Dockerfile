FROM openjdk:11 as builder

WORKDIR /app

COPY . /app

RUN ./gradlew build --no-daemon

FROM openjdk:11

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
