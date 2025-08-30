$version: "2.0"

namespace flipapp.auth

@documentation("Authentication service for user login, registration, and token management")
service AuthService {
    version: "1.0.0",
    operations: [
        Register,
        Login,
        RefreshToken,
        Logout,
        ValidateToken
    ]
}

// --- Structures ---
structure RegisterInput {
    @required
    username: String,

    @required
    email: String,

    @required
    password: String,
}

structure RegisterOutput {
    @required
    userId: String,
}

structure LoginInput {
    @required
    email: String,

    @required
    password: String
}

structure LoginOutput {
    @required
    accessToken: String,

    @required
    refreshToken: String,
}

structure RefreshTokenInput {
    @required
    refreshToken: String
}

structure RefreshTokenOutput {
    @required
    accessToken: String,
}

structure LogoutInput {
    @required
    refreshToken: String
}

structure LogoutOutput {
    @required
    message: String
}

structure ValidateTokenInput {
    @required
    token: String
}

structure ValidateTokenOutput {
    @required
    isValid: Boolean,

    @required
    userId: String
}


// --- Operations ---
@http(method: "POST", uri: "/auth/register")
operation Register {
    input: RegisterInput,
    output: RegisterOutput,
    errors: [UserAlreadyExists]
}

@http(method: "POST", uri: "/auth/login")
operation Login {
    input: LoginInput,
    output: LoginOutput,
    errors: [InvalidCredentials]
}

@http(method: "POST", uri: "/auth/refresh")
operation RefreshToken {
    input: RefreshTokenInput,
    output: RefreshTokenOutput,
    errors: [InvalidToken]
}

@http(method: "POST", uri: "/auth/logout")
operation Logout {
    input: LogoutInput,
    output: LogoutOutput
}

@http(method: "POST", uri: "/auth/validate")
operation ValidateToken {
    input: ValidateTokenInput,
    output: ValidateTokenOutput
}


// --- Errors ---
@error("client")
@httpError(409)
structure UserAlreadyExists {
    @required
    message: String
}

@error("client")
@httpError(401)
structure InvalidCredentials {
    @required
    message: String
}

@error("client")
@httpError(401)
structure InvalidToken {
    @required
    message: String
}
