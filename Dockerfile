# Stage 1: Build Stage
FROM maven:3.8.1-jdk-11 AS build
# ... (rest of build stage remains the same) ...

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim

USER tomcat

# Copy the entrypoint script and the built WAR
COPY entrypoint.sh /usr/local/tomcat/bin/
COPY --from=build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# Grant execution rights
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh

# Change the entrypoint to run our script instead of the default Tomcat command
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080

# CMD is now just passed as an argument to the ENTRYPOINT
CMD [""]
