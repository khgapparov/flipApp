# FlipApp Docker Service Troubleshooting Guide

## System Overview
- **API Gateway**: localhost:9000
- **Auth Service**: localhost:8080  
- **Project Service**: Internal port 8083 (via API gateway)
- **Consul**: localhost:8500 (service discovery)
- **PostgreSQL**: localhost:5432

## Step 1: Verify Services are Running

```bash
# Check Docker containers status
cd flipApp-go && docker-compose ps

# Check API gateway health
curl -X GET http://localhost:9000/api/health

# Check Consul UI (open in browser)
open http://localhost:8500

# Check individual service health endpoints
curl -X GET http://localhost:8080/health        # Auth service
curl -X GET http://localhost:8081/health        # User service  
curl -X GET http://localhost:8082/health        # Property service
curl -X GET http://localhost:8083/health        # Project service
curl -X GET http://localhost:8084/health        # Gallery service
curl -X GET http://localhost:8085/health        # Chat service
```

## Step 2: Authentication Flow Testing

### Register a New User
```bash
curl --location 'localhost:9000/api/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "test123"
}'
```

### Login to Get JWT Token
```bash
curl --location 'localhost:9000/api/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
    "email": "test@example.com",
    "password": "test123"
}'
```

Save the access token from the response for subsequent requests.

### Validate Token (Optional)
```bash
curl --location 'localhost:9000/api/auth/validate' \
--header 'Content-Type: application/json' \
--data-raw '{
    "token": "YOUR_JWT_TOKEN_HERE"
}'
```

## Step 3: Project CRUD Operations Testing

### Create a Project (POST)
```bash
curl --location 'localhost:9000/api/projects' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
    "propertyId": "property-123",
    "projectName": "My First Flip",
    "budget": 50000.00
}'
```

Save the projectId from the response for subsequent operations.

### Get a Project (GET)
```bash
curl --location 'localhost:9000/api/projects/PROJECT_ID_HERE' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

### List All Projects (GET)
```bash
curl --location 'localhost:9000/api/projects' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

### Update Project Status (PUT)
```bash
curl --location --request PUT 'localhost:9000/api/projects/PROJECT_ID_HERE/status' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
    "status": "ACTIVE"
}'
```

### Add Project Member (POST)
```bash
curl --location 'localhost:9000/api/projects/PROJECT_ID_HERE/members' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
    "userId": "user_123",
    "role": "OWNER"
}'
```

### List Project Members (GET)
```bash
curl --location 'localhost:9000/api/projects/PROJECT_ID_HERE/members' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
```

## Step 4: Error Scenario Testing

### Test without Authentication
```bash
curl --location 'localhost:9000/api/projects' \
--header 'Content-Type: application/json' \
--data '{
    "propertyId": "property-123",
    "projectName": "Test Project"
}'
```

### Test with Invalid Token
```bash
curl --location 'localhost:9000/api/projects' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer invalid_token_here' \
--data '{
    "propertyId": "property-123",
    "projectName": "Test Project"
}'
```

### Test Missing Required Fields
```bash
curl --location 'localhost:9000/api/projects' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN_HERE' \
--data '{
    "projectName": "Test Project"
}'
```

## Step 5: Log Checking Commands

### View API Gateway Logs
```bash
docker logs flipapp-go-api-gateway-1 --follow
docker logs flipapp-go-api-gateway-1 --tail 50
```

### View Auth Service Logs
```bash
docker logs flipapp-go-auth-service-1 --follow
docker logs flipapp-go-auth-service-1 --tail 50
```

### View Project Service Logs
```bash
docker logs flipapp-go-project-service-1 --follow
docker logs flipapp-go-project-service-1 --tail 50
```

### View Consul Logs
```bash
docker logs flipapp-go-consul-1 --follow
docker logs flipapp-go-consul-1 --tail 50
```

### View All Service Logs
```bash
cd flipApp-go && docker-compose logs --follow
cd flipApp-go && docker-compose logs --tail 50
```

### View Specific Timeframe Logs
```bash
cd flipApp-go && docker-compose logs --since 5m
cd flipApp-go && docker-compose logs --since 2025-08-31T22:00:00
```

## Step 6: Service Discovery Verification

### Check Consul Service Catalog
```bash
curl http://localhost:8500/v1/catalog/services
```

### Check Project Service Health
```bash
curl http://localhost:8500/v1/health/service/project-service
```

### Check API Gateway Service Health
```bash
curl http://localhost:8500/v1/health/service/api-gateway
```

## Common Issues and Solutions

### 1. JWT Token Issues
- **Symptom**: 401 Unauthorized errors
- **Solution**: Ensure JWT_SECRET matches between auth-service and api-gateway
- **Check**: Verify token generation and validation

### 2. Service Discovery Issues
- **Symptom**: "Service not available" errors
- **Solution**: Check Consul registration and health checks
- **Check**: Verify services are registered in Consul UI

### 3. Database Connection Issues
- **Symptom**: Database-related errors in logs
- **Solution**: Check PostgreSQL container health and connection strings

### 4. Port Conflicts
- **Symptom**: Services failing to start
- **Solution**: Check for port conflicts and update docker-compose.yml if needed

## Quick Health Check Script

```bash
#!/bin/bash
echo "=== FlipApp Health Check ==="

# Check containers
echo "Containers:"
docker-compose ps

# Check API gateway
echo -e "\nAPI Gateway:"
curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/api/health

# Check Consul
echo -e "\nConsul Services:"
curl -s http://localhost:8500/v1/catalog/services | jq .

echo -e "\nHealth Check Complete"
```

Save this as `healthcheck.sh` and run: `chmod +x healthcheck.sh && ./healthcheck.sh`
