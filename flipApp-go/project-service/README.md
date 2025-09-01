# Project Service API

This document provides `curl` commands for interacting with the Project Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing. All endpoints require a valid JWT token in the `Authorization` header.

## Endpoints

### Create a Project

Creates a new project.

```bash
curl -X POST http://localhost:9000/api/projects \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d 
{
    "propertyId": "property-123",
    "projectName": "My First Flip",
    "budget": 50000.00
}
```

### Get a Project

Gets a project by its ID.

```bash
curl -X GET http://localhost:9000/api/projects/project-123 \
-H "Authorization: Bearer <your_token>"
```

### Update Project Status

Updates the status of a project.

```bash
curl -X PUT http://localhost:9000/api/projects/project-123/status \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d 
{
    "status": "ACTIVE"
}
```

### List Projects

Lists all projects.

```bash
curl -X GET http://localhost:9000/api/projects \
-H "Authorization: Bearer <your_token>"
```

### Add a Project Member

Adds a member to a project.

```bash
curl -X POST http://localhost:9000/api/projects/project-123/members \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d 
{
    "userId": "user-123",
    "role": "CONSTRUCTOR"
}
```

### List Project Members

Lists all members of a project.

```bash
curl -X GET http://localhost:9000/api/projects/project-123/members \
-H "Authorization: Bearer <your_token>"
```
