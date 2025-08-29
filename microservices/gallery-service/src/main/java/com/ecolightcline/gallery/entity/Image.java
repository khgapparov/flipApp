package com.ecolightcline.gallery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String imageId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String ownerId;

    private String title;
    
    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "image_tags", joinColumns = @JoinColumn(name = "image_id"))
    private List<String> tags = new ArrayList<>();

    private Integer width;
    private Integer height;
    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageFormat format = ImageFormat.JPEG;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "image_albums", joinColumns = @JoinColumn(name = "image_id"))
    private List<String> albumIds = new ArrayList<>();

    public Image() {
    }

    public Image(String filename, String url, String thumbnailUrl, String ownerId, ImageFormat format) {
        this.filename = filename;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.ownerId = ownerId;
        this.format = format;
    }

    // Getters and Setters
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public List<String> getAlbumIds() {
        return albumIds;
    }

    public void setAlbumIds(List<String> albumIds) {
        this.albumIds = albumIds;
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

    public void addToAlbum(String albumId) {
        if (this.albumIds == null) {
            this.albumIds = new ArrayList<>();
        }
        if (!this.albumIds.contains(albumId)) {
            this.albumIds.add(albumId);
        }
    }

    public void removeFromAlbum(String albumId) {
        if (this.albumIds != null) {
            this.albumIds.remove(albumId);
        }
    }

    public enum ImageFormat {
        JPEG, PNG, GIF, WEBP, SVG, BMP
    }
}
