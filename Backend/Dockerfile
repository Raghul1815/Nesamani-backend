# Use Java 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/nesamani-backend-1.0.0.jar app.jar

# Run app
ENTRYPOINT ["java","-jar","/app/app.jar"]