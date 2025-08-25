package com.lovablecline.service;

import com.lovablecline.entity.ChatMessage;
import com.lovablecline.entity.Project;
import com.lovablecline.repository.ChatMessageRepository;
import com.lovablecline.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private String userId;
    private String projectId;
    private String messageId;
    private Project project;
    private ChatMessage chatMessage;
    private ChatMessage chatMessage2;

    @BeforeEach
    void setUp() {
        userId = "user123";
        projectId = "project456";
        messageId = "message123";

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");

        chatMessage = new ChatMessage();
        chatMessage.setId(messageId);
        chatMessage.setMessage("Hello, this is a test message");
        chatMessage.setIsFromClient(true);
        chatMessage.setProject(project);
        chatMessage.setCreatedAt(LocalDateTime.now());

        chatMessage2 = new ChatMessage();
        chatMessage2.setId("message456");
        chatMessage2.setMessage("Another test message");
        chatMessage2.setIsFromClient(false);
        chatMessage2.setProject(project);
        chatMessage2.setCreatedAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void getAllMessagesByProjectId_ValidRequest_ReturnsMessages() {
        List<ChatMessage> expectedMessages = Arrays.asList(chatMessage, chatMessage2);
        when(chatMessageRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(expectedMessages);

        List<ChatMessage> result = chatMessageService.getAllMessagesByProjectId(userId, projectId);

        assertEquals(2, result.size());
        assertEquals("Hello, this is a test message", result.get(0).getMessage());
        assertEquals("Another test message", result.get(1).getMessage());
        verify(chatMessageRepository).findByUserIdAndProjectId(userId, projectId);
    }

    @Test
    void getAllMessagesByProjectId_NoMessagesFound_ReturnsEmptyList() {
        when(chatMessageRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(List.of());

        List<ChatMessage> result = chatMessageService.getAllMessagesByProjectId(userId, projectId);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository).findByUserIdAndProjectId(userId, projectId);
    }

    @Test
    void getMessageById_ValidRequest_ReturnsMessage() {
        when(chatMessageRepository.findByIdAndProjectId(messageId, projectId)).thenReturn(Optional.of(chatMessage));

        Optional<ChatMessage> result = chatMessageService.getMessageById(userId, projectId, messageId);

        assertTrue(result.isPresent());
        assertEquals("Hello, this is a test message", result.get().getMessage());
        verify(chatMessageRepository).findByIdAndProjectId(messageId, projectId);
    }

    @Test
    void getMessageById_MessageNotFound_ReturnsEmpty() {
        when(chatMessageRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        Optional<ChatMessage> result = chatMessageService.getMessageById(userId, projectId, "nonexistent");

        assertFalse(result.isPresent());
        verify(chatMessageRepository).findByIdAndProjectId("nonexistent", projectId);
    }

    @Test
    void createMessage_ValidRequest_ReturnsCreatedMessage() {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessage("New test message");
        newMessage.setIsFromClient(false);

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(project));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage savedMessage = invocation.getArgument(0);
            savedMessage.setId("new-message-id");
            return savedMessage;
        });

        ChatMessage result = chatMessageService.createMessage(userId, projectId, newMessage);

        assertNotNull(result.getId());
        assertEquals("New test message", result.getMessage());
        assertEquals(false, result.getIsFromClient());
        assertEquals(project, result.getProject());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void createMessage_ProjectNotFound_ThrowsException() {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessage("New test message");

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatMessageService.createMessage(userId, projectId, newMessage);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void updateMessage_ValidRequest_ReturnsUpdatedMessage() {
        ChatMessage updateDetails = new ChatMessage();
        updateDetails.setMessage("Updated message content");
        updateDetails.setIsFromClient(false);

        when(chatMessageRepository.findByIdAndProjectId(messageId, projectId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatMessageService.updateMessage(userId, projectId, messageId, updateDetails);

        assertEquals("Updated message content", result.getMessage());
        assertEquals(false, result.getIsFromClient());
        verify(chatMessageRepository).findByIdAndProjectId(messageId, projectId);
        verify(chatMessageRepository).save(chatMessage);
    }

    @Test
    void updateMessage_PartialUpdate_ReturnsUpdatedMessage() {
        ChatMessage updateDetails = new ChatMessage();
        updateDetails.setMessage("Updated message only");

        when(chatMessageRepository.findByIdAndProjectId(messageId, projectId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatMessageService.updateMessage(userId, projectId, messageId, updateDetails);

        assertEquals("Updated message only", result.getMessage());
        assertEquals(true, result.getIsFromClient()); // Should remain unchanged
        verify(chatMessageRepository).findByIdAndProjectId(messageId, projectId);
        verify(chatMessageRepository).save(chatMessage);
    }

    @Test
    void updateMessage_MessageNotFound_ThrowsException() {
        ChatMessage updateDetails = new ChatMessage();
        updateDetails.setMessage("Updated message");

        when(chatMessageRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatMessageService.updateMessage(userId, projectId, "nonexistent", updateDetails);
        });

        assertEquals("Message not found", exception.getMessage());
        verify(chatMessageRepository).findByIdAndProjectId("nonexistent", projectId);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void deleteMessage_ValidRequest_DeletesMessage() {
        when(chatMessageRepository.findByIdAndProjectId(messageId, projectId)).thenReturn(Optional.of(chatMessage));

        chatMessageService.deleteMessage(userId, projectId, messageId);

        verify(chatMessageRepository).findByIdAndProjectId(messageId, projectId);
        verify(chatMessageRepository).delete(chatMessage);
    }

    @Test
    void deleteMessage_MessageNotFound_ThrowsException() {
        when(chatMessageRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatMessageService.deleteMessage(userId, projectId, "nonexistent");
        });

        assertEquals("Message not found", exception.getMessage());
        verify(chatMessageRepository).findByIdAndProjectId("nonexistent", projectId);
        verify(chatMessageRepository, never()).delete(any());
    }

    @Test
    void getAllMessagesByUserId_ValidRequest_ReturnsMessages() {
        List<ChatMessage> expectedMessages = Arrays.asList(chatMessage, chatMessage2);
        when(chatMessageRepository.findAllByUserId(userId)).thenReturn(expectedMessages);

        List<ChatMessage> result = chatMessageService.getAllMessagesByUserId(userId);

        assertEquals(2, result.size());
        assertEquals("Hello, this is a test message", result.get(0).getMessage());
        assertEquals("Another test message", result.get(1).getMessage());
        verify(chatMessageRepository).findAllByUserId(userId);
    }

    @Test
    void getAllMessagesByUserId_NoMessagesFound_ReturnsEmptyList() {
        when(chatMessageRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<ChatMessage> result = chatMessageService.getAllMessagesByUserId(userId);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository).findAllByUserId(userId);
    }

    @Test
    void getMessagesByProjectIdOrdered_ValidRequest_ReturnsOrderedMessages() {
        List<ChatMessage> expectedMessages = Arrays.asList(chatMessage, chatMessage2);
        when(chatMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(expectedMessages);

        List<ChatMessage> result = chatMessageService.getMessagesByProjectIdOrdered(projectId);

        assertEquals(2, result.size());
        verify(chatMessageRepository).findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Test
    void getMessagesByClientStatus_ValidRequest_ReturnsClientMessages() {
        List<ChatMessage> expectedMessages = Arrays.asList(chatMessage);
        when(chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, true)).thenReturn(expectedMessages);

        List<ChatMessage> result = chatMessageService.getMessagesByClientStatus(projectId, true);

        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getIsFromClient());
        verify(chatMessageRepository).findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, true);
    }

    @Test
    void getMessagesByClientStatus_ValidRequest_ReturnsContractorMessages() {
        List<ChatMessage> expectedMessages = Arrays.asList(chatMessage2);
        when(chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, false)).thenReturn(expectedMessages);

        List<ChatMessage> result = chatMessageService.getMessagesByClientStatus(projectId, false);

        assertEquals(1, result.size());
        assertEquals(false, result.get(0).getIsFromClient());
        verify(chatMessageRepository).findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, false);
    }

    @Test
    void countMessagesByClientStatus_ValidRequest_ReturnsClientCount() {
        when(chatMessageRepository.countByProjectIdAndIsFromClient(projectId, true)).thenReturn(5L);

        Long result = chatMessageService.countMessagesByClientStatus(projectId, true);

        assertEquals(5L, result);
        verify(chatMessageRepository).countByProjectIdAndIsFromClient(projectId, true);
    }

    @Test
    void countMessagesByClientStatus_ValidRequest_ReturnsContractorCount() {
        when(chatMessageRepository.countByProjectIdAndIsFromClient(projectId, false)).thenReturn(3L);

        Long result = chatMessageService.countMessagesByClientStatus(projectId, false);

        assertEquals(3L, result);
        verify(chatMessageRepository).countByProjectIdAndIsFromClient(projectId, false);
    }

    @Test
    void getMessagesByClientStatus_NoMessagesFound_ReturnsEmptyList() {
        when(chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, true)).thenReturn(List.of());

        List<ChatMessage> result = chatMessageService.getMessagesByClientStatus(projectId, true);

        assertTrue(result.isEmpty());
        verify(chatMessageRepository).findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, true);
    }

    @Test
    void countMessagesByClientStatus_NoMessagesFound_ReturnsZero() {
        when(chatMessageRepository.countByProjectIdAndIsFromClient(projectId, true)).thenReturn(0L);

        Long result = chatMessageService.countMessagesByClientStatus(projectId, true);

        assertEquals(0L, result);
        verify(chatMessageRepository).countByProjectIdAndIsFromClient(projectId, true);
    }

    @Test
    void createMessage_NullFields_HandlesGracefully() {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessage("Test message");
        // isFromClient is null

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(project));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatMessageService.createMessage(userId, projectId, newMessage);

        assertNotNull(result);
        assertEquals("Test message", result.getMessage());
        assertNull(result.getIsFromClient());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
}
