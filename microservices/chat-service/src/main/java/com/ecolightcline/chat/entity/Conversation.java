package com.ecolightcline.chat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type = ConversationType.DIRECT;

    @ElementCollection
    @CollectionTable(name = "conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"))
    private List<Participant> participants = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private Integer unreadCount = 0;

    public Conversation() {
    }

    public Conversation(ConversationType type) {
        this.type = type;
    }

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void addParticipant(String userId, String username) {
        Participant participant = new Participant(userId, username);
        this.participants.add(participant);
    }

    public void removeParticipant(String userId) {
        this.participants.removeIf(p -> p.getUserId().equals(userId));
    }

    public boolean hasParticipant(String userId) {
        return this.participants.stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    public enum ConversationType {
        DIRECT, GROUP, CHANNEL
    }

    @Embeddable
    public static class Participant {
        private String userId;
        private String username;
        private LocalDateTime joinedAt = LocalDateTime.now();
        private LocalDateTime lastReadAt;

        public Participant() {
        }

        public Participant(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        // Getters and Setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }

        public LocalDateTime getLastReadAt() {
            return lastReadAt;
        }

        public void setLastReadAt(LocalDateTime lastReadAt) {
            this.lastReadAt = lastReadAt;
        }
    }
}
