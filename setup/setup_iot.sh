#!/bin/bash

export REGION="us-central1"
export PROJECT="sandbox-keyera-poc"
export rsaPath="/home/michail/rsa_public.pem"

export topicIoT="foglamp-demo"

gcloud iot registries create foglamp-registry \
    --project=${PROJECT} \
    --region=${REGION} \
    --event-notification-config=topic=${topicIoT}


gcloud iot devices create foglamp-device \
    --project=${PROJECT} \
    --region=${REGION} \
    --registry=foglamp-registry \
    --public-key path=${rsaPath},type=rsa-pem