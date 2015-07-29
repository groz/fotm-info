#!/bin/bash

# This script is to be run from Google Cloud VM to redeploy application.

echo Starting deployment script from Google Cloud VM instance...

# find all running sbt processes & kill them
ps aux | grep [s]bt-launch | awk '{print $2}' | xargs kill -9

# clean running_pid
rm portal/target/universal/stage/RUNNING_PID

# start new ones and disown
sbt clean compile

#sbt 'portal/run -Dhttp.port=80' &
sbt 'portal/run' &
disown

echo Deployment script complete.
