FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY meetlink-server.jar app.jar
EXPOSE 9100 9200
CMD ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
