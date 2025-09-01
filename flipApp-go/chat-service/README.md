# Chat Service API

This document provides `curl` commands for interacting with the Chat Service API via the API Gateway.

**Note:** The services are not fully implemented and the `curl` commands are for testing the API gateway and the service routing. All endpoints require a valid JWT token in the `Authorization` header.

## Endpoints

### Start a Conversation

Starts a new conversation.

```bash
curl -X POST http://localhost:9000/api/chat/conversations \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "projectId": "project-123",
    "participantIds": ["user-1", "user-2"]
}'
```

### Post a Message

Posts a message to a conversation.

```bash
curl -X POST http://localhost:9000/api/chat/conversations/conversation-123/messages \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <your_token>" \
-d '{
    "senderId": "user-1",
    "content": "Hello, world!"
}'
```

### Get Conversation Messages

Gets all messages from a conversation.

```bash
curl -X GET http://localhost:9000/api/chat/conversations/conversation-123/messages \
-H "Authorization: Bearer <your_token>"
```