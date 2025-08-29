# Microservices Docker Setup

This directory contains Docker configuration for running all LovableCline microservices using Docker Compose.

## Prerequisites

- Docker Desktop (or Docker Engine) installed
- Docker Compose installed
- Java 17 JDK (for building JAR files)
- Maven (for building JAR files)

## Services Overview

The Docker Compose setup includes the following services:

1. **Eureka Server** (8761) - Service discovery and registry
2. **API Gateway** (8080) - Central entry point for all API requests
3. **Auth Service** (8082) - User authentication and JWT token management
4. **User Service** (8083) - User profile management
5. **Project Service** (8085 → 8082) - Project management
6. **Chat Service** (8086 → 8083) - Real-time messaging
7. **Gallery Service** (8084) - Image and media management

## Quick Start

### Option 1: Automated Build and Run

```bash
# Make the build script executable
chmod +x build-docker-images.sh

# Build all Docker images
./build-docker-images.sh

# Start all services
docker-compose up -d
```

### Option 2: Manual Build and Run

```bash
# Build JAR files first (if not already built)
cd microservices
for service in */; do
  cd "$service"
  mvn clean package -DskipTests
  cd ..
done

# Build Docker images
docker-compose build

# Start services
docker-compose up -d
```

## Accessing Services

Once all services are running, you can access them at:

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8082
- **User Service**: http://localhost:8083
- **Project Service**: http://localhost:8085
- **Chat Service**: http://localhost:8086
- **Gallery Service**: http://localhost:8084

## Health Checks

All services include health check endpoints:
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8761/actuator/health  # Eureka Server
curl http://localhost:8082/actuator/health  # Auth Service
# ... etc
```

## Service Discovery

All microservices register with Eureka server. You can view registered services at:
http://localhost:8761/eureka/apps

## Environment Variables

Key environment variables used:

- `SPRING_PROFILES_ACTIVE=docker` - Activates Docker profile
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/` - Eureka service discovery
- `JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-docker` - JWT secret for authentication

## Port Mapping

| Service | Container Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| Eureka Server | 8761 | 8761 | Service discovery |
| API Gateway | 8080 | 8080 | Main API entry point |
| Auth Service | 8082 | 8082 | Authentication |
| User Service | 8083 | 8083 | User management |
| Project Service | 8082 | 8085 | Project management |
| Chat Service | 8083 | 8086 | Messaging |
| Gallery Service | 8084 | 8084 | Media management |

## Useful Commands

```bash
# Start all services in detached mode
docker-compose up -d

# View logs
docker-compose logs -f
docker-compose logs [service-name]

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build

# Check service status
docker-compose ps

# Execute command in running container
docker-compose exec [service-name] /bin/bash

# View resource usage
docker stats
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure no other services are using ports 8080-8086, 8761
2. **Build failures**: Make sure Maven can build each service successfully
3. **Service registration**: Check Eureka dashboard at http://localhost:8761
4. **Health check failures**: Services may take time to start, check logs

### Checking Logs

```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs auth-service
docker-compose logs eureka-server

# Follow logs in real-time
docker-compose logs -f
```

### Database Persistence

- SQLite databases are stored within containers (ephemeral)
- H2 databases are in-memory (data lost on restart)
- Gallery uploads are persisted using Docker volumes

## Development Notes

- Services use Spring Boot 3.2.0 with Java 17
- All services include Actuator endpoints for monitoring
- Circuit breaker pattern implemented for fault tolerance
- JWT authentication required for protected endpoints

## Security Considerations

- Change the default JWT secret in production
- Use HTTPS in production environments
- Consider adding network security policies
- Regularly update base images for security patches

## Production Deployment

For production deployment, consider:

1. Using a proper database (PostgreSQL, MySQL) instead of SQLite/H2
2. Implementing proper secret management (Vault, Kubernetes secrets)
3. Adding monitoring and alerting (Prometheus, Grafana)
4. Setting up proper logging (ELK stack)
5. Implementing CI/CD pipelines
6. Adding resource limits and health checks
7. Using a reverse proxy (Nginx, Traefik)
8. Implementing proper backup strategies
