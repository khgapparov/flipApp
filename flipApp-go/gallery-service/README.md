# Gallery Service API

This document provides `curl` commands for interacting with the Gallery Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing. All endpoints require a valid JWT token in the `Authorization` header.

## Endpoints

### Upload an Image

Uploads an image. This is a multipart form upload.

```bash
curl -X POST http://localhost:9000/api/gallery/images \
-H "Authorization: Bearer <your_token>" \
-F "propertyId=property-123" \
-F "category=BEFORE" \
-F "filename=kitchen-before.jpg" \
-F "description=The kitchen before renovation" \
-F "image=@/path/to/your/image.jpg"
```

### Get an Image

Gets an image by its ID.

```bash
curl -X GET http://localhost:9000/api/gallery/images/image-123 \
-H "Authorization: Bearer <your_token>"
```

### List Images for a Property

Lists all images for a given property.

```bash
curl -X GET http://localhost:9000/api/gallery/properties/property-123/images \
-H "Authorization: Bearer <your_token>"
```

### Create an Album

Creates an album.

```bash
curl -X POST http://localhost:9000/api/gallery/albums \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "propertyId": "property-123",
    "title": "Kitchen Renovation",
    "description": "Photos of the kitchen renovation project"
}'
```

### Get an Album

Gets an album by its ID.

```bash
curl -X GET http://localhost:9000/api/gallery/albums/album-123 \
-H "Authorization: Bearer <your_token>"
```

### Add an Image to an Album

Adds an image to an album.

```bash
curl -X POST http://localhost:9000/api/gallery/albums/album-123/images \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "imageId": "image-123"
}'
```