package com.ecolightcline.project.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class ProjectMember {

    private String userId;
    private String username;
    private MemberRole role;
    private LocalDateTime joinedAt = LocalDateTime.now();

    public ProjectMember() {
    }

    public ProjectMember(String userId, String username, MemberRole role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
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

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public enum MemberRole {
        OWNER, ADMIN, MEMBER, VIEWER
    }
}
