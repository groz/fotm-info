#!/bin/bash

# update java 8
yum -y update
yum -y install java-1.8.0-openjdk.x86_64

# use java 8
alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
