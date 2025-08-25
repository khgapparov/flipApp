package com.lovablecline.controller;

import com.lovablecline.entity.ChatMessage;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/chat")
public class ChatMessageController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getAllMessages(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ChatMessage> messages = chatMessageService.getAllMessagesByProjectId(userId, projectId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable String projectId,
                                      @PathVariable String messageId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            return chatMessageService.getMessageById(userId, projectId, messageId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMessage(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable String projectId,
                                         @RequestBody @Valid ChatMessage message) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            ChatMessage createdMessage = chatMessageService.createMessage(userId, projectId, message);
            return ResponseEntity.ok(createdMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<?> updateMessage(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable String projectId,
                                         @PathVariable String messageId,
                                         @RequestBody @Valid ChatMessage messageDetails) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            ChatMessage updatedMessage = chatMessageService.updateMessage(userId, projectId, messageId, messageDetails);
            return ResponseEntity.ok(updatedMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable String projectId,
                                         @PathVariable String messageId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            chatMessageService.deleteMessage(userId, projectId, messageId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/all")
    public ResponseEntity<?> getAllMessagesByUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ChatMessage> messages = chatMessageService.getAllMessagesByUserId(userId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ordered")
    public ResponseEntity<?> getMessagesOrdered(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ChatMessage> messages = chatMessageService.getMessagesByProjectIdOrdered(projectId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/client/{isFromClient}")
    public ResponseEntity<?> getMessagesByClientStatus(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable String projectId,
                                                     @PathVariable Boolean isFromClient) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ChatMessage> messages = chatMessageService.getMessagesByClientStatus(projectId, isFromClient);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count/client/{isFromClient}")
    public ResponseEntity<?> countMessagesByClientStatus(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable String projectId,
                                                       @PathVariable Boolean isFromClient) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            Long count = chatMessageService.countMessagesByClientStatus(projectId, isFromClient);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }

    private String getUserIdFromToken(String token) {
        return authenticationService.getUserIdFromToken(token);
    }
}
