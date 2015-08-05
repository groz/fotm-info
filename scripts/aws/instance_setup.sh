#!/bin/bash

# this script is to be provided as user data for aws instance creation

# update all packages
yum -y update

# install & use java 8
yum -y install java-1.8.0-openjdk.x86_64
alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java

# install CodeDeploy agent
yum install -y ruby
yum install -y aws-cli
cd /home/ec2-user
aws s3 cp s3://aws-codedeploy-us-east-1/latest/install . --region us-east-1
chmod +x ./install
./install auto
