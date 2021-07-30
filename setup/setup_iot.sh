#!/bin/bash

gcloud iot registries create foglamp-registry \
    --project=${PROJECT} \
    --region=${REGION} \
    --event-notification-config=topic=${topicIoT}


gcloud iot devices create foglamp-device \
    --project=${PROJECT} \
    --region=${REGION} \
    --registry=foglamp-registry \
    --public-key path=${rsaPath},type=rsa-pem