#!/bin/bash
set -e

echo "🧪 Running smoke tests..."

ENDPOINT=$(kubectl get ingress ecommerce-ingress -n ecommerce \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

if [ -z "$ENDPOINT" ]; then
  echo "❌ Could not find ingress endpoint"
  exit 1
fi

echo "📍 Testing endpoint: $ENDPOINT"

SERVICES=(
  "customer-read"
  "customer-write"
)

TEST_FAILED=false
for service in "${SERVICES[@]}"; do
  echo "🔍 Testing $service health..."

  if curl -f -m 10 http://$ENDPOINT/$service/actuator/health; then
    echo "✅ $service is healthy"
  else
    echo "❌ $service health check failed"
    TEST_FAILED=true
  fi

  echo ""
done

echo "🔍 Testing API documentation..."
for service in "${SERVICES[@]}"; do
  if curl -f -m 10 http://$ENDPOINT/$service/swagger-ui.html -o /dev/null -s; then
    echo "✅ $service API docs available"
  else
    echo "⚠️  $service API docs not available"
  fi
done

if [ "$TEST_FAILED" = true ]; then
  echo "❌ Some tests failed"
  exit 1
else
  echo "✅ All smoke tests passed!"
fi