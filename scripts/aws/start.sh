#!/bin/bash

# start portal
/fotm-app/portal-1.0-SNAPSHOT/bin/portal -Dhttp.port=80 > /dev/null 2> /dev/null < /dev/null &
