#!/bin/bash

# stop running portal app
cat /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID | xargs kill -SIGTERM

# delete RUNNING_PID file if it exists
rm -f /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID

# should we do 'rm -rf /fotm-app'?
