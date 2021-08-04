#!/bin/bash

mkdir ~/foglamp_temp
cd ~/foglamp_temp

openssl genpkey -algorithm RSA -out rsa_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in rsa_private.pem -pubout -out rsa_public.pem

wget https://pki.goog/roots.pem
cp roots.pem /usr/local/fledge/data/etc/certs/
cp rsa_private.pem /usr/local/fledge/data/etc/certs/

gcloud pubsub topics create foglamp-demo --project=${PROJECT}
export TOPIC_IOT="foglamp-demo"
export RSA_PATH="~/foglamp_temp/rsa_public.pem"

gcloud pubsub topics create foglamp-demo-raw --project=${PROJECT}
gcloud pubsub topics create foglamp-demo-events --project=${PROJECT}


gcloud iot registries create foglamp-registry \
    --project=${PROJECT} \
    --region=${REGION} \
    --event-notification-config=topic=${TOPIC_IOT}


gcloud iot devices create foglamp-device \
    --project=${PROJECT} \
    --region=${REGION} \
    --registry=foglamp-registry \
    --public-key path=${RSA_PATH},type=rsa-pem