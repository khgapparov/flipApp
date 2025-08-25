package com.lovablecline.controller;

import com.lovablecline.entity.GalleryItem;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.GalleryItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/gallery")
public class GalleryItemController {

    @Autowired
    private GalleryItemService galleryItemService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getAllGalleryItems(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<GalleryItem> galleryItems = galleryItemService.getAllGalleryItemsByProjectId(userId, projectId);
            return ResponseEntity.ok(galleryItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<?> getGalleryItem(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String projectId,
                                          @PathVariable String itemId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            return galleryItemService.getGalleryItemById(userId, projectId, itemId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createGalleryItem(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable String projectId,
                                             @RequestBody @Valid GalleryItem galleryItem) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            GalleryItem createdItem = galleryItemService.createGalleryItem(userId, projectId, galleryItem);
            return ResponseEntity.ok(createdItem);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadGalleryImage(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable String projectId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam("title") String title,
                                              @RequestParam("description") String description,
                                              @RequestParam("room") String room,
                                              @RequestParam("stage") String stage) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadDir.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath);
            
            // Create gallery item with file path
            GalleryItem galleryItem = new GalleryItem();
            galleryItem.setTitle(title);
            galleryItem.setDescription(description);
            galleryItem.setImageUrl("/uploads/" + uniqueFilename);
            galleryItem.setRoom(room);
            galleryItem.setStage(stage);
            galleryItem.setProjectId(projectId);
            
            GalleryItem createdItem = galleryItemService.createGalleryItem(userId, projectId, galleryItem);
            return ResponseEntity.ok(createdItem);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateGalleryItem(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable String projectId,
                                             @PathVariable String itemId,
                                             @RequestBody @Valid GalleryItem itemDetails) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            GalleryItem updatedItem = galleryItemService.updateGalleryItem(userId, projectId, itemId, itemDetails);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteGalleryItem(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable String projectId,
                                             @PathVariable String itemId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            galleryItemService.deleteGalleryItem(userId, projectId, itemId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/all")
    public ResponseEntity<?> getAllGalleryItemsByUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<GalleryItem> galleryItems = galleryItemService.getAllGalleryItemsByUserId(userId);
            return ResponseEntity.ok(galleryItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ordered")
    public ResponseEntity<?> getGalleryItemsOrdered(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<GalleryItem> galleryItems = galleryItemService.getGalleryItemsByProjectIdOrdered(projectId);
            return ResponseEntity.ok(galleryItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/room/{room}")
    public ResponseEntity<?> getGalleryItemsByRoom(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable String projectId,
                                                 @PathVariable String room) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<GalleryItem> galleryItems = galleryItemService.getGalleryItemsByRoom(projectId, room);
            return ResponseEntity.ok(galleryItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stage/{stage}")
    public ResponseEntity<?> getGalleryItemsByStage(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable String projectId,
                                                  @PathVariable String stage) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<GalleryItem> galleryItems = galleryItemService.getGalleryItemsByStage(projectId, stage);
            return ResponseEntity.ok(galleryItems);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }

    private String getUserIdFromToken(String token) {
        return authenticationService.getUserIdFromToken(token);
    }
}
