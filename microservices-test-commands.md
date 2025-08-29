# Microservices Test Commands

## Authentication and Token Generation

### 1. Register a New User
```bash
curl -X POST http://api.ecolight.local/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 2. Login and Get JWT Token
```bash
curl -X POST http://api.ecolight.local/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser",
    "password": "password123"
  }'
```

### 3. Refresh Token
```bash
curl -X POST http://api.ecolight.local/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

### 4. Validate Token
```bash
curl -X POST http://api.ecolight.local/api/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "your-jwt-token-here"
  }'
```

## User Service CRUD Operations

### 1. Get User Profile by ID
```bash
curl -X GET http://api.ecolight.local/api/users/{userId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 2. Get User Profile by Username
```bash
curl -X GET http://api.ecolight.local/api/users/username/{username} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 3. Get User Profile by Email
```bash
curl -X GET http://api.ecolight.local/api/users/email/{email} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 4. Create User Profile
```bash
curl -X POST http://api.ecolight.local/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "userId": "user123",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 5. Update User Profile
```bash
curl -X PUT http://api.ecolight.local/api/users/{userId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name"
  }'
```

### 6. Delete User Profile
```bash
curl -X DELETE http://api.ecolight.local/api/users/{userId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 7. List All Users
```bash
curl -X GET "http://api.ecolight.local/api/users?page=1&size=20" \
  -H "Authorization: Bearer your-jwt-token-here"
```

## Project Service CRUD Operations

### 1. Create Project
```bash
curl -X POST http://api.ecolight.local/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "title": "New Project",
    "description": "Project description",
    "status": "PLANNING",
    "priority": "MEDIUM"
  }'
```

### 2. Get Project by ID
```bash
curl -X GET http://api.ecolight.local/api/projects/{projectId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 3. Update Project
```bash
curl -X PUT http://api.ecolight.local/api/projects/{projectId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "title": "Updated Project Title",
    "status": "IN_PROGRESS"
  }'
```

### 4. Delete Project
```bash
curl -X DELETE http://api.ecolight.local/api/projects/{projectId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 5. List All Projects
```bash
curl -X GET "http://api.ecolight.local/api/projects?page=1&size=20" \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 6. List User Projects
```bash
curl -X GET http://api.ecolight.local/api/users/{userId}/projects \
  -H "Authorization: Bearer your-jwt-token-here"
```

## Chat Service CRUD Operations

### 1. Send Message
```bash
curl -X POST http://api.ecolight.local/api/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "conversationId": "conv123",
    "content": "Hello, this is a test message"
  }'
```

### 2. Get Messages from Conversation
```bash
curl -X GET "http://api.ecolight.local/api/chat/conversations/{conversationId}/messages?limit=50" \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 3. Get Conversation Details
```bash
curl -X GET http://api.ecolight.local/api/chat/conversations/{conversationId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 4. List Conversations
```bash
curl -X GET "http://api.ecolight.local/api/chat/conversations?page=1&limit=20" \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 5. Delete Message
```bash
curl -X DELETE http://api.ecolight.local/api/chat/messages/{messageId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

## Gallery Service CRUD Operations

### 1. Upload Image
```bash
curl -X POST http://api.ecolight.local/api/gallery/images \
  -H "Authorization: Bearer your-jwt-token-here" \
  -F "filename=test.jpg" \
  -F "data=@/path/to/image.jpg" \
  -F "title=Test Image" \
  -F "description=A test image"
```

### 2. Get Image by ID
```bash
curl -X GET http://api.ecolight.local/api/gallery/images/{imageId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 3. List Images
```bash
curl -X GET "http://api.ecolight.local/api/gallery/images?page=1&limit=20" \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 4. Update Image Metadata
```bash
curl -X PUT http://api.ecolight.local/api/gallery/images/{imageId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description"
  }'
```

### 5. Delete Image
```bash
curl -X DELETE http://api.ecolight.local/api/gallery/images/{imageId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 6. Create Album
```bash
curl -X POST http://api.ecolight.local/api/gallery/albums \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "title": "My Album",
    "description": "A collection of images"
  }'
```

### 7. Add Image to Album
```bash
curl -X POST http://api.ecolight.local/api/gallery/albums/{albumId}/images/{imageId} \
  -H "Authorization: Bearer your-jwt-token-here"
```

## Service Discovery and Health Checks

### 1. Check Eureka Server
```bash
curl -X GET http://localhost:8761/eureka/apps
```

### 2. Check API Gateway Health
```bash
curl -X GET http://api.ecolight.local/actuator/health
```

### 3. Check Auth Service Health
```bash
curl -X GET http://api.ecolight.local/api/auth/actuator/health
```

## Notes:
- Replace `your-jwt-token-here` with the actual JWT token obtained from login
- Replace path parameters like `{userId}`, `{projectId}`, etc. with actual IDs
- The API gateway routes requests through Istio to the appropriate services
- All services are registered with Eureka for service discovery
- JWT authentication is required for all protected endpoints
- **Service Ports**: Services may run on different ports than configured (check Eureka for actual ports)
- **Circuit Breaker**: Services may return 503 if temporarily unavailable (circuit breaker pattern)
- **Direct Access**: For testing, you can access services directly on their assigned ports:
  - Auth Service: 8085
  - User Service: 8087  
  - Project Service: 8087
  - Chat Service: [check Eureka]
  - Gallery Service: 8089
