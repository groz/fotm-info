#!/bin/bash

# http://docs.aws.amazon.com/codedeploy/latest/userguide/app-spec-ref.html

# this file is to be executed from CI environment
# it prepares revision/artifact/revision.zip file to be deployed to S3
# and used by AWS CloudDeploy

# prepare revision directory
rm -rf revision
mkdir -p revision/artifact

# copy portal & crawler binaries into revision dir
unzip crawler/target/universal/crawler-1.0-SNAPSHOT.zip -d revision
unzip portal/target/universal/portal-1.0-SNAPSHOT.zip -d revision

# copy scripts
mkdir -p revision/scripts
cp scripts/aws/* revision/scripts/

# copy AWS deployment description file
cp scripts/aws/appspec.yml revision/

# zip all into one revision.zip bundle that will be deployed to S3
cd revision
zip -r artifact/revision.zip *
