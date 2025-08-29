package com.ecolightcline.gallery.service;

import com.ecolightcline.gallery.entity.Album;
import com.ecolightcline.gallery.entity.Image;
import com.ecolightcline.gallery.repository.AlbumRepository;
import com.ecolightcline.gallery.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GalleryService {

    private final ImageRepository imageRepository;
    private final AlbumRepository albumRepository;
    private final ImageProcessingService imageProcessingService;

    @Autowired
    public GalleryService(ImageRepository imageRepository, 
                         AlbumRepository albumRepository,
                         ImageProcessingService imageProcessingService) {
        this.imageRepository = imageRepository;
        this.albumRepository = albumRepository;
        this.imageProcessingService = imageProcessingService;
    }

    // Image operations
    public Image uploadImage(MultipartFile file, String ownerId, String title, String description, List<String> tags) throws IOException {
        // Process the image
        ImageProcessingService.ImageProcessingResult result = imageProcessingService.processImage(file);
        
        // Create image entity
        Image image = new Image(
            file.getOriginalFilename(),
            result.getImageUrl(),
            result.getThumbnailUrl(),
            ownerId,
            Image.ImageFormat.valueOf(result.getFormat().toUpperCase())
        );
        
        image.setTitle(title);
        image.setDescription(description);
        if (tags != null) {
            image.setTags(tags);
        }
        image.setWidth(result.getWidth());
        image.setHeight(result.getHeight());
        image.setSize(result.getSize());
        
        return imageRepository.save(image);
    }

    public Optional<Image> getImage(String imageId, String ownerId) {
        return imageRepository.findById(imageId)
                .filter(image -> image.getOwnerId().equals(ownerId));
    }

    public Page<Image> getImagesByOwner(String ownerId, Pageable pageable) {
        return imageRepository.findByOwnerId(ownerId, pageable);
    }

    public Page<Image> searchImages(String ownerId, String keyword, Pageable pageable) {
        return imageRepository.searchByOwnerIdAndKeyword(ownerId, keyword, pageable);
    }

    public Page<Image> getImagesByTag(String ownerId, String tag, Pageable pageable) {
        return imageRepository.findByOwnerIdAndTag(ownerId, tag, pageable);
    }

    public Page<Image> getImagesByAlbum(String ownerId, String albumId, Pageable pageable) {
        return imageRepository.findByOwnerIdAndAlbumId(ownerId, albumId, pageable);
    }

    public Image updateImage(String imageId, String ownerId, String title, String description, List<String> tags) {
        return imageRepository.findById(imageId)
                .filter(image -> image.getOwnerId().equals(ownerId))
                .map(image -> {
                    if (title != null) image.setTitle(title);
                    if (description != null) image.setDescription(description);
                    if (tags != null) image.setTags(tags);
                    image.setUpdatedAt(LocalDateTime.now());
                    return imageRepository.save(image);
                })
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
    }

    public void deleteImage(String imageId, String ownerId) {
        Image image = imageRepository.findById(imageId)
                .filter(img -> img.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
        
        // Remove image from all albums
        List<Album> albums = albumRepository.findByImageId(imageId);
        for (Album album : albums) {
            album.removeImage(imageId);
            albumRepository.save(album);
        }
        
        // Delete image file and thumbnail
        try {
            Files.deleteIfExists(Paths.get(image.getUrl()));
            Files.deleteIfExists(Paths.get(image.getThumbnailUrl()));
        } catch (IOException e) {
            // Log the error but continue with database deletion
        }
        
        imageRepository.delete(image);
    }

    // Album operations
    public Album createAlbum(String title, String ownerId, String description, Boolean isPublic, List<String> tags) {
        Album album = new Album(title, ownerId);
        album.setDescription(description);
        album.setIsPublic(isPublic != null ? isPublic : false);
        if (tags != null) {
            album.setTags(tags);
        }
        return albumRepository.save(album);
    }

    public Optional<Album> getAlbum(String albumId, String ownerId) {
        return albumRepository.findById(albumId)
                .filter(album -> album.getOwnerId().equals(ownerId) || album.getIsPublic());
    }

    public Page<Album> getAlbumsByOwner(String ownerId, Pageable pageable) {
        return albumRepository.findByOwnerId(ownerId, pageable);
    }

    public Page<Album> getPublicAlbums(Pageable pageable) {
        return albumRepository.findPublicAlbums(pageable);
    }

    public Album updateAlbum(String albumId, String ownerId, String title, String description, Boolean isPublic, List<String> tags) {
        return albumRepository.findById(albumId)
                .filter(album -> album.getOwnerId().equals(ownerId))
                .map(album -> {
                    if (title != null) album.setTitle(title);
                    if (description != null) album.setDescription(description);
                    if (isPublic != null) album.setIsPublic(isPublic);
                    if (tags != null) album.setTags(tags);
                    album.updateTimestamps();
                    return albumRepository.save(album);
                })
                .orElseThrow(() -> new RuntimeException("Album not found or access denied"));
    }

    public void deleteAlbum(String albumId, String ownerId) {
        Album album = albumRepository.findById(albumId)
                .filter(a -> a.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Album not found or access denied"));
        
        // Remove album reference from all images
        List<Image> images = imageRepository.findByAlbumId(albumId, Pageable.unpaged()).getContent();
        for (Image image : images) {
            image.removeFromAlbum(albumId);
            imageRepository.save(image);
        }
        
        albumRepository.delete(album);
    }

    // Album-image relationship operations
    public void addImageToAlbum(String albumId, String imageId, String ownerId) {
        Album album = albumRepository.findById(albumId)
                .filter(a -> a.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Album not found or access denied"));
        
        Image image = imageRepository.findById(imageId)
                .filter(img -> img.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
        
        album.addImage(imageId);
        image.addToAlbum(albumId);
        
        albumRepository.save(album);
        imageRepository.save(image);
    }

    public void removeImageFromAlbum(String albumId, String imageId, String ownerId) {
        Album album = albumRepository.findById(albumId)
                .filter(a -> a.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Album not found or access denied"));
        
        Image image = imageRepository.findById(imageId)
                .filter(img -> img.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
        
        album.removeImage(imageId);
        image.removeFromAlbum(albumId);
        
        albumRepository.save(album);
        imageRepository.save(image);
    }

    public void setAlbumCover(String albumId, String imageId, String ownerId) {
        Album album = albumRepository.findById(albumId)
                .filter(a -> a.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Album not found or access denied"));
        
        // Verify the image exists and belongs to the owner
        imageRepository.findById(imageId)
                .filter(img -> img.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
        
        album.setCoverImageId(imageId);
        albumRepository.save(album);
    }

    // Statistics and utility methods
    public long getImageCount(String ownerId) {
        return imageRepository.countByOwnerId(ownerId);
    }

    public long getAlbumCount(String ownerId) {
        return albumRepository.countByOwnerId(ownerId);
    }

    public List<String> getDistinctTags(String ownerId) {
        return imageRepository.findDistinctTagsByOwnerId(ownerId);
    }

    public List<String> getAllDistinctTags() {
        return imageRepository.findAllDistinctTags();
    }

    public Page<Image> getLatestImages(String ownerId, Pageable pageable) {
        return imageRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    public Page<Album> getLatestAlbums(String ownerId, Pageable pageable) {
        return albumRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
    }

    public boolean imageExists(String imageId, String ownerId) {
        return imageRepository.existsByImageIdAndOwnerId(imageId, ownerId);
    }

    public boolean albumExists(String albumId, String ownerId) {
        return albumRepository.existsByAlbumIdAndOwnerId(albumId, ownerId);
    }
}
