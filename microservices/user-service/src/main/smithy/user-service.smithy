$version: "2.0"

namespace com.lovablecline.users

use aws.protocols#restJson1
use smithy.framework#ValidationException

/// User management service for user profiles and administration
@restJson1
service UserService {
    version: "2024-01-01",
    operations: [
        GetUserProfile,
        UpdateUserProfile,
        DeleteUser,
        ListUsers,
        SearchUsers
    ]
}

/// User profile structure
structure UserProfile {
    @required
    userId: String,

    @required
    username: String,

    @required
    email: String,

    firstName: String,
    lastName: String,
    avatarUrl: String,
    bio: String,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    lastLoginAt: Timestamp
}

/// Get user profile request
structure GetUserRequest {
    @required
    userId: String
}

/// Get user profile response
structure GetUserResponse {
    @required
    profile: UserProfile
}

/// Update user profile request
structure UpdateUserRequest {
    @required
    userId: String,

    firstName: String,
    lastName: String,
    avatarUrl: String,
    bio: String
}

/// Update user profile response
structure UpdateUserResponse {
    @required
    profile: UserProfile
}

/// Delete user request
structure DeleteUserRequest {
    @required
    userId: String
}

/// Delete user response
structure DeleteUserResponse {
    @required
    message: String
}

/// List users request with pagination
structure ListUsersRequest {
    page: Integer = 1,
    limit: Integer = 20
}

/// List users response with pagination metadata
structure ListUsersResponse {
    @required
    users: UserProfileList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// Search users request
structure SearchUsersRequest {
    @required
    query: String,

    page: Integer = 1,
    limit: Integer = 20
}

/// Search users response
structure SearchUsersResponse {
    @required
    users: UserProfileList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// List of user profiles
list UserProfileList {
    member: UserProfile
}

@http(method: "GET", uri: "/api/users/{userId}")
@documentation("Get user profile by ID")
operation GetUserProfile {
    input: GetUserRequest,
    output: GetUserResponse,
    errors: [ValidationException, UserNotFoundError]
}

@http(method: "PUT", uri: "/api/users/{userId}")
@documentation("Update user profile")
operation UpdateUserProfile {
    input: UpdateUserRequest,
    output: UpdateUserResponse,
    errors: [ValidationException, UserNotFoundError]
}

@http(method: "DELETE", uri: "/api/users/{userId}")
@documentation("Delete user account")
operation DeleteUser {
    input: DeleteUserRequest,
    output: DeleteUserResponse,
    errors: [ValidationException, UserNotFoundError]
}

@http(method: "GET", uri: "/api/users")
@documentation("List users with pagination")
operation ListUsers {
    input: ListUsersRequest,
    output: ListUsersResponse,
    errors: [ValidationException]
}

@http(method: "GET", uri: "/api/users/search")
@documentation("Search users by query")
operation SearchUsers {
    input: SearchUsersRequest,
    output: SearchUsersResponse,
    errors: [ValidationException]
}

/// Error when user is not found
@error("client")
@httpError(404)
structure UserNotFoundError {
    @required
    message: String
}
