package com.lovablecline.service;

import com.lovablecline.entity.ChatMessage;
import com.lovablecline.entity.Project;
import com.lovablecline.repository.ChatMessageRepository;
import com.lovablecline.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<ChatMessage> getAllMessagesByProjectId(String userId, String projectId) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }
        return chatMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Optional<ChatMessage> getMessageById(String userId, String projectId, String messageId) {
        return chatMessageRepository.findByIdAndProjectId(messageId, projectId);
    }

    public ChatMessage createMessage(String userId, String projectId, ChatMessage message) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        Project project = projectOptional.get();
        message.setProject(project);
        message.setUserId(userId);
        return chatMessageRepository.save(message);
    }

    public ChatMessage updateMessage(String userId, String projectId, String messageId, ChatMessage messageDetails) {
        Optional<ChatMessage> messageOptional = chatMessageRepository.findByIdAndProjectId(messageId, projectId);
        if (messageOptional.isEmpty()) {
            throw new RuntimeException("Message not found");
        }

        ChatMessage message = messageOptional.get();
        
        if (messageDetails.getMessage() != null) {
            message.setMessage(messageDetails.getMessage());
        }
        if (messageDetails.getIsFromClient() != null) {
            message.setIsFromClient(messageDetails.getIsFromClient());
        }

        return chatMessageRepository.save(message);
    }

    public void deleteMessage(String userId, String projectId, String messageId) {
        Optional<ChatMessage> messageOptional = chatMessageRepository.findByIdAndProjectId(messageId, projectId);
        if (messageOptional.isEmpty()) {
            throw new RuntimeException("Message not found");
        }

        chatMessageRepository.delete(messageOptional.get());
    }

    public List<ChatMessage> getAllMessagesByUserId(String userId) {
        return chatMessageRepository.findAllByUserId(userId);
    }

    public List<ChatMessage> getMessagesByProjectIdOrdered(String projectId) {
        return chatMessageRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<ChatMessage> getMessagesByClientStatus(String projectId, Boolean isFromClient) {
        return chatMessageRepository.findByProjectIdAndIsFromClientOrderByCreatedAtDesc(projectId, isFromClient);
    }

    public Long countMessagesByClientStatus(String projectId, Boolean isFromClient) {
        return chatMessageRepository.countByProjectIdAndIsFromClient(projectId, isFromClient);
    }
}
