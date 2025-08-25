package com.lovablecline.repository;

import com.lovablecline.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.isAnonymous = true AND u.createdAt < :cutoffDate")
    List<User> findExpiredAnonymousUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    List<User> findByIsAnonymous(Boolean isAnonymous);
}
