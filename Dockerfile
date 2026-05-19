FROM eclipse-temurin:21-jre-jammy

COPY build/libs/demo-dong-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Dhttps.protocols=TLSv1.2", "-Djdk.tls.client.protocols=TLSv1.2", "-Xmx512m", "-jar", "/app.jar"]