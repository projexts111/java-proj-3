# Stage 1: Build Stage
FROM maven:3.8.1-jdk-11 AS maven_build  # <-- FIX: Renamed stage here
WORKDIR /app
# 1. Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy source code and build the application
COPY src ./src
RUN mvn package -DskipTests 

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim

# 1. Copy necessary files (still running as ROOT user)
COPY entrypoint.sh /usr/local/tomcat/bin/

# 2. Copy the WAR file from the build stage 
COPY --from=maven_build /app/target/secretsanta.war /usr/local/tomcat/webapps/  # <-- FIX: Renamed stage reference here

# 3. Grant execution rights 
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh 

# 4. Switch user for execution (Security measure)
USER tomcat 

# 5. Set entrypoint and CMD
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
