package com.ecolightcline.chat.service;

import com.ecolightcline.chat.entity.ChatMessage;
import com.ecolightcline.chat.entity.Conversation;
import com.ecolightcline.chat.repository.ChatMessageRepository;
import com.ecolightcline.chat.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, ConversationRepository conversationRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
    }

    public ChatMessage sendMessage(String conversationId, String senderId, String content, ChatMessage.MessageType messageType) {
        // Verify user is participant in conversation
        if (!conversationRepository.isUserParticipantInConversation(conversationId, senderId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        ChatMessage message = new ChatMessage(conversationId, senderId, content, messageType);
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update conversation's updated timestamp
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            conversationRepository.save(conversation);
        });

        return savedMessage;
    }

    public Page<ChatMessage> getMessages(String conversationId, String userId, Pageable pageable) {
        // Verify user is participant in conversation
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return chatMessageRepository.findByConversationId(conversationId, pageable);
    }

    public Page<ChatMessage> getMessagesBefore(String conversationId, String userId, LocalDateTime before, Pageable pageable) {
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return chatMessageRepository.findByConversationIdAndTimestampBefore(conversationId, before, pageable);
    }

    public Page<ChatMessage> getMessagesAfter(String conversationId, String userId, LocalDateTime after, Pageable pageable) {
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return chatMessageRepository.findByConversationIdAndTimestampAfter(conversationId, after, pageable);
    }

    public Optional<Conversation> getConversation(String conversationId, String userId) {
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return conversationRepository.findById(conversationId);
    }

    public Page<Conversation> getUserConversations(String userId, Pageable pageable) {
        return conversationRepository.findByParticipantIdOrderByUpdatedAtDesc(userId, pageable);
    }

    public Conversation createDirectConversation(String userId1, String username1, String userId2, String username2) {
        // Check if direct conversation already exists
        Optional<Conversation> existingConversation = conversationRepository.findDirectConversationBetweenUsers(userId1, userId2);
        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }

        Conversation conversation = new Conversation(Conversation.ConversationType.DIRECT);
        conversation.addParticipant(userId1, username1);
        conversation.addParticipant(userId2, username2);

        return conversationRepository.save(conversation);
    }

    public Conversation createGroupConversation(String creatorId, String creatorUsername, List<String> participantIds, List<String> participantUsernames) {
        Conversation conversation = new Conversation(Conversation.ConversationType.GROUP);
        conversation.addParticipant(creatorId, creatorUsername);

        for (int i = 0; i < participantIds.size(); i++) {
            conversation.addParticipant(participantIds.get(i), participantUsernames.get(i));
        }

        return conversationRepository.save(conversation);
    }

    public boolean markMessageAsRead(String messageId, String userId) {
        return chatMessageRepository.findById(messageId)
                .map(message -> {
                    if (!message.getRead()) {
                        message.setRead(true);
                        chatMessageRepository.save(message);

                        // Update conversation unread count
                        conversationRepository.findById(message.getConversationId()).ifPresent(conversation -> {
                            if (conversation.getUnreadCount() > 0) {
                                conversation.setUnreadCount(conversation.getUnreadCount() - 1);
                                conversationRepository.save(conversation);
                            }
                        });
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean deleteMessage(String messageId, String userId) {
        return chatMessageRepository.findById(messageId)
                .map(message -> {
                    if (!message.getSenderId().equals(userId)) {
                        throw new SecurityException("User can only delete their own messages");
                    }
                    chatMessageRepository.delete(message);
                    return true;
                })
                .orElse(false);
    }

    public boolean addParticipantToConversation(String conversationId, String userId, String username, String requestingUserId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> {
                    // Only group/channel owners or admins can add participants
                    if (!conversation.hasParticipant(requestingUserId)) {
                        throw new SecurityException("User is not a participant in this conversation");
                    }

                    if (conversation.hasParticipant(userId)) {
                        throw new IllegalStateException("User is already a participant in this conversation");
                    }

                    conversation.addParticipant(userId, username);
                    conversationRepository.save(conversation);
                    return true;
                })
                .orElse(false);
    }

    public boolean removeParticipantFromConversation(String conversationId, String userId, String requestingUserId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> {
                    // Users can remove themselves, or owners/admins can remove others
                    boolean isSelfRemoval = userId.equals(requestingUserId);
                    boolean isOwnerOrAdmin = conversation.hasParticipant(requestingUserId); // Simplified check

                    if (!isSelfRemoval && !isOwnerOrAdmin) {
                        throw new SecurityException("Only conversation participants can remove themselves or be removed by owners/admins");
                    }

                    conversation.removeParticipant(userId);
                    conversationRepository.save(conversation);
                    return true;
                })
                .orElse(false);
    }

    public long getUnreadMessageCount(String conversationId, String userId) {
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return chatMessageRepository.countUnreadMessagesByConversationId(conversationId);
    }

    public Page<ChatMessage> searchMessages(String conversationId, String userId, String keyword, Pageable pageable) {
        if (!conversationRepository.isUserParticipantInConversation(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return chatMessageRepository.searchByConversationIdAndKeyword(conversationId, keyword, pageable);
    }

    public Optional<ChatMessage> getMessageById(String messageId, String userId) {
        return chatMessageRepository.findById(messageId)
                .filter(message -> conversationRepository.isUserParticipantInConversation(message.getConversationId(), userId));
    }

    public boolean editMessage(String messageId, String userId, String newContent) {
        return chatMessageRepository.findById(messageId)
                .map(message -> {
                    if (!message.getSenderId().equals(userId)) {
                        throw new SecurityException("User can only edit their own messages");
                    }

                    message.setContent(newContent);
                    message.setEdited(true);
                    message.setEditedAt(LocalDateTime.now());
                    chatMessageRepository.save(message);
                    return true;
                })
                .orElse(false);
    }
}
