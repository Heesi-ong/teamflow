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
# JAVA_OPTS는 기본 비워둔다 — CPU가 넉넉한 EC2(prod)/로컬에서는 풀 JIT(C2)가 장기 실행 처리량에
# 유리하다. CPU가 극도로 제한된 플랫폼(Render 프리티어)만 배포 환경변수로 티어링을 낮춰 콜드스타트를
# 줄인다 (render.yaml 참고).
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
