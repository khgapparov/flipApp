package com.lovablecline.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_items")
public class GalleryItem {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @Column(name = "project_id")
    private String projectId;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @NotBlank
    @Size(max = 100)
    private String title;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 50)
    private String room; // e.g., "Kitchen", "Bathroom", "Living Room"
    
    @Size(max = 50)
    private String stage; // e.g., "Before", "During", "After"
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    @JsonBackReference("project-gallery")
    private Project project;
    
    public GalleryItem() {
        this.createdAt = LocalDateTime.now();
    }
    
    public GalleryItem(String imageUrl, String title, String description, String room, String stage, String projectId) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.room = room;
        this.stage = stage;
        this.projectId = projectId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public String getStage() {
        return stage;
    }
    
    public void setStage(String stage) {
        this.stage = stage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Project getProject() {
        return project;
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public String getCaption() {
        return title + " - " + description;
    }
    
    @JsonIgnore
    public void setCaption(String caption) {
        // For backward compatibility, parse caption into title and description
        if (caption != null && caption.contains(" - ")) {
            String[] parts = caption.split(" - ", 2);
            this.title = parts[0];
            this.description = parts.length > 1 ? parts[1] : "";
        } else {
            this.title = caption;
            this.description = "";
        }
    }
}
