# Stage 1: Build Stage (No changes needed here)
FROM maven:3.8.1-jdk-11 AS maven_build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn dependency:copy-dependencies -DincludeArtifactIds=postgresql -DoutputDirectory=target/dependency
RUN mvn package -DskipTests 

# Stage 2: Runtime Stage (Changes here)
FROM tomcat:9.0-jdk11-openjdk-slim

# 1. ⭐️ NEW: Copy the setenv.sh file into Tomcat's bin directory
COPY setenv.sh /usr/local/tomcat/bin/

# 2. Copy the WAR file and RENAME it to ROOT.war
COPY --from=maven_build /app/target/secretsanta.war /usr/local/tomcat/webapps/ROOT.war

# 3. Copy the PostgreSQL JDBC Driver
COPY --from=maven_build /app/target/dependency/postgresql-*.jar /usr/local/tomcat/lib/

# 4. Grant execution rights (runs as default root user)
RUN chmod +x /usr/local/tomcat/bin/setenv.sh 

# 5. Switch user for security
USER tomcat 

# 6. Set CMD to standard Tomcat startup (no ENTRYPOINT needed)
# The base image already has the correct ENTRYPOINT/CMD for this.
# We trust the base image's standard behavior now.

EXPOSE 8080
# Use the default image CMD which is usually ["catalina.sh", "run"]
CMD ["catalina.sh", "run"]
