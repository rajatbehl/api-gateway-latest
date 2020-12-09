FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/api-gateway-0.0.1-SNAPSHOT.jar /app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar","/app.jar"]
