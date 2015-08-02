#!/bin/bash

# stop apache if it's running for some reason
# probably because it was a demo CodeDeploy fleet
# created for WordPress app
apachectl stop

# start portal & crawler
nohup /fotm-app/portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 &
nohup java -jar /fotm-app/crawler-assembly-1.0-SNAPSHOT.jar &
