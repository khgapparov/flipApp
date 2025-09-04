#!/bin/bash

# Expected services based on your docker-compose
EXPECTED_SERVICES=("api-gateway" "auth-service" "user-service" "property-service" "project-service" "gallery-service" "chat-service")
CONSUL_URL="http://localhost:8500"

echo "=== CONSUL SERVICE HEALTH CHECK ==="
echo ""

# Get all registered services from Consul
echo "1. Checking registered services in Consul..."
REGISTERED_SERVICES=$(curl -s "${CONSUL_URL}/v1/catalog/services" | jq -r 'keys[]' | grep -vE '^consul$' | sort)

if [ -z "$REGISTERED_SERVICES" ]; then
    echo "❌ ERROR: No services found registered with Consul"
    exit 1
fi

echo "✅ Registered services found:"
echo "$REGISTERED_SERVICES"
echo ""

# Check if all expected services are registered
echo "2. Verifying all expected services are registered..."
MISSING_SERVICES=()
for service in "${EXPECTED_SERVICES[@]}"; do
    if ! echo "$REGISTERED_SERVICES" | grep -q "^${service}$"; then
        MISSING_SERVICES+=("$service")
    fi
done

if [ ${#MISSING_SERVICES[@]} -eq 0 ]; then
    echo "✅ All expected services are registered with Consul"
else
    echo "❌ Missing services: ${MISSING_SERVICES[*]}"
fi
echo ""

# Check health status of each service
echo "3. Checking health status of each service..."
ALL_HEALTHY=true

for service in $REGISTERED_SERVICES; do
    HEALTH_STATUS=$(curl -s "${CONSUL_URL}/v1/health/service/${service}?passing" | jq -r '.[].Checks[] | select(.Status == "passing") | .ServiceName' | head -1)
    
    if [ "$HEALTH_STATUS" = "$service" ]; then
        echo "✅ $service: HEALTHY"
    else
        echo "❌ $service: UNHEALTHY or NO PASSING CHECKS"
        ALL_HEALTHY=false
    fi
done
echo ""

# Summary
echo "=== SUMMARY ==="
if [ ${#MISSING_SERVICES[@]} -eq 0 ] && $ALL_HEALTHY; then
    echo "🎉 SUCCESS: All expected services are registered and healthy!"
    exit 0
else
    echo "⚠️  ISSUES DETECTED:"
    [ ${#MISSING_SERVICES[@]} -gt 0 ] && echo "   - Missing services: ${MISSING_SERVICES[*]}"
    ! $ALL_HEALTHY && echo "   - Some services are unhealthy"
    exit 1
fi
