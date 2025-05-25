#!/bin/bash
set -e

configure_ecr_repo() {
  local repo_name=$1

  echo "🔍 Checking ECR repository: $repo_name"

  if ! aws ecr describe-repositories --repository-names $repo_name --region $AWS_REGION 2>/dev/null; then
    echo "📦 Creating ECR repository: $repo_name"

    aws ecr create-repository \
      --repository-name $repo_name \
      --region $AWS_REGION \
      --image-scanning-configuration scanOnPush=true \
      --image-tag-mutability MUTABLE
  else
    echo "✅ ECR repository $repo_name already exists"

    aws ecr put-image-scanning-configuration \
      --repository-name $repo_name \
      --image-scanning-configuration scanOnPush=true \
      --region $AWS_REGION
  fi

  echo "📋 Setting lifecycle policy for $repo_name"
  aws ecr put-lifecycle-policy \
    --repository-name $repo_name \
    --lifecycle-policy-text '{
      "rules": [
        {
          "rulePriority": 1,
          "description": "Keep last 10 production images",
          "selection": {
            "tagStatus": "tagged",
            "tagPrefixList": ["prod-"],
            "countType": "imageCountMoreThan",
            "countNumber": 10
          },
          "action": {
            "type": "expire"
          }
        },
        {
          "rulePriority": 2,
          "description": "Keep last 5 development images",
          "selection": {
            "tagStatus": "tagged",
            "tagPrefixList": ["dev-"],
            "countType": "imageCountMoreThan",
            "countNumber": 5
          },
          "action": {
            "type": "expire"
          }
        },
        {
          "rulePriority": 3,
          "description": "Keep only one latest image",
          "selection": {
            "tagStatus": "tagged",
            "tagPrefixList": ["latest"],
            "countType": "imageCountMoreThan",
            "countNumber": 1
          },
          "action": {
            "type": "expire"
          }
        },
        {
          "rulePriority": 4,
          "description": "Remove untagged images after 1 day",
          "selection": {
            "tagStatus": "untagged",
            "countType": "sinceImagePushed",
            "countUnit": "days",
            "countNumber": 1
          },
          "action": {
            "type": "expire"
          }
        },
        {
          "rulePriority": 5,
          "description": "Remove all images older than 30 days",
          "selection": {
            "tagStatus": "any",
            "countType": "sinceImagePushed",
            "countUnit": "days",
            "countNumber": 30
          },
          "action": {
            "type": "expire"
          }
        }
      ]
    }' \
    --region $AWS_REGION || true
}

SERVICES=(
  "ecommerce-customer-read"
  "ecommerce-customer-write"
)

for service in "${SERVICES[@]}"; do
  configure_ecr_repo "$service"
done

echo "✅ ECR repositories configured successfully"