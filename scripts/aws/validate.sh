#!/bin/bash

if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-portal" ]
then
  sleep 20          # give play time to start
  curl 127.0.0.1
fi
