# Istio Service Mesh Configuration for Lovable Cline Microservices

This directory contains Istio configuration files for setting up a service mesh for the Lovable Cline microservices architecture.

## Files

### 1. gateway.yaml
- **Gateway**: Defines the ingress gateway for external traffic
- **VirtualService**: Routes traffic to appropriate microservices based on URI prefixes
- Host: `api.ecolight.local` (development domain)

### 2. destination-rules.yaml
- **DestinationRule**: Defines traffic policies for each microservice
- Enables mutual TLS between services
- Configures round-robin load balancing
- Defines subsets for canary deployments

### 3. service-entries.yaml
- **ServiceEntry**: Defines external services that can be accessed from the mesh
- Includes Google APIs, Auth0, and AWS services
- Includes SQLite database access configuration

## Installation

1. **Install Istio** (if not already installed):
```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH
istioctl install --set profile=demo -y
```

2. **Label namespace for automatic sidecar injection**:
```bash
kubectl label namespace default istio-injection=enabled
```

3. **Apply configurations**:
```bash
kubectl apply -f gateway.yaml
kubectl apply -f destination-rules.yaml
kubectl apply -f service-entries.yaml
```

## Service Routing

The gateway routes traffic as follows:
- `/api/auth` → auth-service:8082
- `/api/users` → user-service:8083  
- `/api/projects` → project-service:8084
- `/api/chat` → chat-service:8085
- `/api/gallery` → gallery-service:8086
- `/` → api-gateway:8080 (fallback)

## Security Features

- **Mutual TLS**: All service-to-service communication is encrypted
- **Traffic Policies**: Round-robin load balancing configured
- **External Access**: Controlled access to external APIs

## Development Setup

For local development, add to your `/etc/hosts`:
```
127.0.0.1 api.ecolight.local
```

## Next Steps

1. Deploy microservices with Istio sidecars
2. Configure monitoring and tracing
3. Set up circuit breakers and retry policies
4. Implement canary deployment strategies
