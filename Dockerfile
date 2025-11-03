# ... (rest of the runtime stage)

# Stage 2: Runtime Stage
FROM tomcat:9.0-jdk11-openjdk-slim

# Note: We must run the file copy and permission change *before* switching to 'USER tomcat' 
# or temporarily switch back to root for the RUN command.

# 3. Copy the entrypoint script
COPY entrypoint.sh /usr/local/tomcat/bin/

# 4. Copy the WAR file from the build stage 
COPY --from=build /app/target/secretsanta.war /usr/local/tomcat/webapps/

# 5. Grant execution rights as ROOT user (Default before USER tomcat)
# We MUST do this BEFORE the USER tomcat instruction!
RUN chmod +x /usr/local/tomcat/bin/entrypoint.sh 

# 6. Switch user for execution (Security measure)
USER tomcat 

# 7. Set entrypoint and CMD
ENTRYPOINT ["/usr/local/tomcat/bin/entrypoint.sh"]

EXPOSE 8080
CMD [""]
