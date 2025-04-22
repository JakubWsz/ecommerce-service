#!/bin/bash

set -e

echo "CI=true" >> $GITHUB_ENV
echo "SPRING_PROFILES_ACTIVE=test" >> $GITHUB_ENV
echo "KAFKA_BOOTSTRAP_SERVERS=kafka:9092" >> $GITHUB_ENV
echo "SPRING_MAIN_ALLOW_BEAN_DEFINITION_OVERRIDING=true" >> $GITHUB_ENV
echo "TESTCONTAINERS_RYUK_DISABLED=true" >> $GITHUB_ENV

sudo sh -c 'echo "127.0.0.1 host.testcontainers.internal" >> /etc/hosts'
sudo sh -c 'echo "127.0.0.1 kafka" >> /etc/hosts'
sudo sh -c 'echo "127.0.0.1 zookeeper" >> /etc/hosts'

mkdir -p ~/.testcontainers
echo "ryuk.container.enabled=false" > ~/.testcontainers/testcontainers.properties
echo "host.testcontainers.internal.host=localhost" >> ~/.testcontainers/testcontainers.properties

docker network create test-network || true

docker run -d --name zookeeper --hostname zookeeper --network test-network \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:7.5.0

sleep 5

docker run -d --name kafka --hostname kafka --network test-network \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  confluentinc/cp-kafka:7.5.0

sleep 10
echo "=== Zookeeper status ==="
docker ps | grep zookeeper
echo "=== Kafka status ==="
docker ps | grep kafka
