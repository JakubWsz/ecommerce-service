#!/bin/bash
set -e

echo "🚀 Deploying base infrastructure to Kubernetes..."

mvn clean install

echo "📦 Deploying base infrastructure components..."
cdk8s synth -a "mvn exec:java -Dexec.mainClass=pl.ecommerce.k8s.BaseInfrastructureMain"
kubectl apply -f dist/

echo "⏳ Waiting for cert-manager to be ready..."
kubectl wait --for=condition=available --timeout=300s \
  deployment/cert-manager -n cert-manager

echo "📦 Deploying application infrastructure..."
cdk8s synth
kubectl apply -f dist/

echo "✅ Base infrastructure deployed successfully"