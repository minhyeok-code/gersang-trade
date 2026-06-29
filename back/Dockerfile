# ── 빌드 스테이지 ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gersangtrade/gradlew .
COPY gersangtrade/gradle gradle
COPY gersangtrade/build.gradle .
COPY gersangtrade/settings.gradle .
RUN chmod +x gradlew

# 의존성 레이어 캐시 (소스 변경 시 재다운로드 방지)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY gersangtrade/src src
RUN ./gradlew bootJar --no-daemon -x test

# ── 런타임 스테이지 ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
