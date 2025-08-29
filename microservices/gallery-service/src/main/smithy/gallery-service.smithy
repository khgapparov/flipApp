$version: "2.0"

namespace com.lovablecline.gallery

use aws.protocols#restJson1
use smithy.framework#ValidationException

/// Gallery service for managing images and media assets
@restJson1
service GalleryService {
    version: "2024-01-01",
    operations: [
        UploadImage,
        GetImage,
        ListImages,
        UpdateImage,
        DeleteImage,
        CreateAlbum,
        GetAlbum,
        ListAlbums,
        UpdateAlbum,
        DeleteAlbum,
        AddToAlbum,
        RemoveFromAlbum
    ]
}

/// Image structure
structure Image {
    @required
    imageId: String,

    @required
    filename: String,

    @required
    url: String,

    @required
    thumbnailUrl: String,

    @required
    ownerId: String,

    title: String,
    description: String,
    tags: StringList,
    width: Integer,
    height: Integer,
    size: Long,
    format: ImageFormat,
    metadata: MetadataMap,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    albumIds: StringList
}

/// Album structure
structure Album {
    @required
    albumId: String,

    @required
    title: String,

    @required
    ownerId: String,

    description: String,
    coverImageId: String,
    images: ImageList,
    tags: StringList,
    isPublic: Boolean = false,
    createdAt: Timestamp,
    updatedAt: Timestamp
}

/// Upload image request
structure UploadImageRequest {
    @required
    filename: String,

    @required
    data: Blob,

    title: String,
    description: String,
    tags: StringList,
    albumIds: StringList
}

/// Upload image response
structure UploadImageResponse {
    @required
    image: Image
}

/// Get image request
structure GetImageRequest {
    @required
    imageId: String
}

/// Get image response
structure GetImageResponse {
    @required
    image: Image
}

/// List images request with filtering
structure ListImagesRequest {
    ownerId: String,
    albumId: String,
    tags: StringList,
    format: ImageFormat,
    page: Integer = 1,
    limit: Integer = 20
}

/// List images response
structure ListImagesResponse {
    @required
    images: ImageList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// Update image request
structure UpdateImageRequest {
    @required
    imageId: String,

    title: String,
    description: String,
    tags: StringList,
    albumIds: StringList
}

/// Update image response
structure UpdateImageResponse {
    @required
    image: Image
}

/// Delete image request
structure DeleteImageRequest {
    @required
    imageId: String
}

/// Delete image response
structure DeleteImageResponse {
    @required
    message: String
}

/// Create album request
structure CreateAlbumRequest {
    @required
    title: String,

    description: String,
    tags: StringList,
    isPublic: Boolean = false,
    coverImageId: String
}

/// Create album response
structure CreateAlbumResponse {
    @required
    album: Album
}

/// Get album request
structure GetAlbumRequest {
    @required
    albumId: String
}

/// Get album response
structure GetAlbumResponse {
    @required
    album: Album
}

/// List albums request
structure ListAlbumsRequest {
    ownerId: String,
    isPublic: Boolean,
    tags: StringList,
    page: Integer = 1,
    limit: Integer = 20
}

/// List albums response
structure ListAlbumsResponse {
    @required
    albums: AlbumList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// Update album request
structure UpdateAlbumRequest {
    @required
    albumId: String,

    title: String,
    description: String,
    tags: StringList,
    isPublic: Boolean,
    coverImageId: String
}

/// Update album response
structure UpdateAlbumResponse {
    @required
    album: Album
}

/// Delete album request
structure DeleteAlbumRequest {
    @required
    albumId: String
}

/// Delete album response
structure DeleteAlbumResponse {
    @required
    message: String
}

/// Add to album request
structure AddToAlbumRequest {
    @required
    albumId: String,

    @required
    imageId: String
}

/// Add to album response
structure AddToAlbumResponse {
    @required
    message: String
}

/// Remove from album request
structure RemoveFromAlbumRequest {
    @required
    albumId: String,

    @required
    imageId: String
}

/// Remove from album response
structure RemoveFromAlbumResponse {
    @required
    message: String
}

/// List of images
list ImageList {
    member: Image
}

/// List of albums
list AlbumList {
    member: Album
}

/// List of strings
list StringList {
    member: String
}

/// Map of metadata
map MetadataMap {
    key: String,
    value: String
}

/// Image format enum
@enum([
    { value: "JPEG", name: "JPEG" },
    { value: "PNG", name: "PNG" },
    { value: "GIF", name: "GIF" },
    { value: "WEBP", name: "WebP" },
    { value: "SVG", name: "SVG" },
    { value: "BMP", name: "BMP" }
])
string ImageFormat

@http(method: "POST", uri: "/api/gallery/images")
@documentation("Upload a new image")
operation UploadImage {
    input: UploadImageRequest,
    output: UploadImageResponse,
    errors: [ValidationException]
}

@http(method: "GET", uri: "/api/gallery/images/{imageId}")
@documentation("Get image by ID")
operation GetImage {
    input: GetImageRequest,
    output: GetImageResponse,
    errors: [ValidationException, ImageNotFoundError]
}

@http(method: "GET", uri: "/api/gallery/images")
@documentation("List images with filtering")
operation ListImages {
    input: ListImagesRequest,
    output: ListImagesResponse,
    errors: [ValidationException]
}

@http(method: "PUT", uri: "/api/gallery/images/{imageId}")
@documentation("Update image metadata")
operation UpdateImage {
    input: UpdateImageRequest,
    output: UpdateImageResponse,
    errors: [ValidationException, ImageNotFoundError]
}

@http(method: "DELETE", uri: "/api/gallery/images/{imageId}")
@documentation("Delete image")
operation DeleteImage {
    input: DeleteImageRequest,
    output: DeleteImageResponse,
    errors: [ValidationException, ImageNotFoundError]
}

@http(method: "POST", uri: "/api/gallery/albums")
@documentation("Create a new album")
operation CreateAlbum {
    input: CreateAlbumRequest,
    output: CreateAlbumResponse,
    errors: [ValidationException]
}

@http(method: "GET", uri: "/api/gallery/albums/{albumId}")
@documentation("Get album by ID")
operation GetAlbum {
    input: GetAlbumRequest,
    output: GetAlbumResponse,
    errors: [ValidationException, AlbumNotFoundError]
}

@http(method: "GET", uri: "/api/gallery/albums")
@documentation("List albums with filtering")
operation ListAlbums {
    input: ListAlbumsRequest,
    output: ListAlbumsResponse,
    errors: [ValidationException]
}

@http(method: "PUT", uri: "/api/gallery/albums/{albumId}")
@documentation("Update album")
operation UpdateAlbum {
    input: UpdateAlbumRequest,
    output: UpdateAlbumResponse,
    errors: [ValidationException, AlbumNotFoundError]
}

@http(method: "DELETE", uri: "/api/gallery/albums/{albumId}")
@documentation("Delete album")
operation DeleteAlbum {
    input: DeleteAlbumRequest,
    output: DeleteAlbumResponse,
    errors: [ValidationException, AlbumNotFoundError]
}

@http(method: "POST", uri: "/api/gallery/albums/{albumId}/images/{imageId}")
@documentation("Add image to album")
operation AddToAlbum {
    input: AddToAlbumRequest,
    output: AddToAlbumResponse,
    errors: [ValidationException, AlbumNotFoundError, ImageNotFoundError]
}

@http(method: "DELETE", uri: "/api/gallery/albums/{albumId}/images/{imageId}")
@documentation("Remove image from album")
operation RemoveFromAlbum {
    input: RemoveFromAlbumRequest,
    output: RemoveFromAlbumResponse,
    errors: [ValidationException, AlbumNotFoundError, ImageNotFoundError]
}

/// Error when image is not found
@error("client")
@httpError(404)
structure ImageNotFoundError {
    @required
    message: String
}

/// Error when album is not found
@error("client")
@httpError(404)
structure AlbumNotFoundError {
    @required
    message: String
}
