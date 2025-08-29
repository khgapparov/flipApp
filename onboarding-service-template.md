# Service Onboarding Template

## New Service Integration Guide

Use this template to document and onboard new microservices into the LovableCline ecosystem. This ensures consistency with the existing architecture and proper integration with all components.

---

## Service Information

### Basic Service Details
- **Service Name**: `[service-name]-service` (e.g., `notification-service`)
- **Service Description**: [Brief description of the service's purpose and functionality]
- **Domain**: [Business domain this service belongs to]
- **Team/Owner**: [Team or individual responsible for this service]

### Technical Specifications
- **Port**: `[assigned-port]` (e.g., `8087`)
- **Base Path**: `/api/[service-name]` (e.g., `/api/notifications`)
- **Java Package**: `com.lovablecline.[service-name]`
- **Database**: [SQLite/PostgreSQL/MySQL/None]

---

## API Gateway Configuration

### Route Configuration
Add the following route to `microservices/api-gateway/src/main/resources/application.yml`:

```yaml
- id: [service-name]-service
  uri: lb://[service-name]-service
  predicates:
    - Path=/api/[service-name]/**
  filters:
    - name: CircuitBreaker
      args:
        name: [serviceName]Service
        fallbackUri: forward:/fallback/[service-name]
    - name: JwtAuthenticationFilter
```

### Circuit Breaker Configuration
Add circuit breaker configuration:

```yaml
[serviceName]Service:
  registerHealthIndicator: true
  slidingWindowSize: 10
  minimumNumberOfCalls: 5
  waitDurationInOpenState: 10000
  failureRateThreshold: 50
```

---

## Istio Configuration

### Virtual Service Routing
Add to `istio-config/gateway.yaml`:

```yaml
- match:
  - uri:
      prefix: /api/[service-name]
  route:
  - destination:
      host: [service-name]-service
      port:
        number: [port-number]
```

### Destination Rules
Add to `istio-config/destination-rules.yaml`:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: [service-name]-service
  namespace: default
spec:
  host: [service-name]-service
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
    loadBalancer:
      simple: ROUND_ROBIN
  subsets:
  - name: v1
    labels:
      version: v1
```

---

## Service Discovery (Eureka)

### Application Configuration
Ensure your service registers with Eureka by adding to `application.yml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://admin:eureka-password@localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

### Service Naming Convention
- **Spring Application Name**: `[service-name]-service`
- **Eureka Instance ID**: `${spring.application.name}:${spring.application.instance_id:${random.value}}`

---

## Smithy API Definition

### Basic Service Structure
Create `src/main/smithy/[service-name]-service.smithy`:

```smithy
$version: "2.0"

namespace com.lovablecline.[service-name]

use aws.protocols#restJson1
use smithy.framework#ValidationException

/// [Service description]
@restJson1
service [ServiceName]Service {
    version: "2024-01-01",
    operations: [
        // List your operations here
    ]
}

// Add your structures and operations below
```

### Operation Examples
```smithy
@http(method: "POST", uri: "/api/[service-name]")
@documentation("[Operation description]")
operation Create[Resource] {
    input: Create[Resource]Request,
    output: Create[Resource]Response,
    errors: [ValidationException, ResourceAlreadyExistsError]
}

@http(method: "GET", uri: "/api/[service-name]/{id}")
@documentation("Get [resource] by ID")
operation Get[Resource] {
    input: Get[Resource]Request,
    output: Get[Resource]Response,
    errors: [ValidationException, ResourceNotFoundError]
}
```

---

## Security Configuration

### JWT Authentication
Ensure your service validates JWT tokens. The API gateway handles authentication, but services should:

1. **Validate incoming tokens** (if needed for service-specific authorization)
2. **Extract user information** from JWT claims
3. **Implement proper authorization** based on user roles/permissions

### Security Best Practices
- Use `@PreAuthorize` annotations for method-level security
- Validate all input parameters
- Implement proper error handling
- Use HTTPS in production
- Rotate JWT secrets regularly

---

## Database Configuration

### SQLite Configuration (Default)
```yaml
spring:
  datasource:
    url: jdbc:sqlite:[service-name]_service.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update
```

### Alternative Databases
For other databases, update the configuration accordingly and ensure proper connection pooling.

---

## Monitoring and Observability

### Actuator Endpoints
Enable standard Spring Boot Actuator endpoints:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
```

### Custom Metrics
Implement custom metrics using Micrometer:

```java
@Bean
MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> registry.config().commonTags(
        "application", "[service-name]-service",
        "region", "us-east-1"
    );
}
```

### Logging Configuration
```yaml
logging:
  level:
    com.lovablecline.[service-name]: DEBUG
  file:
    name: logs/[service-name]-service.log
    max-size: 10MB
    max-history: 7
```

---

## Testing Requirements

### Unit Tests
- Cover all business logic
- Mock external dependencies
- Test edge cases and error conditions

### Integration Tests
- Test API endpoints
- Verify database interactions
- Test service discovery integration

### Load Testing
- Define performance benchmarks
- Test under expected load patterns
- Monitor resource usage

---

## Deployment Checklist

### Pre-Deployment
- [ ] All tests passing
- [ ] API documentation complete
- [ ] Performance benchmarks met
- [ ] Security review completed
- [ ] Monitoring configured
- [ ] Logging configured
- [ ] Circuit breakers tested

### Post-Deployment
- [ ] Service registered with Eureka
- [ ] API gateway routing working
- [ ] Istio routing configured
- [ ] Health checks passing
- [ ] Metrics being collected
- [ ] Alerts configured

---

## Troubleshooting Guide

### Common Issues

1. **Service Not Registering with Eureka**
   - Check Eureka server connectivity
   - Verify service name configuration
   - Check network policies

2. **API Gateway Routing Issues**
   - Verify route configuration
   - Check service discovery
   - Validate Istio virtual services

3. **JWT Authentication Problems**
   - Verify JWT secret matches API gateway
   - Check token validation logic
   - Validate claim extraction

4. **Database Connectivity**
   - Check database file permissions
   - Verify connection string
   - Test database migrations

### Debug Endpoints
```bash
# Check service health
curl http://localhost:[port]/actuator/health

# Check Eureka registration
curl http://localhost:8761/eureka/apps/[service-name]-service

# Check API gateway routes
curl http://localhost:8080/actuator/gateway/routes
```

---

## Versioning and Updates

### API Versioning
- Use URI versioning: `/api/v1/[service-name]/...`
- Maintain backward compatibility
- Document breaking changes

### Database Migrations
- Use Flyway or similar for schema management
- Test migrations thoroughly
- Backup before applying changes

### Service Updates
- Blue-green deployment strategy
- Canary releases for critical services
- Monitor performance during updates

---

## Contact Information

- **Primary Contact**: [Name] - [Email]
- **Secondary Contact**: [Name] - [Email]
- **On-call Rotation**: [Schedule details]
- **Slack Channel**: `#[service-name]-service`

## Links
- [GitHub Repository](https://github.com/org/repo)
- [API Documentation](https://api-docs.example.com)
- [Monitoring Dashboard](https://grafana.example.com)
- [Error Tracking](https://sentry.example.com)

---

*Last Updated: [Date]*
*Version: 1.0.0*
