#!/bin/bash
set -e

echo "🚀 Deploying applications with Helm..."

if [ -f "/tmp/cdk-outputs-$STAGE.json" ]; then
  echo "Loading CDK outputs..."
  KAFKA_BOOTSTRAP_SERVERS=$(jq -r '.KafkaBootstrapServers' /tmp/cdk-outputs-$STAGE.json)
  POSTGRES_ENDPOINT=$(jq -r '.PostgresEndpoint' /tmp/cdk-outputs-$STAGE.json)
  REDIS_ENDPOINT=$(jq -r '.RedisEndpoint' /tmp/cdk-outputs-$STAGE.json)
  MONGODB_ENDPOINT=$(jq -r '.MongoDBEndpoint' /tmp/cdk-outputs-$STAGE.json)
fi

COMMON_VALUES="--set global.stage=$STAGE \
  --set global.aws.region=$AWS_REGION \
  --set global.domain=$DOMAIN_NAME \
  --set image.tag=$VERSION \
  --set image.registry=$ECR_REGISTRY"

INFRA_VALUES=""
if [ ! -z "$KAFKA_BOOTSTRAP_SERVERS" ]; then
  INFRA_VALUES="$INFRA_VALUES --set kafka.bootstrapServers=$KAFKA_BOOTSTRAP_SERVERS"
fi
if [ ! -z "$POSTGRES_ENDPOINT" ]; then
  INFRA_VALUES="$INFRA_VALUES --set postgres.endpoint=$POSTGRES_ENDPOINT"
fi
if [ ! -z "$REDIS_ENDPOINT" ]; then
  INFRA_VALUES="$INFRA_VALUES --set redis.endpoint=$REDIS_ENDPOINT"
fi
if [ ! -z "$MONGODB_ENDPOINT" ]; then
  INFRA_VALUES="$INFRA_VALUES --set mongodb.endpoint=$MONGODB_ENDPOINT"
fi

echo "📦 Deploying customer-read..."
helm upgrade --install customer-read ./charts/customer-read \
  --namespace ecommerce \
  --values ./charts/customer-read/values.yaml \
  $COMMON_VALUES \
  $INFRA_VALUES \
  --set replicaCount=$REPLICAS \
  --wait

echo "📦 Deploying customer-write..."
helm upgrade --install customer-write ./charts/customer-write \
  --namespace ecommerce \
  --values ./charts/customer-write/values.yaml \
  $COMMON_VALUES \
  $INFRA_VALUES \
  --set replicaCount=$REPLICAS \
  --wait

echo "📦 Configuring ingress..."
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  namespace: ecommerce
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\$2
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - $DOMAIN_NAME
    - api.$DOMAIN_NAME
    secretName: ecommerce-tls
  rules:
  - host: api.$DOMAIN_NAME
    http:
      paths:
      - path: /customer-read(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: customer-read
            port:
              number: 8080
      - path: /customer-write(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: customer-write
            port:
              number: 8080
EOF

echo "Applications deployed successfully"