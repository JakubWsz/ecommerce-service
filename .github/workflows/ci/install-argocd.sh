#!/bin/bash
set -e

echo "📦 Installing Argo CD CLI..."

curl -sSL -o argocd \
  https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64

chmod +x argocd
sudo mv argocd /usr/local/bin/

argocd version --client

echo "✅ Argo CD CLI installed successfully"