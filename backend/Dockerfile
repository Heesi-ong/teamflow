FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S teamflow && adduser -S teamflow -G teamflow
COPY --from=build /app/build/libs/*.jar app.jar
USER teamflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
