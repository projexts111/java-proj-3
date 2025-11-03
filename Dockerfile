# Stage 1: Build Stage
FROM maven:3.8.1-jdk-11 AS build
WORKDIR /app
# 1. Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy source code and build the application
COPY src ./src
RUN mvn package -DskipTests  <-- THIS CREATES /app/target/secretsanta.war

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim

USER tomcat

# 3. Copy the entrypoint script
COPY entrypoint.sh /usr/local/tomcat/bin/

# 4. Copy the WAR file from the build stage (THIS IS WHERE YOUR ERROR OCCURRED)
COPY --from=build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# 5. Grant execution rights and set entrypoint
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
