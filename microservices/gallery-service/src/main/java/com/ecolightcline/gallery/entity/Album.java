package com.ecolightcline.gallery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String albumId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String ownerId;

    @Column(length = 2000)
    private String description;

    private String coverImageId;

    @ElementCollection
    @CollectionTable(name = "album_tags", joinColumns = @JoinColumn(name = "album_id"))
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "album_images", joinColumns = @JoinColumn(name = "album_id"))
    private List<String> imageIds = new ArrayList<>();

    public Album() {
    }

    public Album(String title, String ownerId) {
        this.title = title;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImageId() {
        return coverImageId;
    }

    public void setCoverImageId(String coverImageId) {
        this.coverImageId = coverImageId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }

    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    public void addImage(String imageId) {
        if (this.imageIds == null) {
            this.imageIds = new ArrayList<>();
        }
        if (!this.imageIds.contains(imageId)) {
            this.imageIds.add(imageId);
        }
    }

    public void removeImage(String imageId) {
        if (this.imageIds != null) {
            this.imageIds.remove(imageId);
        }
    }

    public boolean containsImage(String imageId) {
        return this.imageIds != null && this.imageIds.contains(imageId);
    }

    public int getImageCount() {
        return this.imageIds != null ? this.imageIds.size() : 0;
    }

    public void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
    }
}
