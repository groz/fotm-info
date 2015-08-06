#!/bin/bash

# this script deploys portal app to staging env
# it is to be used from the local machine's root
# add --ignore-application-stop-failures to `aws deploy` lines if stop script is broken
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

sbt crawler/dist portal/dist &&
$DIR/bundle.sh &&
aws s3 cp revision/artifact/revision.zip s3://fotm-info-staging-bucket/latest/revision.zip &&
aws deploy create-deployment --application-name fotm-info --deployment-group-name fotm-portal  --s3-location bucket=fotm-info-staging-bucket,bundleType=zip,key=latest/revision.zip --ignore-application-stop-failures &&
aws deploy create-deployment --application-name fotm-info --deployment-group-name fotm-crawler --s3-location bucket=fotm-info-staging-bucket,bundleType=zip,key=latest/revision.zip --ignore-application-stop-failures &&
open https://us-east-1.console.aws.amazon.com/codedeploy/home?region=us-east-1#/deployments/
