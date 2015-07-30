#!/bin/bash

# This script is to be run from Google Cloud VM to redeploy application.

echo Starting deployment script from Google Cloud VM instance...

# find crawler & portal running processes & kill them
ps aux | grep [c]rawler | awk '{print $2}' | xargs kill -KILL
ps aux | grep [p]ortal | awk '{print $2}' | xargs kill -KILL

# rewrite files
rm -rf ~/active
mkdir -p ~/active
cp -f ~/deployment/* ~/active/.

#start play
cd active

# clean running_pid
rm portal-1.0-SNAPSHOT/RUNNING_PID

# unzip play app & start it at port 80 & disown
unzip portal-1.0-SNAPSHOT.zip
portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 &
disown

# start crawler & disown
java -jar crawler-assembly-1.0-SNAPSHOT.jar &
disown

echo Deployment script complete.
