$version: "2.0"

namespace com.lovablecline.chat

use aws.protocols#restJson1
use smithy.framework#ValidationException

/// Chat service for real-time messaging and communication
@restJson1
service ChatService {
    version: "2024-01-01",
    operations: [
        SendMessage,
        GetMessages,
        GetConversation,
        ListConversations,
        MarkAsRead,
        DeleteMessage
    ]
}

/// Chat message structure
structure ChatMessage {
    @required
    messageId: String,

    @required
    conversationId: String,

    @required
    senderId: String,

    @required
    content: String,

    messageType: MessageType = "TEXT",
    attachments: AttachmentList,
    timestamp: Timestamp,
    read: Boolean,
    edited: Boolean,
    editedAt: Timestamp
}

/// Conversation structure
structure Conversation {
    @required
    conversationId: String,

    @required
    type: ConversationType,

    @required
    participants: ParticipantList,

    lastMessage: ChatMessage,
    unreadCount: Integer,
    createdAt: Timestamp,
    updatedAt: Timestamp
}

/// Conversation participant
structure Participant {
    @required
    userId: String,

    @required
    username: String,

    joinedAt: Timestamp,
    lastReadAt: Timestamp
}

/// Message attachment
structure Attachment {
    @required
    attachmentId: String,

    @required
    filename: String,

    @required
    url: String,

    @required
    type: AttachmentType,

    size: Long
}

/// Send message request
structure SendMessageRequest {
    @required
    conversationId: String,

    @required
    content: String,

    messageType: MessageType = "TEXT",
    attachments: AttachmentList
}

/// Send message response
structure SendMessageResponse {
    @required
    message: ChatMessage
}

/// Get messages request with pagination
structure GetMessagesRequest {
    @required
    conversationId: String,

    before: Timestamp,
    after: Timestamp,
    limit: Integer = 50
}

/// Get messages response
structure GetMessagesResponse {
    @required
    messages: ChatMessageList,

    @required
    hasMore: Boolean
}

/// Get conversation request
structure GetConversationRequest {
    @required
    conversationId: String
}

/// Get conversation response
structure GetConversationResponse {
    @required
    conversation: Conversation
}

/// List conversations request
structure ListConversationsRequest {
    page: Integer = 1,
    limit: Integer = 20
}

/// List conversations response
structure ListConversationsResponse {
    @required
    conversations: ConversationList,

    @required
    totalCount: Integer,

    @required
    currentPage: Integer,

    @required
    totalPages: Integer
}

/// Mark as read request
structure MarkAsReadRequest {
    @required
    conversationId: String,

    @required
    messageId: String
}

/// Mark as read response
structure MarkAsReadResponse {
    @required
    message: String
}

/// Delete message request
structure DeleteMessageRequest {
    @required
    messageId: String
}

/// Delete message response
structure DeleteMessageResponse {
    @required
    message: String
}

/// List of chat messages
list ChatMessageList {
    member: ChatMessage
}

/// List of conversations
list ConversationList {
    member: Conversation
}

/// List of participants
list ParticipantList {
    member: Participant
}

/// List of attachments
list AttachmentList {
    member: Attachment
}

/// Message type enum
@enum([
    { value: "TEXT", name: "Text" },
    { value: "IMAGE", name: "Image" },
    { value: "FILE", name: "File" },
    { value: "SYSTEM", name: "System" }
])
string MessageType

/// Conversation type enum
@enum([
    { value: "DIRECT", name: "Direct Message" },
    { value: "GROUP", name: "Group Chat" },
    { value: "CHANNEL", name: "Channel" }
])
string ConversationType

/// Attachment type enum
@enum([
    { value: "IMAGE", name: "Image" },
    { value: "DOCUMENT", name: "Document" },
    { value: "VIDEO", name: "Video" },
    { value: "AUDIO", name: "Audio" },
    { value: "OTHER", name: "Other" }
])
string AttachmentType

@http(method: "POST", uri: "/api/chat/messages")
@documentation("Send a new chat message")
operation SendMessage {
    input: SendMessageRequest,
    output: SendMessageResponse,
    errors: [ValidationException, ConversationNotFoundError]
}

@http(method: "GET", uri: "/api/chat/conversations/{conversationId}/messages")
@documentation("Get messages from a conversation")
operation GetMessages {
    input: GetMessagesRequest,
    output: GetMessagesResponse,
    errors: [ValidationException, ConversationNotFoundError]
}

@http(method: "GET", uri: "/api/chat/conversations/{conversationId}")
@documentation("Get conversation details")
operation GetConversation {
    input: GetConversationRequest,
    output: GetConversationResponse,
    errors: [ValidationException, ConversationNotFoundError]
}

@http(method: "GET", uri: "/api/chat/conversations")
@documentation("List user's conversations")
operation ListConversations {
    input: ListConversationsRequest,
    output: ListConversationsResponse,
    errors: [ValidationException]
}

@http(method: "POST", uri: "/api/chat/messages/{messageId}/read")
@documentation("Mark message as read")
operation MarkAsRead {
    input: MarkAsReadRequest,
    output: MarkAsReadResponse,
    errors: [ValidationException, MessageNotFoundError]
}

@http(method: "DELETE", uri: "/api/chat/messages/{messageId}")
@documentation("Delete a message")
operation DeleteMessage {
    input: DeleteMessageRequest,
    output: DeleteMessageResponse,
    errors: [ValidationException, MessageNotFoundError]
}

/// Error when conversation is not found
@error("client")
@httpError(404)
structure ConversationNotFoundError {
    @required
    message: String
}

/// Error when message is not found
@error("client")
@httpError(404)
structure MessageNotFoundError {
    @required
    message: String
}
