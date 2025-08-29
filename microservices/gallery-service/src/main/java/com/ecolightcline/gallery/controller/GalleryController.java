package com.ecolightcline.gallery.controller;

import com.ecolightcline.gallery.entity.Album;
import com.ecolightcline.gallery.entity.Image;
import com.ecolightcline.gallery.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final GalleryService galleryService;

    @Autowired
    public GalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    // Image endpoints
    @PostMapping("/images/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") String ownerId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        
        try {
            Image image = galleryService.uploadImage(file, ownerId, title, description, tags);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable String imageId, @RequestParam String ownerId) {
        return galleryService.getImage(imageId, ownerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/images")
    public ResponseEntity<Page<Image>> getImages(
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Image> images = galleryService.getImagesByOwner(ownerId, pageable);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/images/search")
    public ResponseEntity<Page<Image>> searchImages(
            @RequestParam String ownerId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Image> images = galleryService.searchImages(ownerId, keyword, pageable);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/images/tag/{tag}")
    public ResponseEntity<Page<Image>> getImagesByTag(
            @PathVariable String tag,
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Image> images = galleryService.getImagesByTag(ownerId, tag, pageable);
        return ResponseEntity.ok(images);
    }

    @PutMapping("/images/{imageId}")
    public ResponseEntity<?> updateImage(
            @PathVariable String imageId,
            @RequestParam String ownerId,
            @RequestBody Map<String, Object> updates) {
        
        try {
            String title = (String) updates.get("title");
            String description = (String) updates.get("description");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) updates.get("tags");
            
            Image updatedImage = galleryService.updateImage(imageId, ownerId, title, description, tags);
            return ResponseEntity.ok(updatedImage);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable String imageId, @RequestParam String ownerId) {
        try {
            galleryService.deleteImage(imageId, ownerId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Album endpoints
    @PostMapping("/albums")
    public ResponseEntity<Album> createAlbum(
            @RequestBody Map<String, Object> albumData) {
        
        String title = (String) albumData.get("title");
        String ownerId = (String) albumData.get("ownerId");
        String description = (String) albumData.get("description");
        Boolean isPublic = (Boolean) albumData.get("isPublic");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) albumData.get("tags");
        
        Album album = galleryService.createAlbum(title, ownerId, description, isPublic, tags);
        return ResponseEntity.status(HttpStatus.CREATED).body(album);
    }

    @GetMapping("/albums/{albumId}")
    public ResponseEntity<?> getAlbum(
            @PathVariable String albumId,
            @RequestParam(required = false) String ownerId) {
        
        // If ownerId is provided, check ownership, otherwise allow public access
        if (ownerId != null) {
            return galleryService.getAlbum(albumId, ownerId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return galleryService.getAlbum(albumId, "")
                    .filter(Album::getIsPublic)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @GetMapping("/albums")
    public ResponseEntity<Page<Album>> getAlbums(
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Album> albums = galleryService.getAlbumsByOwner(ownerId, pageable);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/albums/public")
    public ResponseEntity<Page<Album>> getPublicAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Album> albums = galleryService.getPublicAlbums(pageable);
        return ResponseEntity.ok(albums);
    }

    @PutMapping("/albums/{albumId}")
    public ResponseEntity<?> updateAlbum(
            @PathVariable String albumId,
            @RequestParam String ownerId,
            @RequestBody Map<String, Object> updates) {
        
        try {
            String title = (String) updates.get("title");
            String description = (String) updates.get("description");
            Boolean isPublic = (Boolean) updates.get("isPublic");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) updates.get("tags");
            
            Album updatedAlbum = galleryService.updateAlbum(albumId, ownerId, title, description, isPublic, tags);
            return ResponseEntity.ok(updatedAlbum);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/albums/{albumId}")
    public ResponseEntity<?> deleteAlbum(@PathVariable String albumId, @RequestParam String ownerId) {
        try {
            galleryService.deleteAlbum(albumId, ownerId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Album-image relationship endpoints
    @PostMapping("/albums/{albumId}/images/{imageId}")
    public ResponseEntity<?> addImageToAlbum(
            @PathVariable String albumId,
            @PathVariable String imageId,
            @RequestParam String ownerId) {
        
        try {
            galleryService.addImageToAlbum(albumId, imageId, ownerId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/albums/{albumId}/images/{imageId}")
    public ResponseEntity<?> removeImageFromAlbum(
            @PathVariable String albumId,
            @PathVariable String imageId,
            @RequestParam String ownerId) {
        
        try {
            galleryService.removeImageFromAlbum(albumId, imageId, ownerId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/albums/{albumId}/cover")
    public ResponseEntity<?> setAlbumCover(
            @PathVariable String albumId,
            @RequestParam String imageId,
            @RequestParam String ownerId) {
        
        try {
            galleryService.setAlbumCover(albumId, imageId, ownerId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Statistics and utility endpoints
    @GetMapping("/stats/images/count")
    public ResponseEntity<Map<String, Long>> getImageCount(@RequestParam String ownerId) {
        long count = galleryService.getImageCount(ownerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/stats/albums/count")
    public ResponseEntity<Map<String, Long>> getAlbumCount(@RequestParam String ownerId) {
        long count = galleryService.getAlbumCount(ownerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getTags(@RequestParam String ownerId) {
        List<String> tags = galleryService.getDistinctTags(ownerId);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/tags/all")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = galleryService.getAllDistinctTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/images/latest")
    public ResponseEntity<Page<Image>> getLatestImages(
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Image> images = galleryService.getLatestImages(ownerId, pageable);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/albums/latest")
    public ResponseEntity<Page<Album>> getLatestAlbums(
            @RequestParam String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Album> albums = galleryService.getLatestAlbums(ownerId, pageable);
        return ResponseEntity.ok(albums);
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gallery service is healthy");
    }
}
