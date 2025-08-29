#!/bin/bash

# Build script for microservices Docker images
echo "Building Docker images for all microservices..."

# Build each service
services=("eureka-server" "api-gateway" "auth-service" "user-service" "project-service" "chat-service" "gallery-service")

for service in "${services[@]}"; do
    echo "Building $service..."
    cd "$service"
    
    # Build the JAR file first (if not already built)
    if [ ! -f "target/*.jar" ]; then
        echo "Building JAR for $service..."
        mvn clean package -DskipTests
    fi
    
    # Build Docker image
    docker build -t "$service:latest" .
    
    cd ..
    echo "$service image built successfully!"
    echo "-----------------------------------"
done

echo "All Docker images built successfully!"
echo ""
echo "To start all services, run:"
echo "docker-compose up -d"
echo ""
echo "To view logs:"
echo "docker-compose logs -f"
