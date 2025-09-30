$version: "2"

namespace flipapp.storage

/// Storage service for managing file uploads and storage
service StorageService {
    version: "2024-01-01"
    resources: [File]
    operations: [
        UploadFile,
        GetFile,
        DeleteFile,
        GetFileURL,
        ListFiles,
        HealthCheck
    ]
}

/// File resource representing a stored file
resource File {
    identifiers: { fileId: FileId }
    read: GetFile
    delete: DeleteFile
    list: ListFiles
}

/// Unique identifier for a file
string FileId

/// File metadata
structure FileMetadata {
    @required
    fileId: FileId

    @required
    filename: String

    @required
    contentType: String

    @required
    size: Long

    @required
    createdAt: Timestamp

    @required
    url: String

    tags: TagMap

    metadata: Document
}

/// Map of tags for file categorization
map TagMap {
    key: String
    value: String
}

/// Operation to upload a file
@http(method: "POST", uri: "/files", code: 201)
operation UploadFile {
    input: UploadFileInput
    output: UploadFileOutput
    errors: [FileTooLarge, InvalidFileType, StorageError]
}

/// Input for uploading a file
structure UploadFileInput {
    @httpHeader("Content-Type")
    contentType: String

    @httpHeader("Content-Length")
    contentLength: Long

    @httpPayload
    fileContent: Blob

    @httpQuery("filename")
    @required
    filename: String

    @httpQuery("tags")
    tags: TagMap

    @httpQuery("metadata")
    metadata: Document
}

/// Output after successful file upload
structure UploadFileOutput {
    @required
    file: FileMetadata
}

/// Operation to get file content
@http(method: "GET", uri: "/files/{fileId}")
operation GetFile {
    input: GetFileInput
    output: GetFileOutput
    errors: [FileNotFound, StorageError]
}

/// Input for getting a file
structure GetFileInput {
    @required
    @httpLabel
    fileId: FileId
}

/// Output containing file content
structure GetFileOutput {
    @httpHeader("Content-Type")
    contentType: String

    @httpHeader("Content-Length")
    contentLength: Long

    @httpHeader("Content-Disposition")
    contentDisposition: String

    @httpPayload
    fileContent: Blob
}

/// Operation to delete a file
@http(method: "DELETE", uri: "/files/{fileId}")
operation DeleteFile {
    input: DeleteFileInput
    output: DeleteFileOutput
    errors: [FileNotFound, StorageError]
}

/// Input for deleting a file
structure DeleteFileInput {
    @required
    @httpLabel
    fileId: FileId
}

/// Output after successful file deletion
structure DeleteFileOutput {
    @required
    success: Boolean
}

/// Operation to get file URL
@http(method: "GET", uri: "/files/{fileId}/url")
operation GetFileURL {
    input: GetFileURLInput
    output: GetFileURLOutput
    errors: [FileNotFound, StorageError]
}

/// Input for getting file URL
structure GetFileURLInput {
    @required
    @httpLabel
    fileId: FileId
}

/// Output containing file URL
structure GetFileURLOutput {
    @required
    url: String
}

/// Operation to list files
@http(method: "GET", uri: "/files")
operation ListFiles {
    input: ListFilesInput
    output: ListFilesOutput
    errors: [StorageError]
}

/// Input for listing files
structure ListFilesInput {
    @httpQuery("limit")
    limit: Integer

    @httpQuery("offset")
    offset: Integer

    @httpQuery("tags")
    tags: TagMap
}

/// Output containing list of files
structure ListFilesOutput {
    @required
    files: FileMetadataList

    @required
    total: Integer
}

/// List of file metadata
list FileMetadataList {
    member: FileMetadata
}

/// Health check operation
@http(method: "GET", uri: "/health")
operation HealthCheck {
    output: HealthCheckOutput
}

/// Health check output
structure HealthCheckOutput {
    @required
    status: String
}

// Error definitions

/// File not found error
@error("client")
@httpError(404)
structure FileNotFound {
    @required
    message: String
}

/// File too large error
@error("client")
@httpError(413)
structure FileTooLarge {
    @required
    message: String
}

/// Invalid file type error
@error("client")
@httpError(415)
structure InvalidFileType {
    @required
    message: String
}

/// Storage error
@error("server")
@httpError(500)
structure StorageError {
    @required
    message: String
}
