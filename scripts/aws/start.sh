#!/bin/bash

portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 &
disown

java -jar crawler-assembly-1.0-SNAPSHOT.jar &
disown
