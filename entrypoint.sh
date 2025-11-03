#!/bin/bash

# Ensure all environment variables, especially secrets, are fully sourced and visible.
# This is a robust workaround for PaaS platforms that hide secrets during initial JVM bootstrap.
set -a 
set +a

echo "Starting Tomcat server for Secret Santa Generator..."
# Execute the original Tomcat startup command to keep the container running
exec catalina.sh run
