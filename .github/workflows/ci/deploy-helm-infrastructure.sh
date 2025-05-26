#!/bin/bash
set -e

echo "Deploying infrastructure with Helm..."

echo "Creating namespaces..."
kubectl create namespace infrastructure --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -

echo "Adding Helm repositories..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add elastic https://helm.elastic.co
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm repo add jetstack https://charts.jetstack.io
helm repo update

echo "Installing cert-manager..."
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.13.0 \
  --set installCRDs=true \
  --wait

echo "Installing NGINX Ingress Controller..."
helm upgrade --install nginx-ingress bitnami/nginx-ingress-controller \
  --namespace infrastructure \
  --set service.type=LoadBalancer \
  --set controller.replicaCount=$REPLICAS \
  --wait

echo "Installing infrastructure components..."
helm upgrade --install infrastructure ./charts/infrastructure \
  --namespace infrastructure \
  --values ./charts/infrastructure/values.yaml \
  --set global.stage=$STAGE \
  --set global.replicas=$REPLICAS \
  --wait

echo "Installing Prometheus..."
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values ./charts/infrastructure/prometheus-values.yaml \
  --set prometheus.prometheusSpec.replicas=$REPLICAS \
  --wait

echo "Installing Grafana..."
helm upgrade --install grafana grafana/grafana \
  --namespace monitoring \
  --values ./charts/infrastructure/grafana-values.yaml \
  --wait

echo "Installing Jaeger..."
helm upgrade --install jaeger jaegertracing/jaeger \
  --namespace monitoring \
  --values ./charts/infrastructure/jaeger-values.yaml \
  --wait

echo "Installing FluentBit..."
helm upgrade --install fluent-bit bitnami/fluent-bit \
  --namespace monitoring \
  --values ./charts/infrastructure/fluent-bit-values.yaml \
  --wait

echo "Infrastructure deployed successfully"