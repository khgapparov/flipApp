$version: "2.0"

namespace flipapp.gallery

@documentation("Stores all media related to properties and projects")
service GalleryService {
    version: "1.0.0",
    operations: [
        UploadImage,
        GetImage,
        ListImagesForProperty,
        CreateAlbum,
        GetAlbum,
        AddImageToAlbum
    ]
}

// --- Enums ---
@enum([
    { value: "BEFORE", name: "Before" },
    { value: "AFTER", name: "After" },
    { value: "PROGRESS", name: "Progress" },
    { value: "MARKETING", name: "Marketing" },
    { value: "RECEIPT", name: "Receipt" },
    { value: "UNCATEGORIZED", name: "Uncategorized" }
])
string ImageCategory

// --- Structures ---
structure Image {
    @required
    imageId: String,

    @required
    propertyId: String, // Links to the property-service

    @required
    url: String,

    @required
    category: ImageCategory,

    description: String,
    createdAt: Timestamp
}

structure Album {
    @required
    albumId: String,

    @required
    propertyId: String,

    @required
    title: String,

    description: String,
    createdAt: Timestamp
}

list ImageList {
    member: Image
}

// --- Operations ---
@http(method: "POST", uri: "/images")
operation UploadImage {
    input: {
        @required
        propertyId: String,

        @required
        category: ImageCategory,

        @required
        filename: String, // e.g., "kitchen-before.jpg"

        description: String,

        // The image data would be sent as a multipart form upload,
        // which is handled by the server implementation.
    },
    output: {
        @required
        image: Image
    },
    errors: [PropertyNotFound]
}

@http(method: "GET", uri: "/images/{imageId}")
operation GetImage {
    input: {
        @required
        @httpLabel
        imageId: String
    },
    output: {
        @required
        image: Image
    },
    errors: [ImageNotFound]
}

@http(method: "GET", uri: "/properties/{propertyId}/images")
operation ListImagesForProperty {
    input: {
        @required
        @httpLabel
        propertyId: String
    },
    output: {
        @required
        images: ImageList
    },
    errors: [PropertyNotFound]
}

@http(method: "POST", uri: "/albums")
operation CreateAlbum {
    input: {
        @required
        propertyId: String,

        @required
        title: String,

        description: String
    },
    output: {
        @required
        album: Album
    },
    errors: [PropertyNotFound]
}

@http(method: "GET", uri: "/albums/{albumId}")
operation GetAlbum {
    input: {
        @required
        @httpLabel
        albumId: String
    },
    output: {
        @required
        album: Album,
        images: ImageList
    },
    errors: [AlbumNotFound]
}

@http(method: "POST", uri: "/albums/{albumId}/images")
operation AddImageToAlbum {
    input: {
        @required
        @httpLabel
        albumId: String,

        @required
        imageId: String
    },
    output: {},
    errors: [AlbumNotFound, ImageNotFound]
}

// --- Errors ---
@error("client")
@httpError(404)
structure PropertyNotFound {
    @required
    message: String
}

@error("client")
@httpError(404)
structure ImageNotFound {
    @required
    message: String
}

@error("client")
@httpError(404)
structure AlbumNotFound {
    @required
    message: String
}
