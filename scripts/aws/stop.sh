#!/bin/bash

# stop running crawler
ps aux | grep "[c]rawler" | awk '{print $2}' | xargs kill -KILL

# stop running portal app
cat /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID | xargs kill -SIGTERM
rm -f /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID

# should we do 'rm -rf /fotm-app'?
