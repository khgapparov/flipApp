package com.lovablecline.repository;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.ProjectUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, String> {
    
    List<ProjectUpdate> findByProject(Project project);
    
    List<ProjectUpdate> findByProjectId(String projectId);
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.project.id = :projectId ORDER BY pu.createdAt DESC")
    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") String projectId);
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.project.owner.id = :userId AND pu.project.id = :projectId")
    List<ProjectUpdate> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);
    
    @Query("SELECT pu FROM ProjectUpdate pu WHERE pu.project.owner.id = :userId")
    List<ProjectUpdate> findAllByUserId(@Param("userId") String userId);
    
    Optional<ProjectUpdate> findByIdAndProjectId(String id, String projectId);
}
