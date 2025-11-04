#!/bin/bash
# Tomcat automatically sources this script.

# Read environment variables set by the platform
DB_HOST=${DB_HOST}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# Pass the variables to the JVM as System Properties (most robust method)
# This guarantees the AppDAO code can find them via System.getenv() or System.getProperty()
export JAVA_OPTS="$JAVA_OPTS \
    -DDB_HOST=$DB_HOST \
    -DDB_NAME=$DB_NAME \
    -DDB_USERNAME=$DB_USERNAME \
    -DDB_PASSWORD=$DB_PASSWORD"

# You no longer need to call catalina.sh run here; Tomcat handles that.
# The Docker image's default CMD will handle the Tomcat startup now.
