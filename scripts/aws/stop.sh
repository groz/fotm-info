#!/bin/bash

ps aux | grep [c]rawler | awk '{print $2}' | xargs kill -KILL
ps aux | grep [p]ortal | awk '{print $2}' | xargs kill -KILL

# clean running_pid
rm portal-1.0-SNAPSHOT/RUNNING_PID
