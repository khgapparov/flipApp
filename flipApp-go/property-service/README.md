# Property Service API

This document provides `curl` commands for interacting with the Property Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing. All endpoints require a valid JWT token in the `Authorization` header.

## Endpoints

### Create a Property

Creates a new property.

```bash
curl -X POST http://localhost:9000/api/properties \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "address": {
        "street": "123 Main St",
        "city": "Anytown",
        "state": "CA",
        "zipCode": "12345"
    },
    "squareFootage": 1500,
    "bedrooms": 3,
    "bathrooms": 2,
    "lotSize": 0.25,
    "yearBuilt": 1990,
    "propertyType": "Single Family"
}'
```

### Get a Property

Gets a property by its ID.

```bash
curl -X GET http://localhost:9000/api/properties/property-123 \
-H "Authorization: Bearer <your_token>"
```

### Update a Property

Updates a property.

```bash
curl -X PUT http://localhost:9000/api/properties/property-123 \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "address": {
        "street": "123 Main St",
        "city": "Anytown",
        "state": "CA",
        "zipCode": "12345"
    },
    "squareFootage": 1600,
    "bedrooms": 3,
    "bathrooms": 2.5,
    "lotSize": 0.25,
    "yearBuilt": 1990,
    "propertyType": "Single Family"
}'
```

### List Properties

Lists all properties.

```bash
curl -X GET http://localhost:9000/api/properties \
-H "Authorization: Bearer <your_token>"
```
