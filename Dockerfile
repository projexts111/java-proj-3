# Stage 1: Build Stage
FROM maven:3.8.1-jdk-11 AS maven_build
WORKDIR /app

# 1. Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy source code and build the application
COPY src ./src

# ⭐️ NEW STEP: Copy PostgreSQL driver to a known location in /app
# We use the 'copy-dependencies' goal to place the driver in /app/target/dependency
# The driver artifact is 'org.postgresql:postgresql:jar:...'
RUN mvn dependency:copy-dependencies -DincludeArtifactIds=postgresql -DoutputDirectory=target/dependency

# This creates /app/target/secretsanta.war
RUN mvn package -DskipTests 

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim

# 1. Copy entrypoint script
COPY entrypoint.sh /usr/local/tomcat/bin/

# 2. Copy the WAR file
COPY --from=maven_build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# ⭐️ UPDATED COPY: Copy the driver from the new known location /app/target/dependency
# The wildcard (*) ensures we get the driver regardless of its version number.
COPY --from=maven_build /app/target/dependency/postgresql-*.jar /usr/local/tomcat/lib/

# 3. Grant execution rights
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh 

# 4. Switch user for security
USER tomcat 

# 5. Set entrypoint and CMD
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
