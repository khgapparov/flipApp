package com.lovablecline.service;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.ProjectUpdate;
import com.lovablecline.repository.ProjectRepository;
import com.lovablecline.repository.ProjectUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectUpdateService {

    @Autowired
    private ProjectUpdateRepository projectUpdateRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<ProjectUpdate> getAllUpdatesByProjectId(String userId, String projectId) {
        return projectUpdateRepository.findByUserIdAndProjectId(userId, projectId);
    }

    public Optional<ProjectUpdate> getUpdateById(String userId, String projectId, String updateId) {
        return projectUpdateRepository.findByIdAndProjectId(updateId, projectId);
    }

    public ProjectUpdate createUpdate(String userId, String projectId, ProjectUpdate update) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        update.setProject(projectOptional.get());
        return projectUpdateRepository.save(update);
    }

    public ProjectUpdate updateUpdate(String userId, String projectId, String updateId, ProjectUpdate updateDetails) {
        Optional<ProjectUpdate> updateOptional = projectUpdateRepository.findByIdAndProjectId(updateId, projectId);
        if (updateOptional.isEmpty()) {
            throw new RuntimeException("Update not found");
        }

        ProjectUpdate update = updateOptional.get();
        
        if (updateDetails.getTitle() != null) {
            update.setTitle(updateDetails.getTitle());
        }
        if (updateDetails.getDescription() != null) {
            update.setDescription(updateDetails.getDescription());
        }

        return projectUpdateRepository.save(update);
    }

    public void deleteUpdate(String userId, String projectId, String updateId) {
        Optional<ProjectUpdate> updateOptional = projectUpdateRepository.findByIdAndProjectId(updateId, projectId);
        if (updateOptional.isEmpty()) {
            throw new RuntimeException("Update not found");
        }

        projectUpdateRepository.delete(updateOptional.get());
    }

    public List<ProjectUpdate> getAllUpdatesByUserId(String userId) {
        return projectUpdateRepository.findAllByUserId(userId);
    }

    public List<ProjectUpdate> getUpdatesByProjectIdOrdered(String projectId) {
        return projectUpdateRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
