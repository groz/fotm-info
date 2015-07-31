#!/bin/bash

# this file is to be executed from CI environment

# prepare revision directory
rm -rf revision
rm revision.zip
mkdir -p revision

mkdir -p revision/
cp crawler/target/scala-2.11/crawler-assembly-1.0-SNAPSHOT.jar revision/

unzip portal/target/universal/portal-1.0-SNAPSHOT.zip -d revision

mkdir -p revision/scripts
cp scripts/aws/* revision/scripts/

cp scripts/aws/appspec.yml revision/

# zip all into one revision.zip bundle to be deployed to S3
cd revision
mkdir -p artifact
zip -r artifact/revision.zip *
