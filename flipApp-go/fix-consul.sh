#!/bin/bash

# Script to fix Consul connection issues in all services

SERVICES=("gallery-service" "project-service" "chat-service" "user-service")

for service in "${SERVICES[@]}"; do
    echo "Fixing Consul configuration in $service..."
    
    # Add os import
    sed -i '' 's/import (/import (\
    "os"/' "$service/main.go"
    
    # Update Consul configuration
    sed -i '' 's/config := consul.DefaultConfig()/config := consul.DefaultConfig()\
    consulAddr := os.Getenv("CONSUL_ADDRESS")\
    if consulAddr == "" {\
        consulAddr = "consul:8500"\
    }\
    config.Address = consulAddr/' "$service/main.go"
    
    # Update service address and health check
    sed -i '' 's/Address: "127.0.0.1",/serviceAddr := os.Getenv("SERVICE_ADDRESS")\
    if serviceAddr == "" {\
        serviceAddr = SERVICE_NAME\
    }\
    Address: serviceAddr,/' "$service/main.go"
    
    sed -i '' 's|http://127.0.0.1:|http://%s:|' "$service/main.go"
    sed -i '' 's|fmt.Sprintf("http://127.0.0.1:%d/health"|fmt.Sprintf("http://%s:%d/health", serviceAddr|' "$service/main.go"
    
    # Update success message
    sed -i '' 's|fmt.Printf("Successfully registered service.*|fmt.Printf("Successfully registered service '\''%s'\'' with Consul at %s\\n", SERVICE_NAME, consulAddr)|' "$service/main.go"
    
    echo "Fixed $service"
done

echo "All services have been updated with proper Consul configuration!"
