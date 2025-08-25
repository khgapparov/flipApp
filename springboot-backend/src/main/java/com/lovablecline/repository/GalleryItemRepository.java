package com.lovablecline.repository;

import com.lovablecline.entity.GalleryItem;
import com.lovablecline.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryItemRepository extends JpaRepository<GalleryItem, String> {
    
    List<GalleryItem> findByProject(Project project);
    
    List<GalleryItem> findByProjectId(String projectId);
    
    @Query("SELECT gi FROM GalleryItem gi WHERE gi.project.id = :projectId ORDER BY gi.createdAt DESC")
    List<GalleryItem> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") String projectId);
    
    @Query("SELECT gi FROM GalleryItem gi WHERE gi.project.owner.id = :userId AND gi.project.id = :projectId")
    List<GalleryItem> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);
    
    @Query("SELECT gi FROM GalleryItem gi WHERE gi.project.owner.id = :userId")
    List<GalleryItem> findAllByUserId(@Param("userId") String userId);
    
    Optional<GalleryItem> findByIdAndProjectId(String id, String projectId);
    
    @Query("SELECT gi FROM GalleryItem gi WHERE gi.project.id = :projectId AND gi.room = :room")
    List<GalleryItem> findByProjectIdAndRoom(@Param("projectId") String projectId, @Param("room") String room);
    
    @Query("SELECT gi FROM GalleryItem gi WHERE gi.project.id = :projectId AND gi.stage = :stage")
    List<GalleryItem> findByProjectIdAndStage(@Param("projectId") String projectId, @Param("stage") String stage);
}
