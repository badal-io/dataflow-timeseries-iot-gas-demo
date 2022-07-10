#!/bin/bash -x
#can't work on purpose if initiate_terraform_project.sh has not been run

#in case of errors we stop the script
set -euf -o pipefail

export PROJECT_ID="iot-poc-354821"

echo "=== Project Organization ID==="
gcloud projects describe "$PROJECT_ID" | grep id

echo "=== Activate_service ==="
./activate_services.sh

echo "=== Configuring terraform state repo ==="
BUCKET_FOR_STATE="gs://$PROJECT_ID-terraform-state"
echo "creation du bucket state terraform"
gsutil mb -c regional -l us-central1 "$BUCKET_FOR_STATE" || true

gcloud compute networks create default

