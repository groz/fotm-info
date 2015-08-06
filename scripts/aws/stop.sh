#!/bin/bash

if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-portal" ]
then
  kill $(cat /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID); sleep 1
  rm -f /fotm-app/portal-1.0-SNAPSHOT/RUNNING_PID
fi

if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-crawler" ]
then
  ps aux | grep "[c]rawler" | awk '{print $2}' | xargs kill; sleep 1
fi
