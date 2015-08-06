#!/bin/bash

if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-portal" ]
then
  sleep 30          # give play time to start
  curl 127.0.0.1/healthcheck
fi
