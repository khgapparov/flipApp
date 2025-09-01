# User Service API

This document provides `curl` commands for interacting with the User Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing. All endpoints require a valid JWT token in the `Authorization` header.

## Endpoints

### Create a User Profile

Creates a new user profile.

#### For a Person

```bash
curl -X POST http://localhost:9000/api/users \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "profileType": "PERSON",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "123-456-7890"
}'
```

#### For an LLC

```bash
curl -X POST http://localhost:9000/api/users \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "profileType": "LLC",
    "email": "contact@company.com",
    "companyName": "My Flipping Company, LLC",
    "phoneNumber": "098-765-4321"
}'
```

### Get a User Profile

Gets a user profile by its ID.

```bash
curl -X GET http://localhost:9000/api/users/user-123 \
-H "Authorization: Bearer <your_token>"
```

### Update a User Profile

Updates a user profile.

```bash
curl -X PUT http://localhost:9000/api/users/user-123 \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "111-222-3333"
}'
```

### List Users

Lists all users.

```bash
curl -X GET http://localhost:9000/api/users \
-H "Authorization: Bearer <your_token>"
```
