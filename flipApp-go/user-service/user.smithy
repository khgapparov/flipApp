$version: "2.0"

namespace flipapp.user

@documentation("Manages user and entity profiles")
service UserService {
    version: "1.0.0",
    operations: [
        CreateUserProfile,
        GetUserProfile,
        UpdateUserProfile,
        ListUsers
    ]
}

// --- Enums ---
@enum([
    { value: "PERSON", name: "Person" },
    { value: "LLC", name: "LLC" }
])
string ProfileType

// --- Structures ---
structure UserProfile {
    @required
    userId: String,

    @required
    profileType: ProfileType,

    @required
    email: String,

    // Fields for PERSON type
    firstName: String,
    lastName: String,

    // Fields for LLC type
    companyName: String,

    // Common fields
    avatarUrl: String,
    phoneNumber: String,
    createdAt: Timestamp,
    updatedAt: Timestamp
}

list UserProfileList {
    member: UserProfile
}

// --- Operations ---
@http(method: "POST", uri: "/users")
operation CreateUserProfile {
    input: {
        @required
        profileType: ProfileType,

        @required
        email: String,

        firstName: String,
        lastName: String,
        companyName: String,
        avatarUrl: String,
        phoneNumber: String
    },
    output: {
        @required
        profile: UserProfile
    },
    errors: [EmailAlreadyExists]
}

@http(method: "GET", uri: "/users/{userId}")
operation GetUserProfile {
    input: {
        @required
        @httpLabel
        userId: String
    },
    output: {
        @required
        profile: UserProfile
    },
    errors: [UserNotFound]
}

@http(method: "PUT", uri: "/users/{userId}")
operation UpdateUserProfile {
    input: {
        @required
        @httpLabel
        userId: String,

        firstName: String,
        lastName: String,
        companyName: String,
        avatarUrl: String,
        phoneNumber: String
    },
    output: {
        @required
        profile: UserProfile
    },
    errors: [UserNotFound]
}

@http(method: "GET", uri: "/users")
operation ListUsers {
    input: {},
    output: {
        @required
        users: UserProfileList
    }
}

// --- Errors ---
@error("client")
@httpError(404)
structure UserNotFound {
    @required
    message: String
}

@error("client")
@httpError(409)
structure EmailAlreadyExists {
    @required
    message: String
}
