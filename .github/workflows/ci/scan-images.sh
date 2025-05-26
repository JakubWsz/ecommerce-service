#!/bin/bash
set -e

check_scan_results() {
  local repo_name=$1
  local tag=$2

  echo "🔍 Waiting for scan results for $repo_name:$tag..."

  for i in {1..60}; do
    SCAN_STATUS=$(aws ecr describe-image-scan-findings \
      --repository-name $repo_name \
      --image-id imageTag=$tag \
      --region $AWS_REGION \
      --query 'imageScanStatus.status' \
      --output text 2>/dev/null || echo "IN_PROGRESS")

    if [ "$SCAN_STATUS" = "COMPLETE" ]; then
      break
    elif [ "$SCAN_STATUS" = "FAILED" ]; then
      echo "⚠️  Image scan failed for $repo_name:$tag"
      return 1
    fi

    echo -n "."
    sleep 5
  done

  echo ""

  FINDINGS=$(aws ecr describe-image-scan-findings \
    --repository-name $repo_name \
    --image-id imageTag=$tag \
    --region $AWS_REGION \
    --query 'imageScanFindings.findingSeverityCounts' \
    --output json)

  echo "📊 Scan results for $repo_name:$tag:"
  echo "$FINDINGS" | jq '.'

  CRITICAL=$(echo "$FINDINGS" | jq '.CRITICAL // 0')
  HIGH=$(echo "$FINDINGS" | jq '.HIGH // 0')
  MEDIUM=$(echo "$FINDINGS" | jq '.MEDIUM // 0')
  LOW=$(echo "$FINDINGS" | jq '.LOW // 0')

  echo "Summary: CRITICAL=$CRITICAL, HIGH=$HIGH, MEDIUM=$MEDIUM, LOW=$LOW"

  if [ "$CRITICAL" -gt 0 ]; then
    echo "❌ Found $CRITICAL CRITICAL vulnerabilities!"
    return 1
  elif [ "$HIGH" -gt 5 ]; then
    echo "⚠️  Found $HIGH HIGH vulnerabilities (threshold: 5)"
  else
    echo "✅ Image scan passed"
  fi
}

SERVICES=(
  "ecommerce-customer-read"
  "ecommerce-customer-write"
)

SCAN_FAILED=false
for service in "${SERVICES[@]}"; do
  if ! check_scan_results "$service" "$VERSION"; then
    SCAN_FAILED=true
  fi
done

if [ "$SCAN_FAILED" = true ]; then
  echo "❌ Some images failed security scan"
  exit 1
else
  echo "✅ All images passed security scan"
fi