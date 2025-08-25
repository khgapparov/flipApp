package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.entity.ChatMessage;
import com.lovablecline.entity.Project;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatMessageController.class)
public class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatMessageService chatMessageService;

    @MockBean
    private AuthenticationService authenticationService;

    private String validToken;
    private String userId;
    private String projectId;
    private ChatMessage chatMessage;
    private Project project;

    @BeforeEach
    void setUp() {
        validToken = "valid-jwt-token";
        userId = "user123";
        projectId = "project456";
        
        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        
        chatMessage = new ChatMessage();
        chatMessage.setId("message123");
        chatMessage.setMessage("Hello, this is a test message");
        chatMessage.setIsFromClient(true);
        chatMessage.setProject(project);
        chatMessage.setCreatedAt(LocalDateTime.now());

        when(authenticationService.getUserIdFromToken(validToken)).thenReturn(userId);
        when(authenticationService.getUserIdFromToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));
    }

    @Test
    void getAllMessages_ValidRequest_ReturnsMessages() throws Exception {
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(chatMessageService.getAllMessagesByProjectId(userId, projectId)).thenReturn(messages);

        mockMvc.perform(get("/api/projects/{projectId}/chat", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("message123"))
                .andExpect(jsonPath("$[0].message").value("Hello, this is a test message"));
    }

    @Test
    void getAllMessages_InvalidToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/chat", projectId)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    void getAllMessages_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        when(chatMessageService.getAllMessagesByProjectId(userId, projectId))
                .thenThrow(new RuntimeException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}/chat", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getMessage_ValidRequest_ReturnsMessage() throws Exception {
        when(chatMessageService.getMessageById(userId, projectId, "message123"))
                .thenReturn(Optional.of(chatMessage));

        mockMvc.perform(get("/api/projects/{projectId}/chat/{messageId}", projectId, "message123")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("message123"))
                .andExpect(jsonPath("$.message").value("Hello, this is a test message"));
    }

    @Test
    void getMessage_MessageNotFound_ReturnsNotFound() throws Exception {
        when(chatMessageService.getMessageById(userId, projectId, "nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}/chat/{messageId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMessage_ValidRequest_ReturnsCreatedMessage() throws Exception {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessage("New test message");
        newMessage.setIsFromClient(false);

        when(chatMessageService.createMessage(eq(userId), eq(projectId), any(ChatMessage.class)))
                .thenReturn(chatMessage);

        mockMvc.perform(post("/api/projects/{projectId}/chat", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("message123"));
    }

    @Test
    void createMessage_InvalidData_ReturnsBadRequest() throws Exception {
        ChatMessage invalidMessage = new ChatMessage(); // Missing required fields

        mockMvc.perform(post("/api/projects/{projectId}/chat", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMessage)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMessage_ValidRequest_ReturnsUpdatedMessage() throws Exception {
        ChatMessage updateDetails = new ChatMessage();
        updateDetails.setMessage("Updated message content");
        updateDetails.setIsFromClient(false);

        when(chatMessageService.updateMessage(eq(userId), eq(projectId), eq("message123"), any(ChatMessage.class)))
                .thenReturn(chatMessage);

        mockMvc.perform(put("/api/projects/{projectId}/chat/{messageId}", projectId, "message123")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("message123"));
    }

    @Test
    void updateMessage_MessageNotFound_ReturnsBadRequest() throws Exception {
        ChatMessage updateDetails = new ChatMessage();
        updateDetails.setMessage("Updated message");

        when(chatMessageService.updateMessage(eq(userId), eq(projectId), eq("nonexistent"), any(ChatMessage.class)))
                .thenThrow(new RuntimeException("Message not found"));

        mockMvc.perform(put("/api/projects/{projectId}/chat/{messageId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Message not found"));
    }

    @Test
    void deleteMessage_ValidRequest_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/projects/{projectId}/chat/{messageId}", projectId, "message123")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMessage_MessageNotFound_ReturnsBadRequest() throws Exception {
        Mockito.doThrow(new RuntimeException("Message not found"))
                .when(chatMessageService).deleteMessage(userId, projectId, "nonexistent");

        mockMvc.perform(delete("/api/projects/{projectId}/chat/{messageId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Message not found"));
    }

    @Test
    void getAllMessagesByUser_ValidRequest_ReturnsMessages() throws Exception {
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(chatMessageService.getAllMessagesByUserId(userId)).thenReturn(messages);

        mockMvc.perform(get("/api/projects/{projectId}/chat/user/all", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("message123"));
    }

    @Test
    void getMessagesOrdered_ValidRequest_ReturnsOrderedMessages() throws Exception {
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(chatMessageService.getMessagesByProjectIdOrdered(projectId)).thenReturn(messages);

        mockMvc.perform(get("/api/projects/{projectId}/chat/ordered", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("message123"));
    }

    @Test
    void getMessagesByClientStatus_ValidRequest_ReturnsClientMessages() throws Exception {
        List<ChatMessage> messages = Arrays.asList(chatMessage);
        when(chatMessageService.getMessagesByClientStatus(projectId, true)).thenReturn(messages);

        mockMvc.perform(get("/api/projects/{projectId}/chat/client/{isFromClient}", projectId, true)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("message123"))
                .andExpect(jsonPath("$[0].isFromClient").value(true));
    }

    @Test
    void getMessagesByClientStatus_ValidRequest_ReturnsContractorMessages() throws Exception {
        ChatMessage contractorMessage = new ChatMessage();
        contractorMessage.setId("message456");
        contractorMessage.setMessage("Contractor response");
        contractorMessage.setIsFromClient(false);
        contractorMessage.setProject(project);

        List<ChatMessage> messages = Arrays.asList(contractorMessage);
        when(chatMessageService.getMessagesByClientStatus(projectId, false)).thenReturn(messages);

        mockMvc.perform(get("/api/projects/{projectId}/chat/client/{isFromClient}", projectId, false)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("message456"))
                .andExpect(jsonPath("$[0].isFromClient").value(false));
    }

    @Test
    void countMessagesByClientStatus_ValidRequest_ReturnsClientCount() throws Exception {
        when(chatMessageService.countMessagesByClientStatus(projectId, true)).thenReturn(5L);

        mockMvc.perform(get("/api/projects/{projectId}/chat/count/client/{isFromClient}", projectId, true)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void countMessagesByClientStatus_ValidRequest_ReturnsContractorCount() throws Exception {
        when(chatMessageService.countMessagesByClientStatus(projectId, false)).thenReturn(3L);

        mockMvc.perform(get("/api/projects/{projectId}/chat/count/client/{isFromClient}", projectId, false)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void getMessagesByClientStatus_NoMessagesFound_ReturnsEmptyList() throws Exception {
        when(chatMessageService.getMessagesByClientStatus(projectId, true)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/{projectId}/chat/client/{isFromClient}", projectId, true)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void countMessagesByClientStatus_NoMessagesFound_ReturnsZero() throws Exception {
        when(chatMessageService.countMessagesByClientStatus(projectId, true)).thenReturn(0L);

        mockMvc.perform(get("/api/projects/{projectId}/chat/count/client/{isFromClient}", projectId, true)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
