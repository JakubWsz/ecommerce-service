#!/bin/bash
set -e

APP_NAME="ecommerce-app"
REPO_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}.git"
REVISION="${GITHUB_REF}"
PATH="ecommerce-cdk8s/dist"

echo "🚀 Deploying with Argo CD..."
echo "Repository: $REPO_URL"
echo "Revision: $REVISION"

if ! argocd app get $APP_NAME --grpc-web 2>/dev/null; then
  echo "📦 Creating Argo CD application..."

  argocd app create $APP_NAME \
    --repo $REPO_URL \
    --revision $REVISION \
    --path $PATH \
    --dest-server https://kubernetes.default.svc \
    --dest-namespace ecommerce \
    --sync-policy automated \
    --self-heal \
    --grpc-web
else
  echo "✅ Argo CD application already exists"
fi

echo "🔄 Syncing Argo CD application..."
argocd app sync $APP_NAME --grpc-web

echo "⏳ Waiting for application to be healthy..."
argocd app wait $APP_NAME --health --timeout 600 --grpc-web

echo "✅ Application deployed successfully"