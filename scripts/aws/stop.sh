#!/bin/bash

# stop running portal app
cat /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID | xargs kill -SIGTERM

# stop running portal & crawler
ps aux | grep "[p]ortal" | awk '{print $2}' | xargs kill -9
ps aux | grep "[c]rawler" | awk '{print $2}' | xargs kill -9

# delete RUNNING_PID file if it exists
rm -f /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID

# should we do 'rm -rf /fotm-app'?
