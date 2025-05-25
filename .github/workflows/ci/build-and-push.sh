#!/bin/bash
set -e

if [ "$BRANCH_NAME" = "master" ]; then
  TAG_PREFIX="prod"
elif [ "$BRANCH_NAME" = "develop" ]; then
  TAG_PREFIX="dev"
else
  TAG_PREFIX="pr"
fi

echo "🏷️  Using tag prefix: $TAG_PREFIX"

build_and_push() {
  local service=$1
  local dockerfile=$2

  echo "🔨 Building $service..."

  docker build \
    -f $dockerfile \
    -t $ECR_REGISTRY/$service:$VERSION \
    -t $ECR_REGISTRY/$service:latest \
    -t $ECR_REGISTRY/$service:$TAG_PREFIX-$VERSION \
    -t $ECR_REGISTRY/$service:$TAG_PREFIX-latest \
    .

  echo "📤 Pushing $service images..."

  docker push $ECR_REGISTRY/$service:$VERSION
  docker push $ECR_REGISTRY/$service:latest
  docker push $ECR_REGISTRY/$service:$TAG_PREFIX-$VERSION
  docker push $ECR_REGISTRY/$service:$TAG_PREFIX-latest

  echo "✅ Successfully pushed $service"
}

build_and_push "ecommerce-customer-read" "customer-read/Dockerfile"
build_and_push "ecommerce-customer-write" "customer-write/Dockerfile"

echo "✅ All images built and pushed successfully"