# Dockerfile for Northstar Banking System
# Multi-stage build with Eclipse Temurin Java 21

# Stage 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven pom.xml and download dependencies (cached layer)
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install tini for proper signal handling
RUN apk add --no-cache tini

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Make JAR readable by non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose the application port
EXPOSE 8080

# Use tini as entrypoint for proper signal handling
ENTRYPOINT ["tini", "--"]

# Run the Spring Boot application
CMD ["java", "-jar", "/app/app.jar"]