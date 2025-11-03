# Stage 1: Build Stage
FROM maven:3.8.1-jdk-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
# Build the WAR file
RUN mvn package -DskipTests

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim
# CRITICAL FIX: The 'tomcat' user/group does not exist in the slim image's chown database.
# The base Tomcat image already manages the necessary directory permissions for the 'tomcat' user.
# We only need to switch the user.

USER tomcat  # <-- This line is correct and switches the context to the tomcat user

# Copy the built WAR from the build stage to Tomcat's webapps directory
COPY --from=build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# Expose the standard Tomcat port
EXPOSE 8080

# The default tomcat entrypoint will run
CMD ["catalina.sh", "run"]
