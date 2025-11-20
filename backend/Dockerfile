# 1) Java 17 기반 이미지 사용
FROM eclipse-temurin:17-jre-alpine

# 2) 작업 디렉토리
WORKDIR /app

# 3) JAR 파일 복사
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

# 4) 서버 포트
EXPOSE 8080

# 5) 실행 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
