#!/bin/bash

# http://docs.aws.amazon.com/codedeploy/latest/userguide/app-spec-ref.html#app-spec-ref-hooks
echo "Setting fotm env to prod"
export FOTM_ENV=prod-env

if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-portal" ]
then
  echo "starting portal"
  JAVA_OPTS="-Xmx1g" /fotm-app/portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 > /dev/null 2> /dev/null < /dev/null &
fi

echo "starting crawler"
if [ "$DEPLOYMENT_GROUP_NAME" == "fotm-crawler" ]
then
  JAVA_OPTS="-Xmx2g" /fotm-app/crawler-1.0-SNAPSHOT/bin/crawler > /dev/null 2> /dev/null < /dev/null &
fi
