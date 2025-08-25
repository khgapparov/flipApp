package com.lovablecline.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @Size(max = 50)
    private String status = "Planning"; // Planning, Renovation, For Sale, Completed
    
    @Size(max = 200)
    private String address;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "estimated_end_date")
    private LocalDateTime estimatedEndDate;
    
    @Column(name = "projected_profit_status")
    @Size(max = 50)
    private String projectedProfitStatus = "on track"; // on track, ahead, delay
    
    @Column(name = "owner_id")
    private String ownerId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    @JsonBackReference("user-projects")
    private User owner;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("project-updates")
    private List<ProjectUpdate> updates = new ArrayList<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("project-gallery")
    private List<GalleryItem> galleryItems = new ArrayList<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("project-chat")
    private List<ChatMessage> chatMessages = new ArrayList<>();
    
    public Project() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Project(String name, String status, String address, LocalDateTime startDate, 
                  LocalDateTime estimatedEndDate, String projectedProfitStatus, String ownerId) {
        this.name = name;
        this.status = status;
        this.address = address;
        this.startDate = startDate;
        this.estimatedEndDate = estimatedEndDate;
        this.projectedProfitStatus = projectedProfitStatus;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEstimatedEndDate() {
        return estimatedEndDate;
    }
    
    public void setEstimatedEndDate(LocalDateTime estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }
    
    public String getProjectedProfitStatus() {
        return projectedProfitStatus;
    }
    
    public void setProjectedProfitStatus(String projectedProfitStatus) {
        this.projectedProfitStatus = projectedProfitStatus;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public void setUser(User user) {
        this.owner = user;
    }
    
    public LocalDateTime getEstimatedCompletionDate() {
        return estimatedEndDate;
    }
    
    public void setEstimatedCompletionDate(LocalDateTime estimatedCompletionDate) {
        this.estimatedEndDate = estimatedCompletionDate;
    }
    
    public LocalDateTime getActualCompletionDate() {
        return estimatedEndDate; // This should probably be a separate field, but using estimatedEndDate for now
    }
    
    public void setActualCompletionDate(LocalDateTime actualCompletionDate) {
        this.estimatedEndDate = actualCompletionDate; // This should probably be a separate field
    }
    
    public List<ProjectUpdate> getUpdates() {
        return updates;
    }
    
    public void setUpdates(List<ProjectUpdate> updates) {
        this.updates = updates;
    }
    
    public List<GalleryItem> getGalleryItems() {
        return galleryItems;
    }
    
    public void setGalleryItems(List<GalleryItem> galleryItems) {
        this.galleryItems = galleryItems;
    }
    
    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }
    
    public void setChatMessages(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }
}
