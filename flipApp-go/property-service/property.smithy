$version: "2.0"

namespace flipapp.property

@documentation("Manages core data for physical properties")
service PropertyService {
    version: "1.0.0",
    operations: [
        CreateProperty,
        GetProperty,
        UpdateProperty,
        ListProperties
    ]
}

// --- Structures ---
structure Property {
    @required
    propertyId: String,

    @required
    address: Address,

    // Property specifications
    squareFootage: Integer,
    bedrooms: Float, // Using float to accommodate 0.5 for dens etc.
    bathrooms: Float,
    lotSize: Float,
    yearBuilt: Integer,
    propertyType: String, // e.g., Single Family, Multi-Family

    createdAt: Timestamp,
    updatedAt: Timestamp
}

structure Address {
    @required
    street: String,

    @required
    city: String,

    @required
    state: String,

    @required
    zipCode: String,
}

list PropertyList {
    member: Property
}

// --- Operations ---
@http(method: "POST", uri: "/properties")
operation CreateProperty {
    input: {
        @required
        address: Address,
        squareFootage: Integer,
        bedrooms: Float,
        bathrooms: Float,
        lotSize: Float,
        yearBuilt: Integer,
        propertyType: String,
    },
    output: {
        @required
        property: Property
    }
}

@http(method: "GET", uri: "/properties/{propertyId}")
operation GetProperty {
    input: {
        @required
        @httpLabel
        propertyId: String
    },
    output: {
        @required
        property: Property
    },
    errors: [PropertyNotFound]
}

@http(method: "PUT", uri: "/properties/{propertyId}")
operation UpdateProperty {
    input: {
        @required
        @httpLabel
        propertyId: String,

        @required
        address: Address,
        squareFootage: Integer,
        bedrooms: Float,
        bathrooms: Float,
        lotSize: Float,
        yearBuilt: Integer,
        propertyType: String,
    },
    output: {
        @required
        property: Property
    },
    errors: [PropertyNotFound]
}

@http(method: "GET", uri: "/properties")
operation ListProperties {
    input: {},
    output: {
        @required
        properties: PropertyList
    }
}

// --- Errors ---
@error("client")
@httpError(404)
structure PropertyNotFound {
    @required
    message: String
}
