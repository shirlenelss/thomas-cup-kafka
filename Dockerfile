# Use Ubuntu as the base image
FROM ubuntu:latest

# Install OpenJDK and other dependencies
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    apt-get clean

# Set working directory
WORKDIR /app

# Copy the built jar file into the container
COPY target/*.jar app.jar

# Set environment variables for Kafka and PostgreSQL
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/thomas_cup_dev
ENV SPRING_DATASOURCE_USERNAME=thomas
ENV SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]