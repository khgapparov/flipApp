$version: "2.0"

namespace flipapp.chat

@documentation("Facilitates communication about a specific project")
service ChatService {
    version: "1.0.0",
    operations: [
        StartConversation,
        PostMessage,
        GetConversationMessages
    ]
}

// --- Structures ---
structure Conversation {
    @required
    conversationId: String,

    @required
    projectId: String, // Links to the project-service

    // A list of userIds from the user-service
    @required
    participants: StringList,

    createdAt: Timestamp
}

structure Message {
    @required
    messageId: String,

    @required
    conversationId: String,

    @required
    senderId: String, // The userId of the sender

    @required
    content: String,

    @required
    timestamp: Timestamp
}

list StringList {
    member: String
}

list MessageList {
    member: Message
}

// --- Operations ---
@http(method: "POST", uri: "/conversations")
operation StartConversation {
    input: {
        @required
        projectId: String,

        @required
        participantIds: StringList
    },
    output: {
        @required
        conversation: Conversation
    },
    errors: [ProjectNotFound]
}

@http(method: "POST", uri: "/conversations/{conversationId}/messages")
operation PostMessage {
    input: {
        @required
        @httpLabel
        conversationId: String,

        @required
        senderId: String,

        @required
        content: String
    },
    output: {
        @required
        message: Message
    },
    errors: [ConversationNotFound, InvalidParticipant]
}

@http(method: "GET", uri: "/conversations/{conversationId}/messages")
operation GetConversationMessages {
    input: {
        @required
        @httpLabel
        conversationId: String
    },
    output: {
        @required
        messages: MessageList
    },
    errors: [ConversationNotFound]
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
structure ConversationNotFound {
    @required
    message: String
}

@error("client")
@httpError(403)
structure InvalidParticipant {
    @documentation("Fired when the sender is not a participant in the conversation")
    @required
    message: String
}
