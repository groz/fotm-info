#!/bin/bash

if [ -z "$1" ]
then
  echo "Usage:
./create_instance.sh tag [env] [instance_type]
tag = fotm-crawler OR fotm-portal
env = prod
instance_type = t2.micro | t2.small | ...
"
  exit 1
else
  TAG_KEY=$1
fi

if [ -z "$2" ]
then
  TAG_VALUE="prod"
else
  TAG_VALUE=$2
fi

if [ -z "$3" ]
then
  INSTANCE_TYPE=t2.micro
else
  INSTANCE_TYPE=$3
fi

# get current directory
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

AMI_ID=ami-1ecae776
KEY_NAME=fotm-info-keypair-us
USER_DATA_FILE=instance_setup.sh
IAM_PROFILE_NAME=FotmRole

INSTANCE_ID=$(aws ec2 run-instances \
  --image-id $AMI_ID \
  --key-name $KEY_NAME \
  --user-data file://$DIR/$USER_DATA_FILE \
  --count 1 \
  --instance-type $INSTANCE_TYPE \
  --iam-instance-profile Name=$IAM_PROFILE_NAME \
  --security-group-ids sg-dd8f32ba \
  --subnet-id subnet-b15265c6 \
  | jsawk 'return this.Instances[0].InstanceId')

echo "Created instance with id" $INSTANCE_ID

aws ec2 create-tags --resources $INSTANCE_ID --tags Key=$TAG_KEY,Value=$TAG_VALUE
