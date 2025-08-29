package com.ecolightcline.project.controller;

import com.ecolightcline.project.entity.Project;
import com.ecolightcline.project.entity.ProjectMember;
import com.ecolightcline.project.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(
            @RequestBody Project project,
            @RequestHeader("X-User-Id") String userId) {
        Project createdProject = projectService.createProject(project, userId);
        return ResponseEntity.ok(createdProject);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        
        Optional<Project> project = projectService.getProjectById(id);
        if (project.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to this project
        if (!projectService.isUserOwnerOfProject(id, userId) && 
            !projectService.isUserMemberOfProject(id, userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(project.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable String id,
            @RequestBody Project projectDetails,
            @RequestHeader("X-User-Id") String userId) {
        
        Optional<Project> updatedProject = projectService.updateProject(id, projectDetails, userId);
        return updatedProject.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean deleted = projectService.deleteProject(id, userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Page<Project>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.getAllProjects(pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<Page<Project>> getProjectsByOwner(
            @PathVariable String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.getProjectsByOwner(ownerId, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/member/{userId}")
    public ResponseEntity<Page<Project>> getProjectsByMember(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.getProjectsByMember(userId, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Project>> getProjectsByStatus(
            @PathVariable Project.ProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.getProjectsByStatus(status, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<Page<Project>> getProjectsByPriority(
            @PathVariable Project.ProjectPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.getProjectsByPriority(priority, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Project>> searchProjects(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Project> projects = projectService.searchProjects(keyword, pageable);
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<Void> addMemberToProject(
            @PathVariable String projectId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String requestingUserId) {
        
        String userId = (String) request.get("userId");
        String username = (String) request.get("username");
        String roleStr = (String) request.get("role");
        ProjectMember.MemberRole role = ProjectMember.MemberRole.valueOf(roleStr);

        boolean added = projectService.addMemberToProject(projectId, userId, username, role);
        return added ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMemberFromProject(
            @PathVariable String projectId,
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requestingUserId) {
        
        boolean removed = projectService.removeMemberFromProject(projectId, userId, requestingUserId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{projectId}/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable String projectId,
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String requestingUserId) {
        
        String roleStr = request.get("role");
        ProjectMember.MemberRole role = ProjectMember.MemberRole.valueOf(roleStr);

        boolean updated = projectService.updateMemberRole(projectId, userId, role, requestingUserId);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{projectId}/is-owner")
    public ResponseEntity<Boolean> isUserOwnerOfProject(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean isOwner = projectService.isUserOwnerOfProject(projectId, userId);
        return ResponseEntity.ok(isOwner);
    }

    @GetMapping("/{projectId}/is-member")
    public ResponseEntity<Boolean> isUserMemberOfProject(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean isMember = projectService.isUserMemberOfProject(projectId, userId);
        return ResponseEntity.ok(isMember);
    }

    @GetMapping("/owner/{ownerId}/count")
    public ResponseEntity<Long> countProjectsByOwner(@PathVariable String ownerId) {
        long count = projectService.countProjectsByOwner(ownerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/member/{userId}/count")
    public ResponseEntity<Long> countProjectsByMember(@PathVariable String userId) {
        long count = projectService.countProjectsByMember(userId);
        return ResponseEntity.ok(count);
    }
}
