package com.ecolightcline.project.repository;

import com.ecolightcline.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Page<Project> findByOwnerId(String ownerId, Pageable pageable);

    Page<Project> findByOwnerIdAndStatus(String ownerId, Project.ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.userId = :userId")
    Page<Project> findByMemberId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.userId = :userId AND p.status = :status")
    Page<Project> findByMemberIdAndStatus(@Param("userId") String userId, 
                                         @Param("status") Project.ProjectStatus status, 
                                         Pageable pageable);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(p) FROM Project p JOIN p.members m WHERE m.userId = :userId")
    long countByMemberId(@Param("userId") String userId);

    @Query("SELECT p FROM Project p WHERE p.status = :status")
    Page<Project> findByStatus(@Param("status") Project.ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.priority = :priority")
    Page<Project> findByPriority(@Param("priority") Project.ProjectPriority priority, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.title LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Project> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByIdAndOwnerId(String id, String ownerId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Project p JOIN p.members m WHERE p.id = :projectId AND m.userId = :userId")
    boolean isUserMemberOfProject(@Param("projectId") String projectId, @Param("userId") String userId);

    @Query("SELECT CASE WHEN p.ownerId = :userId THEN true ELSE false END FROM Project p WHERE p.id = :projectId")
    boolean isUserOwnerOfProject(@Param("projectId") String projectId, @Param("userId") String userId);
}
