# Auth Service API

This document provides `curl` commands for interacting with the Auth Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing.

## Endpoints

### Register

Registers a new user.

```bash
curl -X POST http://localhost:9000/api/auth/register \
-H "Content-Type: application/json" \
-d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
}'
```

### Login

Logs in a user and returns an access token and a refresh token.

```bash
curl -X POST http://localhost:9000/api/auth/login \
-H "Content-Type: application/json" \
-d '{
    "email": "test@example.com",
    "password": "password123"
}'
```

### Refresh Token

Refreshes an access token using a refresh token.

```bash
curl -X POST http://localhost:9000/api/auth/refresh \
-H "Content-Type: application/json" \
-d '{
    "refreshToken": "your_refresh_token"
}'
```

### Logout

Logs out a user.

```bash
curl -X POST http://localhost:9000/api/auth/logout \
-H "Content-Type: application/json" \
-d '{
    "refreshToken": "your_refresh_token"
}'
```

### Validate Token

Validates a token.

```bash
curl -X POST http://localhost:9000/api/auth/validate \
-H "Content-Type: application/json" \
-d '{
    "token": "your_access_token"
}'
```
