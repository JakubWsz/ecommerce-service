#!/bin/bash
set -e

echo "Running smoke tests..."

ENDPOINT=$(kubectl get ingress ecommerce-ingress -n ecommerce \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)

if [ -z "$ENDPOINT" ]; then
  ENDPOINT=$(kubectl get ingress ecommerce-ingress -n ecommerce \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
fi

if [ -z "$ENDPOINT" ]; then
  echo "No ingress endpoint found, trying service endpoints..."
  ENDPOINT=$(kubectl get svc nginx-ingress-controller -n infrastructure \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
fi

if [ -z "$ENDPOINT" ]; then
  echo "Could not find any endpoint"
  kubectl get ingress -A
  kubectl get svc -A | grep LoadBalancer
  exit 1
fi

echo "Testing endpoint: $ENDPOINT"

echo "Waiting for endpoint to be ready..."
for i in {1..30}; do
  if curl -f -m 10 http://$ENDPOINT/ -o /dev/null -s; then
    break
  fi
  echo -n "."
  sleep 10
done
echo ""

SERVICES=(
  "customer-read"
  "customer-write"
)

TEST_FAILED=false

for service in "${SERVICES[@]}"; do
  echo "Testing $service health..."

  HEALTH_URL="http://$ENDPOINT/$service/actuator/health"
  echo "  Testing: $HEALTH_URL"

  if curl -f -m 10 $HEALTH_URL; then
    echo "$service is healthy"
  else
    echo "  Trying direct service endpoint..."
    SERVICE_IP=$(kubectl get svc $service -n ecommerce -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")

    if [ ! -z "$SERVICE_IP" ] && curl -f -m 10 http://$SERVICE_IP:8080/actuator/health; then
      echo "$service is healthy (direct)"
    else
      echo "$service health check failed"
      kubectl logs -n ecommerce deployment/$service --tail=50
      TEST_FAILED=true
    fi
  fi
  echo ""
done

echo "Testing API documentation..."
for service in "${SERVICES[@]}"; do
  SWAGGER_URL="http://$ENDPOINT/$service/swagger-ui.html"
  echo "  Testing: $SWAGGER_URL"

  if curl -f -m 10 $SWAGGER_URL -o /dev/null -s; then
    echo "$service API docs available"
  else
    if curl -f -m 10 http://$ENDPOINT/$service/swagger-ui/index.html -o /dev/null -s; then
      echo "$service API docs available (alternative URL)"
    else
      echo "$service API docs not available"
    fi
  fi
done

echo "Testing Prometheus metrics..."
for service in "${SERVICES[@]}"; do
  METRICS_URL="http://$ENDPOINT/$service/actuator/prometheus"
  if curl -f -m 10 $METRICS_URL -o /dev/null -s; then
    echo "$service metrics available"
  else
    echo "$service metrics not available"
  fi
done

echo "Pod status:"
kubectl get pods -n ecommerce

echo "Recent events:"
kubectl get events -n ecommerce --sort-by='.lastTimestamp' | tail -10

if [ "$TEST_FAILED" = true ]; then
  echo "Some tests failed"
  exit 1
else
  echo "All smoke tests passed!"
fi