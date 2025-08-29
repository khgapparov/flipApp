package com.ecolightcline.chat.controller;

import com.ecolightcline.chat.entity.ChatMessage;
import com.ecolightcline.chat.entity.Conversation;
import com.ecolightcline.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String userId) {
        
        String conversationId = (String) request.get("conversationId");
        String content = (String) request.get("content");
        String messageTypeStr = (String) request.get("messageType");
        
        ChatMessage.MessageType messageType = messageTypeStr != null ? 
            ChatMessage.MessageType.valueOf(messageTypeStr) : ChatMessage.MessageType.TEXT;

        ChatMessage message = chatService.sendMessage(conversationId, userId, content, messageType);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessage>> getMessages(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ChatMessage> messages = chatService.getMessages(conversationId, userId, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}/messages/before")
    public ResponseEntity<Page<ChatMessage>> getMessagesBefore(
            @PathVariable String conversationId,
            @RequestParam LocalDateTime before,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> messages = chatService.getMessagesBefore(conversationId, userId, before, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}/messages/after")
    public ResponseEntity<Page<ChatMessage>> getMessagesAfter(
            @PathVariable String conversationId,
            @RequestParam LocalDateTime after,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
        Page<ChatMessage> messages = chatService.getMessagesAfter(conversationId, userId, after, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Conversation> getConversation(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") String userId) {
        
        Optional<Conversation> conversation = chatService.getConversation(conversationId, userId);
        return conversation.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<Conversation>> getUserConversations(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Conversation> conversations = chatService.getUserConversations(userId, pageable);
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<Conversation> createDirectConversation(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        
        String otherUserId = request.get("otherUserId");
        String otherUsername = request.get("otherUsername");
        
        Conversation conversation = chatService.createDirectConversation(userId, username, otherUserId, otherUsername);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/conversations/group")
    public ResponseEntity<Conversation> createGroupConversation(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) request.get("participantIds");
        @SuppressWarnings("unchecked")
        List<String> participantUsernames = (List<String>) request.get("participantUsernames");
        
        Conversation conversation = chatService.createGroupConversation(userId, username, participantIds, participantUsernames);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<String> markMessageAsRead(
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean marked = chatService.markMessageAsRead(messageId, userId);
        return marked ? ResponseEntity.ok("Message marked as read") : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean deleted = chatService.deleteMessage(messageId, userId);
        return deleted ? ResponseEntity.ok("Message deleted") : ResponseEntity.notFound().build();
    }

    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<String> addParticipant(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String requestingUserId) {
        
        String userId = request.get("userId");
        String username = request.get("username");
        
        boolean added = chatService.addParticipantToConversation(conversationId, userId, username, requestingUserId);
        return added ? ResponseEntity.ok("Participant added") : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<String> removeParticipant(
            @PathVariable String conversationId,
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requestingUserId) {
        
        boolean removed = chatService.removeParticipantFromConversation(conversationId, userId, requestingUserId);
        return removed ? ResponseEntity.ok("Participant removed") : ResponseEntity.notFound().build();
    }

    @GetMapping("/conversations/{conversationId}/unread-count")
    public ResponseEntity<Long> getUnreadMessageCount(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") String userId) {
        
        long count = chatService.getUnreadMessageCount(conversationId, userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/conversations/{conversationId}/search")
    public ResponseEntity<Page<ChatMessage>> searchMessages(
            @PathVariable String conversationId,
            @RequestParam String keyword,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> messages = chatService.searchMessages(conversationId, userId, keyword, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessage> getMessageById(
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId) {
        
        Optional<ChatMessage> message = chatService.getMessageById(messageId, userId);
        return message.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<String> editMessage(
            @PathVariable String messageId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String userId) {
        
        String newContent = request.get("content");
        boolean edited = chatService.editMessage(messageId, userId, newContent);
        return edited ? ResponseEntity.ok("Message edited") : ResponseEntity.notFound().build();
    }
}
