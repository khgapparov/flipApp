# Storage Service

A microservice for managing file storage operations in the FlipApp ecosystem. This service provides a flexible, extensible storage layer that can support multiple storage backends.

## Features

- **File Operations**: Upload, download, delete, and list files
- **Metadata Management**: Store file metadata including tags and custom metadata
- **Extensible Architecture**: Support for multiple storage backends (file system, S3, database, etc.)
- **Tag-based Filtering**: Filter files by tags for better organization
- **URL Generation**: Generate URLs for accessing stored files
- **Health Checks**: Built-in health monitoring
- **Service Discovery**: Automatic registration with Consul

## API Endpoints

### Upload File
- **POST** `/api/storage/files`
- **Description**: Upload a new file
- **Parameters**: 
  - `file` (multipart/form-data): The file to upload
  - `filename` (optional): Custom filename
  - `tags` (optional): Comma-separated key=value pairs for tagging
  - `metadata` (optional): Comma-separated key=value pairs for custom metadata

### Get File
- **GET** `/api/storage/files/{fileId}`
- **Description**: Download a file by its ID
- **Response**: File content with appropriate headers

### Delete File
- **DELETE** `/api/storage/files/{fileId}`
- **Description**: Delete a file by its ID

### Get File URL
- **GET** `/api/storage/files/{fileId}/url`
- **Description**: Get the URL for accessing a file

### List Files
- **GET** `/api/storage/files`
- **Description**: List files with optional filtering and pagination
- **Query Parameters**:
  - `limit`: Number of files to return (default: 10)
  - `offset`: Pagination offset (default: 0)
  - `tags`: Filter by tags (comma-separated key=value pairs)

### Health Check
- **GET** `/api/storage/health`
- **Description**: Service health status

## Architecture

The storage service follows a layered architecture with clear separation of concerns:

### Storage Interface
```go
type Storage interface {
    Save(fileID string, filename string, contentType string, data []byte, tags map[string]string, metadata map[string]interface{}) (*FileMetadata, error)
    Get(fileID string) ([]byte, *FileMetadata, error)
    Delete(fileID string) error
    GetURL(fileID string) (string, error)
    List(limit, offset int, tags map[string]string) ([]FileMetadata, int, error)
}
```

### Current Implementations

1. **FileStorage**: Local file system storage
   - Stores files in a configurable directory
   - Maintains metadata in memory (can be extended to use database)
   - Suitable for development and small-scale deployments

### Extensibility

The service is designed to be easily extended with additional storage backends:

- **S3Storage**: Amazon S3 integration
- **DatabaseStorage**: Store files in database as BLOBs
- **AzureStorage**: Azure Blob Storage integration
- **GoogleStorage**: Google Cloud Storage integration

## Configuration

### Environment Variables

- `CONSUL_ADDRESS`: Consul service discovery address (default: consul:8500)
- `SERVICE_ADDRESS`: Service address for Consul registration (default: storage-service)
- `STORAGE_DIR`: Local storage directory (default: ./storage)

### Port

- **Service Port**: 8085
- **API Gateway Path**: `/api/storage/*`

## Development

### Running Tests

```bash
cd storage-service
go test -v
```

### Building

```bash
cd storage-service
go build
```

### Docker

```bash
# Build the image
docker build -t storage-service .

# Run with Docker Compose
docker-compose up storage-service
```

## Usage Examples

### Upload a File

```bash
curl -X POST \
  http://localhost:9000/api/storage/files \
  -H 'Authorization: Bearer <jwt-token>' \
  -F 'file=@/path/to/file.jpg' \
  -F 'filename=custom-name.jpg' \
  -F 'tags=category=images,type=jpeg' \
  -F 'metadata=author=user,project=flipapp'
```

### Get a File

```bash
curl -X GET \
  http://localhost:9000/api/storage/files/{fileId} \
  -H 'Authorization: Bearer <jwt-token>' \
  --output downloaded-file.jpg
```

### List Files

```bash
curl -X GET \
  'http://localhost:9000/api/storage/files?limit=10&offset=0&tags=category=images' \
  -H 'Authorization: Bearer <jwt-token>'
```

## Integration with Other Services

The storage service can be used by other services in the FlipApp ecosystem:

- **Gallery Service**: Store property images and progress photos
- **Project Service**: Store project documents and receipts
- **User Service**: Store user profile pictures
- **Chat Service**: Store chat attachments

## Future Enhancements

- [ ] Database persistence for metadata
- [ ] S3 storage backend implementation
- [ ] File versioning support
- [ ] Access control and permissions
- [ ] File compression and optimization
- [ ] CDN integration for file distribution
- [ ] File encryption at rest
- [ ] Backup and recovery mechanisms
