#!/bin/bash
#this script replaces gcp project name and project number in tf files.
#you might need to update path of terraform files inn lines 17, 20 and 23.

echo "Enter your project id:"
read -r PROJECT_ID

echo "Enter your project number:"
read -r PROJECT_NUMBER

PROJECT_ID=$(echo "$PROJECT_ID" | tr '[:upper:]' '[:lower:]')
PROJECT_NUMBER=$(echo "$PROJECT_NUMBER" | tr '[:upper:]' '[:lower:]')

echo "=== Replacing project id variable across files ==="

echo "set project id in  terraform/variables.tfvars"
sed -i .backup "s/<project>/$PROJECT_ID/g" ../../terraform/variables.tfvars

echo "set project id in  terraform/main.tf"
sed -i .backup "s/<project>/$PROJECT_ID/g" ../../terraform/main.tf

echo "set project id in  terraform/scripts/setup_gcp_infra.sh"
sed -i .backup "s/<project>/$PROJECT_ID/g" ../../terraform/scripts/setup_gcp_infra.sh

echo "set project number in  terraform/variables.tfvars"
sed -i .backup "s/<project_number>/$PROJECT_NUMBER/g" ../../terraform/variables.tfvars
