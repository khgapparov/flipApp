package com.ecolightcline.chat.repository;

import com.ecolightcline.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId")
    Page<Conversation> findByParticipantId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId AND c.type = :type")
    Page<Conversation> findByParticipantIdAndType(
            @Param("userId") String userId,
            @Param("type") Conversation.ConversationType type,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.type = :type")
    Page<Conversation> findByType(@Param("type") Conversation.ConversationType type, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants p WHERE p.userId = :userId")
    long countByParticipantId(@Param("userId") String userId);

    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants p WHERE p.userId = :userId AND c.type = :type")
    long countByParticipantIdAndType(
            @Param("userId") String userId,
            @Param("type") Conversation.ConversationType type);

    @Query("SELECT c FROM Conversation c WHERE c.unreadCount > 0")
    Page<Conversation> findConversationsWithUnreadMessages(Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId AND c.unreadCount > 0")
    Page<Conversation> findConversationsWithUnreadMessagesByParticipantId(
            @Param("userId") String userId,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE SIZE(c.participants) > :minParticipants")
    Page<Conversation> findByMinParticipants(
            @Param("minParticipants") int minParticipants,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE SIZE(c.participants) = :exactParticipants")
    Page<Conversation> findByExactParticipants(
            @Param("exactParticipants") int exactParticipants,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.updatedAt > :since")
    Page<Conversation> findRecentlyUpdated(
            @Param("since") java.time.LocalDateTime since,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId AND c.updatedAt > :since")
    Page<Conversation> findRecentlyUpdatedByParticipantId(
            @Param("userId") String userId,
            @Param("since") java.time.LocalDateTime since,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.conversationId IN :conversationIds")
    List<Conversation> findByConversationIds(@Param("conversationIds") List<String> conversationIds);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Conversation c JOIN c.participants p WHERE c.conversationId = :conversationId AND p.userId = :userId")
    boolean isUserParticipantInConversation(
            @Param("conversationId") String conversationId,
            @Param("userId") String userId);

    @Query("SELECT c FROM Conversation c WHERE c.conversationId = :conversationId AND SIZE(c.participants) = 2")
    Optional<Conversation> findDirectConversationById(@Param("conversationId") String conversationId);

    @Query("SELECT c FROM Conversation c WHERE SIZE(c.participants) = 2 AND EXISTS (SELECT p FROM c.participants p WHERE p.userId = :userId1) AND EXISTS (SELECT p FROM c.participants p WHERE p.userId = :userId2)")
    Optional<Conversation> findDirectConversationBetweenUsers(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'GROUP' AND c.conversationId = :conversationId")
    Optional<Conversation> findGroupConversationById(@Param("conversationId") String conversationId);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'CHANNEL' AND c.conversationId = :conversationId")
    Optional<Conversation> findChannelConversationById(@Param("conversationId") String conversationId);

    @Query("SELECT c FROM Conversation c ORDER BY c.updatedAt DESC")
    Page<Conversation> findAllOrderByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipantIdOrderByUpdatedAtDesc(
            @Param("userId") String userId,
            Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.unreadCount > 0 ORDER BY c.updatedAt DESC")
    Page<Conversation> findConversationsWithUnreadMessagesOrderByUpdatedAtDesc(Pageable pageable);
}
