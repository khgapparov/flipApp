$version: "2.0"

namespace flipapp.project

@documentation("Manages the lifecycle and operations of a property flip")
service ProjectService {
    version: "1.0.0",
    operations: [
        CreateProject,
        GetProject,
        UpdateProjectStatus,
        ListProjects,
        AddProjectMember,
        ListProjectMembers
    ]
}

// --- Enums ---
@enum([
    { value: "LEAD", name: "Lead" },
    { value: "OFFER_SENT", name: "Offer Sent" },
    { value: "OFFER_ACCEPTED", name: "Offer Accepted" },
    { value: "ACTIVE", name: "Active" }, // Repair/flipping started
    { value: "ON_MARKET", name: "On Market" },
    { value: "SOLD", name: "Sold" },
    { value: "ARCHIVED", name: "Archived" }
])
string ProjectStatus

@enum([
    { value: "OWNER", name: "Owner" },
    { value: "CONSTRUCTOR", name: "Constructor" },
    { value: "AGENT", name: "Agent" },
    { value: "PROJECT_MANAGER", name: "Project Manager" }
])
string MemberRole

// --- Structures ---
structure Project {
    @required
    projectId: String,

    @required
    propertyId: String, // Links to the property-service

    @required
    status: ProjectStatus,

    projectName: String,
    budget: Float,
    startDate: Timestamp,
    endDate: Timestamp,
    createdAt: Timestamp,
    updatedAt: Timestamp
}

structure ProjectMember {
    @required
    projectId: String,

    @required
    userId: String, // Links to the user-service

    @required
    role: MemberRole
}

list ProjectList {
    member: Project
}

list ProjectMemberList {
    member: ProjectMember
}

// --- Operations ---
@http(method: "POST", uri: "/projects")
operation CreateProject {
    input: {
        @required
        propertyId: String,
        projectName: String,
        budget: Float
    },
    output: {
        @required
        project: Project
    }
}

@http(method: "GET", uri: "/projects/{projectId}")
operation GetProject {
    input: {
        @required
        @httpLabel
        projectId: String
    },
    output: {
        @required
        project: Project
    },
    errors: [ProjectNotFound]
}

@http(method: "PUT", uri: "/projects/{projectId}/status")
operation UpdateProjectStatus {
    input: {
        @required
        @httpLabel
        projectId: String,

        @required
        status: ProjectStatus
    },
    output: {
        @required
        project: Project
    },
    errors: [ProjectNotFound]
}

@http(method: "GET", uri: "/projects")
operation ListProjects {
    input: {},
    output: {
        @required
        projects: ProjectList
    }
}

@http(method: "POST", uri: "/projects/{projectId}/members")
operation AddProjectMember {
    input: {
        @required
        @httpLabel
        projectId: String,

        @required
        userId: String,

        @required
        role: MemberRole
    },
    output: {
        @required
        member: ProjectMember
    },
    errors: [ProjectNotFound, UserNotFound]
}

@http(method: "GET", uri: "/projects/{projectId}/members")
operation ListProjectMembers {
    input: {
        @required
        @httpLabel
        projectId: String
    },
    output: {
        @required
        members: ProjectMemberList
    },
    errors: [ProjectNotFound]
}

// --- Errors ---
@error("client")
@httpError(404)
structure ProjectNotFound {
    @required
    message: String
}

@error("client")
@httpError(404)
structure UserNotFound {
    @documentation("Fired when the userId does not exist in the user-service")
    @required
    message: String
}
