package com.ecolightcline.project.service;

import com.ecolightcline.project.entity.Project;
import com.ecolightcline.project.entity.ProjectMember;
import com.ecolightcline.project.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project createProject(Project project, String ownerId) {
        project.setOwnerId(ownerId);
        
        // Add owner as the first member with OWNER role
        ProjectMember ownerMember = new ProjectMember(ownerId, "Owner", ProjectMember.MemberRole.OWNER);
        project.getMembers().add(ownerMember);
        
        return projectRepository.save(project);
    }

    public Optional<Project> getProjectById(String id) {
        return projectRepository.findById(id);
    }

    public Optional<Project> updateProject(String id, Project projectDetails, String userId) {
        return projectRepository.findById(id)
                .map(existingProject -> {
                    if (!existingProject.getOwnerId().equals(userId)) {
                        throw new SecurityException("User is not the owner of this project");
                    }

                    if (projectDetails.getTitle() != null) {
                        existingProject.setTitle(projectDetails.getTitle());
                    }
                    if (projectDetails.getDescription() != null) {
                        existingProject.setDescription(projectDetails.getDescription());
                    }
                    if (projectDetails.getStatus() != null) {
                        existingProject.setStatus(projectDetails.getStatus());
                    }
                    if (projectDetails.getPriority() != null) {
                        existingProject.setPriority(projectDetails.getPriority());
                    }
                    if (projectDetails.getTags() != null) {
                        existingProject.setTags(projectDetails.getTags());
                    }
                    if (projectDetails.getStartDate() != null) {
                        existingProject.setStartDate(projectDetails.getStartDate());
                    }
                    if (projectDetails.getDueDate() != null) {
                        existingProject.setDueDate(projectDetails.getDueDate());
                    }
                    if (projectDetails.getProgress() != null) {
                        existingProject.setProgress(projectDetails.getProgress());
                    }

                    return projectRepository.save(existingProject);
                });
    }

    public boolean deleteProject(String id, String userId) {
        return projectRepository.findById(id)
                .map(project -> {
                    if (!project.getOwnerId().equals(userId)) {
                        throw new SecurityException("User is not the owner of this project");
                    }
                    projectRepository.delete(project);
                    return true;
                })
                .orElse(false);
    }

    public Page<Project> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    public Page<Project> getProjectsByOwner(String ownerId, Pageable pageable) {
        return projectRepository.findByOwnerId(ownerId, pageable);
    }

    public Page<Project> getProjectsByMember(String userId, Pageable pageable) {
        return projectRepository.findByMemberId(userId, pageable);
    }

    public Page<Project> getProjectsByStatus(Project.ProjectStatus status, Pageable pageable) {
        return projectRepository.findByStatus(status, pageable);
    }

    public Page<Project> getProjectsByPriority(Project.ProjectPriority priority, Pageable pageable) {
        return projectRepository.findByPriority(priority, pageable);
    }

    public Page<Project> searchProjects(String keyword, Pageable pageable) {
        return projectRepository.searchByKeyword(keyword, pageable);
    }

    public boolean addMemberToProject(String projectId, String userId, String username, ProjectMember.MemberRole role) {
        return projectRepository.findById(projectId)
                .map(project -> {
                    // Check if user is already a member
                    boolean isAlreadyMember = project.getMembers().stream()
                            .anyMatch(member -> member.getUserId().equals(userId));
                    
                    if (isAlreadyMember) {
                        throw new IllegalStateException("User is already a member of this project");
                    }

                    ProjectMember newMember = new ProjectMember(userId, username, role);
                    project.getMembers().add(newMember);
                    projectRepository.save(project);
                    return true;
                })
                .orElse(false);
    }

    public boolean removeMemberFromProject(String projectId, String userId, String requestingUserId) {
        return projectRepository.findById(projectId)
                .map(project -> {
                    // Only owner or the member themselves can remove a member
                    boolean isOwner = project.getOwnerId().equals(requestingUserId);
                    boolean isSelfRemoval = userId.equals(requestingUserId);
                    
                    if (!isOwner && !isSelfRemoval) {
                        throw new SecurityException("Only project owner or the member themselves can remove a member");
                    }

                    // Cannot remove owner
                    if (userId.equals(project.getOwnerId())) {
                        throw new IllegalStateException("Cannot remove project owner");
                    }

                    boolean removed = project.getMembers().removeIf(member -> 
                            member.getUserId().equals(userId) && !member.getUserId().equals(project.getOwnerId()));
                    
                    if (removed) {
                        projectRepository.save(project);
                    }
                    return removed;
                })
                .orElse(false);
    }

    public boolean updateMemberRole(String projectId, String userId, ProjectMember.MemberRole newRole, String requestingUserId) {
        return projectRepository.findById(projectId)
                .map(project -> {
                    // Only owner can update roles
                    if (!project.getOwnerId().equals(requestingUserId)) {
                        throw new SecurityException("Only project owner can update member roles");
                    }

                    // Cannot change owner's role
                    if (userId.equals(project.getOwnerId())) {
                        throw new IllegalStateException("Cannot change owner's role");
                    }

                    return project.getMembers().stream()
                            .filter(member -> member.getUserId().equals(userId))
                            .findFirst()
                            .map(member -> {
                                member.setRole(newRole);
                                projectRepository.save(project);
                                return true;
                            })
                            .orElse(false);
                })
                .orElse(false);
    }

    public boolean isUserOwnerOfProject(String projectId, String userId) {
        return projectRepository.isUserOwnerOfProject(projectId, userId);
    }

    public boolean isUserMemberOfProject(String projectId, String userId) {
        return projectRepository.isUserMemberOfProject(projectId, userId);
    }

    public long countProjectsByOwner(String ownerId) {
        return projectRepository.countByOwnerId(ownerId);
    }

    public long countProjectsByMember(String userId) {
        return projectRepository.countByMemberId(userId);
    }
}
