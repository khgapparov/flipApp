package com.lovablecline.service;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import com.lovablecline.repository.ProjectRepository;
import com.lovablecline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Project> getAllProjectsByUserId(String userId) {
        return projectRepository.findAllByUserId(userId);
    }

    public Optional<Project> getProjectById(String userId, String projectId) {
        return projectRepository.findByUserIdAndProjectId(userId, projectId);
    }

    public Project createProject(String userId, Project project) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        if (projectRepository.existsByAddressAndOwnerId(project.getAddress(), userId)) {
            throw new RuntimeException("Project with this address already exists for this user");
        }

        // Check if project with the same ID already exists
        if (project.getId() != null && projectRepository.existsById(project.getId())) {
            throw new RuntimeException("Project with this ID already exists");
        }

        User owner = userOptional.get();
        project.setOwner(owner);
        project.setOwnerId(owner.getId());
        return projectRepository.save(project);
    }

    public Project updateProject(String userId, String projectId, Project projectDetails) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        Project project = projectOptional.get();
        
        if (projectDetails.getAddress() != null && 
            !projectDetails.getAddress().equals(project.getAddress()) &&
            projectRepository.existsByAddressAndOwnerId(projectDetails.getAddress(), userId)) {
            throw new RuntimeException("Project with this address already exists for this user");
        }

        if (projectDetails.getAddress() != null) {
            project.setAddress(projectDetails.getAddress());
        }
        if (projectDetails.getStatus() != null) {
            project.setStatus(projectDetails.getStatus());
        }
        if (projectDetails.getStartDate() != null) {
            project.setStartDate(projectDetails.getStartDate());
        }
        if (projectDetails.getEstimatedCompletionDate() != null) {
            project.setEstimatedCompletionDate(projectDetails.getEstimatedCompletionDate());
        }
        if (projectDetails.getActualCompletionDate() != null) {
            project.setActualCompletionDate(projectDetails.getActualCompletionDate());
        }

        return projectRepository.save(project);
    }

    public void deleteProject(String userId, String projectId) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        projectRepository.delete(projectOptional.get());
    }

    public List<Project> getProjectsByStatus(String userId, String status) {
        List<Project> userProjects = projectRepository.findAllByUserId(userId);
        return userProjects.stream()
                .filter(project -> project.getStatus().equals(status))
                .toList();
    }

    public Project transferDemoProject(String userId, String projectId) {
        // Find the project by ID without user ownership check
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        Project project = projectOptional.get();
        
        // Check if user exists
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Transfer ownership to the current user
        project.setOwnerId(userId);
        project.setOwner(userOptional.get());
        
        return projectRepository.save(project);
    }
}
