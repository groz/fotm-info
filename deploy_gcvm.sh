#!/bin/bash

# This script is to be run from Travis CI.
# It's just a trigger for Cloud VM to pull git repo and execute local script "run.sh"
# The reason to do 'git pull' in here and not in the 'run_gcvm.sh' is to pull the
# latest version of 'run_gcvm.sh' itself.
#
# TODO: It might be better to move to Travis CI script command in .travis.yml

echo Triggering deployment script on Google Cloud VM instance...

export CLOUDSDK_CORE_DISABLE_PROMPTS=1

curl https://sdk.cloud.google.com | bash

. ~/.bashrc
ls ~/.bashrc

gcloud compute --project "fotm-info" \
               ssh fotm-canary-1 --zone "us-central1-f" \
               --command "cd fotm-info && git pull && git submodule update --recursive && chmod +x run_gcvm.sh && ./run_gcvm.sh"
