package com.lovablecline.controller;

import com.lovablecline.entity.ProjectUpdate;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ProjectUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/updates")
public class ProjectUpdateController {

    @Autowired
    private ProjectUpdateService projectUpdateService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getAllUpdates(@RequestHeader("Authorization") String authHeader, 
                                         @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ProjectUpdate> updates = projectUpdateService.getAllUpdatesByProjectId(userId, projectId);
            return ResponseEntity.ok(updates);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUpdate(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable String projectId,
                                     @PathVariable String id) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            return projectUpdateService.getUpdateById(userId, projectId, id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUpdate(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable String projectId,
                                        @RequestBody ProjectUpdate update) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            ProjectUpdate createdUpdate = projectUpdateService.createUpdate(userId, projectId, update);
            return ResponseEntity.ok(createdUpdate);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUpdate(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable String projectId,
                                        @PathVariable String id,
                                        @RequestBody ProjectUpdate updateDetails) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            ProjectUpdate updatedUpdate = projectUpdateService.updateUpdate(userId, projectId, id, updateDetails);
            return ResponseEntity.ok(updatedUpdate);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUpdate(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable String projectId,
                                        @PathVariable String id) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            projectUpdateService.deleteUpdate(userId, projectId, id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/all")
    public ResponseEntity<?> getAllUpdatesByUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ProjectUpdate> updates = projectUpdateService.getAllUpdatesByUserId(userId);
            return ResponseEntity.ok(updates);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ordered")
    public ResponseEntity<?> getUpdatesOrdered(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable String projectId) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<ProjectUpdate> updates = projectUpdateService.getUpdatesByProjectIdOrdered(projectId);
            return ResponseEntity.ok(updates);
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
