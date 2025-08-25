package com.lovablecline.repository;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    
    List<Project> findByOwner(User owner);
    
    List<Project> findByStatus(String status);
    
    Optional<Project> findByAddress(String address);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :userId AND p.id = :projectId")
    Optional<Project> findByUserIdAndProjectId(@Param("userId") String userId, @Param("projectId") String projectId);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :userId")
    List<Project> findAllByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.address = :address AND p.owner.id = :ownerId")
    boolean existsByAddressAndOwnerId(@Param("address") String address, @Param("ownerId") String ownerId);
}
