package com.ecolightcline.chat.repository;

import com.ecolightcline.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    Page<ChatMessage> findByConversationId(String conversationId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.timestamp < :before ORDER BY cm.timestamp DESC")
    Page<ChatMessage> findByConversationIdAndTimestampBefore(
            @Param("conversationId") String conversationId,
            @Param("before") LocalDateTime before,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.timestamp > :after ORDER BY cm.timestamp ASC")
    Page<ChatMessage> findByConversationIdAndTimestampAfter(
            @Param("conversationId") String conversationId,
            @Param("after") LocalDateTime after,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.senderId = :senderId")
    Page<ChatMessage> findByConversationIdAndSenderId(
            @Param("conversationId") String conversationId,
            @Param("senderId") String senderId,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.read = false")
    List<ChatMessage> findUnreadMessagesByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.read = false")
    long countUnreadMessagesByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findLatestMessagesByConversationId(
            @Param("conversationId") String conversationId,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.messageType = :messageType")
    Page<ChatMessage> findByConversationIdAndMessageType(
            @Param("conversationId") String conversationId,
            @Param("messageType") ChatMessage.MessageType messageType,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.content LIKE %:keyword%")
    Page<ChatMessage> searchByConversationIdAndKeyword(
            @Param("conversationId") String conversationId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.senderId = :senderId")
    Page<ChatMessage> findBySenderId(@Param("senderId") String senderId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.timestamp BETWEEN :start AND :end")
    Page<ChatMessage> findByConversationIdAndTimestampBetween(
            @Param("conversationId") String conversationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.timestamp ASC")
    List<ChatMessage> findAllByConversationIdOrderByTimestampAsc(@Param("conversationId") String conversationId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findAllByConversationIdOrderByTimestampDesc(@Param("conversationId") String conversationId);

    @Query("SELECT MAX(cm.timestamp) FROM ChatMessage cm WHERE cm.conversationId = :conversationId")
    Optional<LocalDateTime> findLastMessageTimestampByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationId = :conversationId")
    long countByConversationId(@Param("conversationId") String conversationId);

    boolean existsByMessageIdAndSenderId(String messageId, String senderId);
}
