#!/bin/bash

cd ~

/fotm-app/portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 &
disown

java -jar /fotm-app/crawler-assembly-1.0-SNAPSHOT.jar &
disown
