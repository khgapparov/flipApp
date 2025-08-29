$version: "2.0"

namespace com.lovablecline.projects

use aws.protocols#restJson1
use smithy.framework#ValidationException

/// Project management service for creating and managing projects
@restJson1
service ProjectService {
    version: "2024-01-01",
    operations: [
        CreateProject,
        GetProject,
        UpdateProject,
        DeleteProject,
        ListProjects,
        ListUserProjects,
        AddProjectMember,
        RemoveProjectMember
    ]
}

/// Project structure
structure Project {
    @required
    projectId: String,

    @required
    title: String,

    description: String,
    status: ProjectStatus,
    priority: ProjectPriority,
    ownerId: String,
    members: MemberList,
    tags: StringList,
    startDate: Timestamp,
    dueDate: Timestamp,
    createdAt: Timestamp,
    updatedAt: Timestamp,
    progress: Integer
}

/// Project member structure
structure ProjectMember {
    @required
    userId: String,

    @required
    username: String,

    @required
    role: MemberRole,

    joinedAt: Timestamp
}

/// Create project request
structure CreateProjectRequest {
    @required
    title: String,

    description: String,
    status: ProjectStatus = "PLANNING",
    priority: ProjectPriority = "MEDIUM",
    tags: StringList,
    startDate: Timestamp,
    dueDate: Timestamp
}

/// Create project response
structure CreateProjectResponse {
    @required
    project: Project
}

/// Get project request
structure GetProjectRequest {
    @required
    projectId: String
}

/// Get project response
structure GetProjectResponse {
    @required
    project: Project
}

/// Update project request
structure UpdateProjectRequest {
    @required
    projectId: String,

    title: String,
    description: String,
    status: ProjectStatus,
    priority: ProjectPriority,
    tags: StringList,
    startDate: Timestamp,
    dueDate: Timestamp,
    progress: Integer
}

/// Update project response
structure UpdateProjectResponse {
    @required
    project: Project
}

/// Delete project request
structure DeleteProjectRequest {
    @required
    projectId: String
}

/// Delete project response
structure DeleteProjectResponse {
    @required
    message: String
}

/// List projects request with pagination
structure ListProjectsRequest {
    status: ProjectStatus,
    priority: ProjectPriority,
    page: Integer = 1,
    limit: Integer = 20
}

/// List projects response
structure ListProjectsResponse {
    @required
    projects: ProjectList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// List user projects request
structure ListUserProjectsRequest {
    @required
    userId: String,

    status: ProjectStatus,
    page: Integer = 1,
    limit: Integer = 20
}

/// List user projects response
structure ListUserProjectsResponse {
    @required
    projects: ProjectList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// Add project member request
structure AddProjectMemberRequest {
    @required
    projectId: String,

    @required
    userId: String,

    @required
    role: MemberRole
}

/// Add project member response
structure AddProjectMemberResponse {
    @required
    message: String
}

/// Remove project member request
structure RemoveProjectMemberRequest {
    @required
    projectId: String,

    @required
    userId: String
}

/// Remove project member response
structure RemoveProjectMemberResponse {
    @required
    message: String
}

/// List of projects
list ProjectList {
    member: Project
}

/// List of project members
list MemberList {
    member: ProjectMember
}

/// List of strings
list StringList {
    member: String
}

/// Project status enum
@enum([
    { value: "PLANNING", name: "Planning" },
    { value: "IN_PROGRESS", name: "In Progress" },
    { value: "COMPLETED", name: "Completed" },
    { value: "ON_HOLD", name: "On Hold" },
    { value: "CANCELLED", name: "Cancelled" }
])
string ProjectStatus

/// Project priority enum
@enum([
    { value: "LOW", name: "Low" },
    { value: "MEDIUM", name: "Medium" },
    { value: "HIGH", name: "High" },
    { value: "URGENT", name: "Urgent" }
])
string ProjectPriority

/// Member role enum
@enum([
    { value: "OWNER", name: "Owner" },
    { value: "ADMIN", name: "Admin" },
    { value: "MEMBER", name: "Member" },
    { value: "VIEWER", name: "Viewer" }
])
string MemberRole

@http(method: "POST", uri: "/api/projects")
@documentation("Create a new project")
operation CreateProject {
    input: CreateProjectRequest,
    output: CreateProjectResponse,
    errors: [ValidationException]
}

@http(method: "GET", uri: "/api/projects/{projectId}")
@documentation("Get project by ID")
operation GetProject {
    input: GetProjectRequest,
    output: GetProjectResponse,
    errors: [ValidationException, ProjectNotFoundError]
}

@http(method: "PUT", uri: "/api/projects/{projectId}")
@documentation("Update project")
operation UpdateProject {
    input: UpdateProjectRequest,
    output: UpdateProjectResponse,
    errors: [ValidationException, ProjectNotFoundError]
}

@http(method: "DELETE", uri: "/api/projects/{projectId}")
@documentation("Delete project")
operation DeleteProject {
    input: DeleteProjectRequest,
    output: DeleteProjectResponse,
    errors: [ValidationException, ProjectNotFoundError]
}

@http(method: "GET", uri: "/api/projects")
@documentation("List all projects with filtering")
operation ListProjects {
    input: ListProjectsRequest,
    output: ListProjectsResponse,
    errors: [ValidationException]
}

@http(method: "GET", uri: "/api/users/{userId}/projects")
@documentation("List projects for a specific user")
operation ListUserProjects {
    input: ListUserProjectsRequest,
    output: ListUserProjectsResponse,
    errors: [ValidationException, UserNotFoundError]
}

@http(method: "POST", uri: "/api/projects/{projectId}/members")
@documentation("Add member to project")
operation AddProjectMember {
    input: AddProjectMemberRequest,
    output: AddProjectMemberResponse,
    errors: [ValidationException, ProjectNotFoundError, UserNotFoundError]
}

@http(method: "DELETE", uri: "/api/projects/{projectId}/members/{userId}")
@documentation("Remove member from project")
operation RemoveProjectMember {
    input: RemoveProjectMemberRequest,
    output: RemoveProjectMemberResponse,
    errors: [ValidationException, ProjectNotFoundError, UserNotFoundError]
}

/// Error when project is not found
@error("client")
@httpError(404)
structure ProjectNotFoundError {
    @required
    message: String
}

/// Error when user is not found
@error("client")
@httpError(404)
structure UserNotFoundError {
    @required
    message: String
}
