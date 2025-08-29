$version: "2.0"

namespace com.lovablecline.auth

@documentation("Authentication service for user login, registration, and token management")
service AuthService {
    version: "2024-01-01",
    operations: [
        RegisterUser,
        LoginUser,
        RefreshToken,
        LogoutUser,
        ValidateToken
    ]
}

@documentation("Request to register a new user")
structure RegisterRequest {
    @required
    username: String,

    @required
    email: String,

    @required
    password: String,

    firstName: String,
    lastName: String
}

@documentation("User registration response")
structure RegisterResponse {
    @required
    userId: String,

    @required
    message: String
}

@documentation("Request to authenticate a user")
structure LoginRequest {
    @required
    identifier: String, // Can be username or email

    @required
    password: String
}

@documentation("User login response with JWT tokens")
structure LoginResponse {
    @required
    accessToken: String,

    @required
    refreshToken: String,

    @required
    expiresIn: Long,

    @required
    userId: String,

    @required
    username: String
}

@documentation("Token refresh request")
structure RefreshRequest {
    @required
    refreshToken: String
}

@documentation("Token refresh response")
structure RefreshResponse {
    @required
    accessToken: String,

    @required
    expiresIn: Long
}

@documentation("Token validation request")
structure ValidateRequest {
    @required
    token: String
}

@documentation("Token validation response")
structure ValidateResponse {
    @required
    isValid: Boolean,

    userId: String,
    username: String,
    expiresAt: Timestamp
}

@documentation("Logout request")
structure LogoutRequest {
    @required
    refreshToken: String
}

@documentation("Logout response")
structure LogoutResponse {
    @required
    message: String
}

@http(method: "POST", uri: "/api/auth/register")
@documentation("Register a new user account")
operation RegisterUser {
    input: RegisterRequest,
    output: RegisterResponse,
    errors: [UserAlreadyExistsError]
}

@http(method: "POST", uri: "/api/auth/login")
@documentation("Authenticate user and return JWT tokens")
operation LoginUser {
    input: LoginRequest,
    output: LoginResponse,
    errors: [InvalidCredentialsError]
}

@http(method: "POST", uri: "/api/auth/refresh")
@documentation("Refresh access token using refresh token")
operation RefreshToken {
    input: RefreshRequest,
    output: RefreshResponse,
    errors: [InvalidTokenError]
}

@http(method: "POST", uri: "/api/auth/logout")
@documentation("Invalidate refresh token")
operation LogoutUser {
    input: LogoutRequest,
    output: LogoutResponse
}

@http(method: "POST", uri: "/api/auth/validate")
@documentation("Validate JWT token")
operation ValidateToken {
    input: ValidateRequest,
    output: ValidateResponse
}

@documentation("Error when user already exists")
@error("client")
@httpError(409)
structure UserAlreadyExistsError {
    @required
    message: String
}

@documentation("Error when credentials are invalid")
@error("client")
@httpError(401)
structure InvalidCredentialsError {
    @required
    message: String
}

@documentation("Error when token is invalid or expired")
@error("client")
@httpError(401)
structure InvalidTokenError {
    @required
    message: String
}
