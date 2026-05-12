# 1. 빌드 환경 설정 (자바 버전 확인!)
FROM eclipse-temurin:21-jre-jammy

# 2. JAR 파일을 컨테이너 내부로 복사
# Gradle 빌드 시 생성되는 SNAPSHOT.jar 파일을 app.jar라는 이름으로 복사한다.
COPY build/libs/demo-dong-0.0.1-SNAPSHOT.jar app.jar

# 3. 서버 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]
