package com.lovablecline.controller;

import com.lovablecline.entity.Project;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<?> getAllProjects(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = authenticationService.getUsernameFromToken(token);
            
            // In a real application, you would get user ID from token claims
            // For now, we'll use a placeholder - you'd need to implement user ID extraction
            String userId = getUserIdFromToken(token); // This needs to be implemented
            
            List<Project> projects = projectService.getAllProjectsByUserId(userId);
            return ResponseEntity.ok(projects);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            return projectService.getProjectById(userId, id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestHeader("Authorization") String authHeader, @RequestBody Project project) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            Project createdProject = projectService.createProject(userId, project);
            return ResponseEntity.ok(createdProject);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@RequestHeader("Authorization") String authHeader, 
                                         @PathVariable String id, 
                                         @RequestBody Project projectDetails) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            Project updatedProject = projectService.updateProject(userId, id, projectDetails);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            projectService.deleteProject(userId, id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getProjectsByStatus(@RequestHeader("Authorization") String authHeader, 
                                               @PathVariable String status) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            List<Project> projects = projectService.getProjectsByStatus(userId, status);
            return ResponseEntity.ok(projects);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/demo/{id}/transfer")
    public ResponseEntity<?> transferDemoProject(@RequestHeader("Authorization") String authHeader, 
                                               @PathVariable String id) {
        try {
            String token = extractToken(authHeader);
            String userId = getUserIdFromToken(token);
            
            Project transferredProject = projectService.transferDemoProject(userId, id);
            return ResponseEntity.ok(transferredProject);
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
