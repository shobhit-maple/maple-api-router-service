FROM openjdk:slim
COPY build/libs/maple-api-router-service-1.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]