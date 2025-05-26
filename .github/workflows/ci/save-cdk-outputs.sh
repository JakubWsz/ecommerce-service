#!/bin/bash
set -e

echo "Saving CDK outputs..."

OUTPUTS_FILE="/tmp/cdk-outputs-$STAGE.json"

echo "{}" > $OUTPUTS_FILE

KAFKA_SERVERS=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-messaging-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`KafkaBootstrapServers`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$KAFKA_SERVERS" ]; then
  jq --arg kafka "$KAFKA_SERVERS" '.KafkaBootstrapServers = $kafka' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

POSTGRES_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-database-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`PostgresEndpoint`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$POSTGRES_ENDPOINT" ]; then
  jq --arg postgres "$POSTGRES_ENDPOINT" '.PostgresEndpoint = $postgres' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

REDIS_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-database-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`RedisEndpoint`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$REDIS_ENDPOINT" ]; then
  jq --arg redis "$REDIS_ENDPOINT" '.RedisEndpoint = $redis' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

MONGODB_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-database-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`MongoDBEndpoint`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$MONGODB_ENDPOINT" ]; then
  jq --arg mongodb "$MONGODB_ENDPOINT" '.MongoDBEndpoint = $mongodb' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

VPC_ID=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-network-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`VpcId`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$VPC_ID" ]; then
  jq --arg vpc "$VPC_ID" '.VpcId = $vpc' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

EKS_CLUSTER=$(aws cloudformation describe-stacks \
  --stack-name ecommerce-kubernetes-$STAGE \
  --query 'Stacks[0].Outputs[?OutputKey==`ClusterName`].OutputValue' \
  --output text 2>/dev/null || echo "")

if [ ! -z "$EKS_CLUSTER" ]; then
  jq --arg eks "$EKS_CLUSTER" '.EksClusterName = $eks' $OUTPUTS_FILE > tmp.json && mv tmp.json $OUTPUTS_FILE
fi

echo "CDK outputs saved to: $OUTPUTS_FILE"
cat $OUTPUTS_FILE

echo "CDK outputs saved successfully"