#!/bin/bash

# http://docs.aws.amazon.com/codedeploy/latest/userguide/app-spec-ref.html#app-spec-ref-hooks

if [ "$APPLICATION_NAME" == "portal" ]
then
  /fotm-app/portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 > /dev/null 2> /dev/null < /dev/null &
fi

if [ "$APPLICATION_NAME" == "crawler" ]
then
  java -jar /fotm-app/crawler-assembly-1.0-SNAPSHOT.jar > /dev/null 2> /dev/null < /dev/null &
fi

if [ "$APPLICATION_NAME" == "storage" ]
then
  # TODO start storage actor
fi
