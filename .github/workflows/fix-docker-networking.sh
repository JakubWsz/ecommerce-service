#!/bin/bash

echo "Fixing Docker networking issues for Testcontainers..."

echo "Current Docker configuration:"
docker version
docker info
docker network ls
echo "Docker bridge network details:"
docker network inspect bridge || echo "No bridge network found"

echo "Creating dedicated network for testcontainers..."
docker network create testcontainers || echo "Network already exists"

echo "Network interfaces:"
ip addr show

echo "Adding required host mappings..."
sudo sh -c 'echo "127.0.0.1 host.testcontainers.internal" >> /etc/hosts'
sudo sh -c 'echo "127.0.0.1 host.docker.internal" >> /etc/hosts'
sudo sh -c 'echo "127.0.0.1 gateway.docker.internal" >> /etc/hosts'
sudo sh -c 'echo "127.0.0.1 10.1.0.1" >> /etc/hosts'

echo "Opening necessary ports in firewall..."
sudo iptables -A INPUT -p tcp --dport 32768:35000 -j ACCEPT
sudo iptables -A OUTPUT -p tcp --dport 32768:35000 -j ACCEPT

echo "Creating Testcontainers config file..."
mkdir -p ~/.testcontainers
echo "ryuk.container.enabled=false" > ~/.testcontainers/testcontainers.properties
echo "checks.disable=true" >> ~/.testcontainers/testcontainers.properties
echo "docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy" >> ~/.testcontainers/testcontainers.properties

echo "Testcontainers configuration:"
cat ~/.testcontainers/testcontainers.properties

echo "Setting environment variables..."
echo "TESTCONTAINERS_RYUK_DISABLED=true" >> $GITHUB_ENV
echo "TESTCONTAINERS_CHECKS_DISABLE=true" >> $GITHUB_ENV
echo "DOCKER_HOST=unix:///var/run/docker.sock" >> $GITHUB_ENV
echo "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock" >> $GITHUB_ENV
echo "TESTCONTAINERS_HOST_OVERRIDE=localhost" >> $GITHUB_ENV
echo "TESTCONTAINERS_NETWORK=testcontainers" >> $GITHUB_ENV

echo "Docker network setup complete!"