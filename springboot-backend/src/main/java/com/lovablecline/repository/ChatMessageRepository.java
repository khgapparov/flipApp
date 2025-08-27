package com.lovablecline.repository;

import com.lovablecline.entity.ChatMessage;
import com.lovablecline.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    List<ChatMessage> findByProject(Project project);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project.id = :projectId")
    List<ChatMessage> findByProjectId(@Param("projectId") String projectId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project.id = :projectId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") String projectId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId AND cm.project.id = :projectId")
    List<ChatMessage> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId")
    List<ChatMessage> findAllByUserId(@Param("userId") String userId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.id = :id AND cm.project.id = :projectId")
    Optional<ChatMessage> findByIdAndProjectId(@Param("id") String id, @Param("projectId") String projectId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project.id = :projectId AND cm.isFromClient = :isFromClient ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByProjectIdAndIsFromClientOrderByCreatedAtDesc(@Param("projectId") String projectId, @Param("isFromClient") Boolean isFromClient);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.project.id = :projectId AND cm.isFromClient = :isFromClient")
    Long countByProjectIdAndIsFromClient(@Param("projectId") String projectId, @Param("isFromClient") Boolean isFromClient);
}
