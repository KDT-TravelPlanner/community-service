FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 실행 전용 사용자(UID/GID 10001 고정 — Kubernetes securityContext와 맞물린다)와
# 로그 디렉터리를 만든다. application-prod.yml이 /var/log/travel-planner 에 파일 로그를
# 쓰므로, spring 사용자가 그 디렉터리에 쓸 수 있어야 한다.
RUN addgroup -S -g 10001 spring \
    && adduser -S -D -H -u 10001 -G spring spring \
    && mkdir -p /var/log/travel-planner \
    && chown -R spring:spring /var/log/travel-planner

# 호스트에서 ./gradlew bootJar 로 미리 만든 JAR만 넣는다. 소스·Gradle·GitHub Packages
# 토큰은 이미지에 남기지 않는다(travel-common 인증은 이 이미지 밖 Gradle 단계에서 끝난다).
COPY --chown=spring:spring build/libs/app.jar /app/app.jar

USER spring

# 앱 포트. 관리 포트 9091은 Compose/Kubernetes에서 명시적으로 매핑한다.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
