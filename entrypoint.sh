#!/bin/bash

# Ensure all environment variables are fully sourced and visible
# This is the single most effective way to solve "silent crash" issues on PaaS.
set -a 
source /etc/profile # Or the relevant platform-specific profile/sourcing command
set +a

echo "Starting Tomcat server..."
# Execute the original Tomcat startup command
exec catalina.sh run
