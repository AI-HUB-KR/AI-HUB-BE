# ===================================================================
# OCI 표준 컨테이너 이미지 빌드 (Spring Boot Application)
# ===================================================================
# 빌드 최적화:
#   - 멀티스테이지 빌드로 최종 이미지 크기 최소화
#   - Spring Boot Layered JAR로 레이어 캐싱 최적화
#   - Java 25 지원
#
# 컨테이너 런타임 호환성:
#   - OCI (Open Container Initiative) 표준 준수
#   - Kubernetes containerd, CRI-O, Docker 모두 지원
#   - 모든 OCI 호환 런타임에서 실행 가능
# ===================================================================

# ===================================================================
# Stage 1: Build
# ===================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper와 설정 파일 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Gradle 실행 권한 부여 (권한 누락 대비)
RUN chmod +x gradlew

# 의존성 다운로드 (캐싱 레이어)
RUN ./gradlew dependencies --no-daemon || return 0

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# Spring Boot Layered JAR 추출
RUN mkdir -p build/extracted && \
    java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

# ===================================================================
# Stage 2: Runtime
# ===================================================================
FROM eclipse-temurin:25-jre-alpine

# 런타임 유틸 설치 (healthcheck용 wget)
RUN apk add --no-cache wget

# 보안 강화: non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

WORKDIR /app

# Spring Boot Layered JAR 복사 (레이어 순서대로)
COPY --from=builder /app/build/extracted/dependencies/ ./
COPY --from=builder /app/build/extracted/spring-boot-loader/ ./
COPY --from=builder /app/build/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/build/extracted/application/ ./

# 애플리케이션 포트 노출
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
