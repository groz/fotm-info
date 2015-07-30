#!/bin/bash

# This script is to be run from Travis CI.

echo Triggering deployment script on Google Cloud VM instance...

export CLOUDSDK_CORE_DISABLE_PROMPTS=1

curl https://sdk.cloud.google.com | bash

/home/travis/google-cloud-sdk/bin/gcloud auth \
               activate-service-account "${GC_SERVICE_ACCOUNT}" \
               --key-file fotm-info-project-b8031120a9c7.json \
               --project "fotm-info"

/home/travis/google-cloud-sdk/bin/gcloud compute copy-files crawler/target/scala-2.11/crawler-assembly-1.0-SNAPSHOT.jar portal/target/universal/portal-1.0-SNAPSHOT.zip run_gcvm.sh \
              fotm-canary-1:~/deployment --zone "us-central1-f"

/home/travis/google-cloud-sdk/bin/gcloud compute \
               --project "fotm-info" \
               ssh fotm-canary-1 --zone "us-central1-f" \
               --command "chmod +x deployment/run_gcvm.sh && deployment/run_gcvm.sh"
