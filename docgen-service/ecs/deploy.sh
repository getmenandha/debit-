#!/usr/bin/env bash
# Build, push to ECR and register the task definition. Creating the ECS service, ALB and target
# group is a one-time step (usually Terraform/CloudFormation in your org) - see README "Deploy to ECS".
#
#   ACCOUNT_ID=123456789012 REGION=us-east-1 TAG=1.0.0 ./ecs/deploy.sh
set -euo pipefail
: "${ACCOUNT_ID:?set ACCOUNT_ID}" "${REGION:?set REGION}"
TAG="${TAG:-$(git rev-parse --short HEAD)}"
REPO="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/docgen-service"
cd "$(dirname "$0")/.."

aws ecr describe-repositories --repository-names docgen-service --region "$REGION" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name docgen-service --region "$REGION" >/dev/null
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "${REPO%/*}"

docker build --platform linux/amd64 -t "$REPO:$TAG" .
docker push "$REPO:$TAG"

sed -e "s/<ACCOUNT_ID>/$ACCOUNT_ID/g" -e "s/<REGION>/$REGION/g" -e "s/<TAG>/$TAG/g" \
  ecs/task-definition.json > /tmp/docgen-taskdef.json
aws ecs register-task-definition --cli-input-json file:///tmp/docgen-taskdef.json --region "$REGION" \
  --query 'taskDefinition.taskDefinitionArn' --output text

# If the service already exists:
#   aws ecs update-service --cluster <CLUSTER> --service docgen-service \
#     --task-definition docgen-service --region "$REGION"
