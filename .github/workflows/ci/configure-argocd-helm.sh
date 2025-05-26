#!/bin/bash
set -e

APP_NAME="ecommerce-$STAGE"
REPO_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}.git"
REVISION="${GITHUB_REF_NAME}"

echo "Configuring Argo CD for Helm charts..."
echo "Repository: $REPO_URL"
echo "Revision: $REVISION"

if ! kubectl get namespace argocd &>/dev/null; then
  echo "Installing Argo CD..."
  kubectl create namespace argocd
  kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

  echo "Waiting for Argo CD to be ready..."
  kubectl wait --for=condition=available --timeout=300s \
    deployment/argocd-server -n argocd
fi

ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d)

kubectl port-forward svc/argocd-server -n argocd 8080:443 &
PORTFORWARD_PID=$!
sleep 5

argocd login localhost:8080 \
  --username admin \
  --password $ARGOCD_PASSWORD \
  --insecure \
  --grpc-web

echo "📦 Creating Argo CD applications..."

argocd app create $APP_NAME-customer-read \
  --repo $REPO_URL \
  --revision $REVISION \
  --path helm/charts/customer-read \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace ecommerce \
  --helm-set image.tag=$VERSION \
  --helm-set image.registry=$ECR_REGISTRY \
  --helm-set global.stage=$STAGE \
  --sync-policy automated \
  --self-heal \
  --grpc-web \
  --upsert

argocd app create $APP_NAME-customer-write \
  --repo $REPO_URL \
  --revision $REVISION \
  --path helm/charts/customer-write \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace ecommerce \
  --helm-set image.tag=$VERSION \
  --helm-set image.registry=$ECR_REGISTRY \
  --helm-set global.stage=$STAGE \
  --sync-policy automated \
  --self-heal \
  --grpc-web \
  --upsert

argocd app create $APP_NAME-infrastructure \
  --repo $REPO_URL \
  --revision $REVISION \
  --path helm/charts/infrastructure \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace infrastructure \
  --helm-set global.stage=$STAGE \
  --sync-policy manual \
  --grpc-web \
  --upsert

echo "Syncing applications..."
argocd app sync $APP_NAME-customer-read --grpc-web
argocd app sync $APP_NAME-customer-write --grpc-web

echo "Waiting for applications to be healthy..."
argocd app wait $APP_NAME-customer-read --health --timeout 600 --grpc-web
argocd app wait $APP_NAME-customer-write --health --timeout 600 --grpc-web

kill $PORTFORWARD_PID 2>/dev/null || true

echo "Argo CD configured successfully"