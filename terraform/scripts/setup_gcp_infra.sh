#!/bin/bash -x
#can't work on purpose if initiate_terraform_project.sh has not been run
#gcp project has to be created first, and linked to a billing account

#in case of errors we stop the script
set -euf -o pipefail

export PROJECT_ID="iot-poc-354821"

echo "=== Activate_service ==="
./activate_services.sh

echo "=== Configuring terraform state repo ==="
BUCKET_FOR_STATE="gs://$PROJECT_ID-terraform-state"
gsutil mb -c regional -l us-central1 "$BUCKET_FOR_STATE" || true

echo "=== Creating default compute network ==="
gcloud compute networks create default
gcloud compute firewall-rules create default-allow-icmp --network default --allow icmp --source-ranges 0.0.0.0/0
gcloud compute firewall-rules create default-allow-ssh --network default --allow tcp:22 --source-ranges 0.0.0.0/0

echo "=== Executing terraform scripts ==="
#to be in terraform folder
cd ..
terraform init
terraform apply -var-file="variables.tfvars" -auto-approve



