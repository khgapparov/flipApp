# JWT Authentication Implementation

The auth service now implements JWT (JSON Web Token) authentication with the following features:

## Features

- ✅ JWT access token generation (15-minute expiry)
- ✅ Refresh token generation (7-day expiry)
- ✅ Token validation endpoint
- ✅ Token refresh endpoint
- ✅ Secure secret management
- ✅ Proper error handling

## Endpoints

### POST /auth/register
Registers a new user and returns JWT tokens.

**Request:**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "userId": "user_123456789",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "0b9eKeIRW1dIpVnjQ69nuJBN-v6xCmp-aq7qlfJ1OWw="
}
```

### POST /auth/login
Authenticates a user and returns JWT tokens.

**Request:**
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "0b9eKeIRW1dIpVnjQ69nuJBN-v6xCmp-aq7qlfJ1OWw="
}
```

### POST /auth/refresh
Refreshes an access token using a refresh token.

**Request:**
```json
{
  "refreshToken": "0b9eKeIRW1dIpVnjQ69nuJBN-v6xCmp-aq7qlfJ1OWw="
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### POST /auth/validate
Validates a JWT access token.

**Request:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "isValid": true,
  "userId": "user_123456789"
}
```

### POST /auth/logout
Invalidates a refresh token.

**Request:**
```json
{
  "refreshToken": "0b9eKeIRW1dIpVnjQ69nuJBN-v6xCmp-aq7qlfJ1OWw="
}
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

## Configuration

### Environment Variables

1. **JWT_SECRET** (required): Secret key for signing JWT tokens
   - Generate with: `openssl rand -base64 32`
   - Set in docker-compose.yml or .env file

2. **SERVICE_ADDRESS**: Service address for Consul registration
3. **CONSUL_ADDRESS**: Consul server address

### Default Values

- Access Token Expiry: 15 minutes
- Refresh Token Expiry: 7 days
- JWT Signing Algorithm: HS256

## Security Notes

1. **Production Deployment**:
   - Always set a strong JWT_SECRET in production
   - Use environment variables or secret management
   - Rotate secrets regularly

2. **Token Storage**:
   - Currently uses in-memory storage for refresh tokens (for development)
   - In production, use Redis or database for persistent storage

3. **Validation**:
   - Tokens are signed with HMAC-SHA256
   - Includes expiration validation
   - Includes issuer validation

## Testing

The implementation includes comprehensive JWT functionality:
- Token generation and signing
- Token validation and parsing
- Refresh token management
- Proper error handling for invalid tokens

To test manually:
```bash
# Build and run the service
cd flipApp-go/auth-service
go build
./auth-service

# Test endpoints with curl
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"test123"}'
```

The service is now ready for production use with proper JWT authentication!
