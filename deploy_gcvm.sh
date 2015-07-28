#!/bin/bash

# This script is to be run from Travis CI.
# It's just a trigger for Cloud VM to pull all data and execute local script "run.sh"

echo Starting shell script
gcloud compute --project "fotm-info" ssh --zone "us-central1-f" "fotm-canary-1" --command "cd fotm-info && git pull && ./run_gcvm.sh"
echo Shell script complete.
