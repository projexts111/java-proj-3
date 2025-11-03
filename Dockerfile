# Stage 1: Build Stage
# Build stage for compiling the Java application
FROM maven:3.8.1-jdk-11 AS maven_build
WORKDIR /app

# 1. Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy source code and build the application
COPY src ./src
# This creates /app/target/secretsanta.war
RUN mvn package -DskipTests 

# Stage 2: Runtime Stage
# Use a minimal Tomcat image for running the application
FROM tomcat:9.0-jdk11-openjdk-slim

# 1. Copy entrypoint script (must be done before USER switch)
COPY entrypoint.sh /usr/local/tomcat/bin/

# 2. Copy the WAR file from the build stage 
COPY --from=maven_build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# 3. Grant execution rights (runs as default root user)
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh 

# 4. Switch user for security
USER tomcat 

# 5. Set entrypoint and CMD
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
