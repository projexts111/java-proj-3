# Stage 1: Build Stage
# Build stage for compiling the Java application
FROM maven:3.8.1-jdk-11 AS maven_build
WORKDIR /app

# 1. Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy source code and build the application
COPY src ./src

# ⭐️ FIX 1: Explicitly copy the PostgreSQL driver to a known location
# This ensures the driver is available regardless of the version.
RUN mvn dependency:copy-dependencies -DincludeArtifactIds=postgresql -DoutputDirectory=target/dependency

# This creates /app/target/secretsanta.war (Package name)
RUN mvn package -DskipTests 

# Stage 2: Runtime Stage
# Use a minimal Tomcat image for running the application
FROM tomcat:9.0-jdk11-openjdk-slim

# 1. Copy entrypoint script (must be done before USER switch)
COPY entrypoint.sh /usr/local/tomcat/bin/

# 2. ⭐️ FIX 2: Copy the WAR file and RENAME it to ROOT.war
# This ensures Tomcat deploys it as the default application, resolving 404/startup errors.
COPY --from=maven_build /app/target/secretsanta.war /usr/local/tomcat/webapps/ROOT.war

# 3. ⭐️ FIX 3: Copy the PostgreSQL JDBC Driver from Maven Cache to Tomcat lib directory
COPY --from=maven_build /app/target/dependency/postgresql-*.jar /usr/local/tomcat/lib/

# 4. Grant execution rights (runs as default root user)
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh 

# 5. Switch user for security
USER tomcat 

# 6. Set entrypoint and CMD
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
