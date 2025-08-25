package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private AuthenticationService authenticationService;

    private String validToken = "valid-token";
    private String userId = "user-123";
    private Project testProject;
    private Project testProject2;

    @BeforeEach
    void setUp() {
        // Mock authentication service
        when(authenticationService.getUsernameFromToken(validToken)).thenReturn("testuser");
        when(authenticationService.getUserIdFromToken(validToken)).thenReturn(userId);

        // Create test projects
        User owner = new User();
        owner.setId(userId);
        owner.setUsername("testuser");

        testProject = new Project();
        testProject.setId("project-1");
        testProject.setName("Test Project 1");
        testProject.setAddress("123 Test St");
        testProject.setStatus("active");
        testProject.setOwner(owner);
        testProject.setOwnerId(userId);

        testProject2 = new Project();
        testProject2.setId("project-2");
        testProject2.setName("Test Project 2");
        testProject2.setAddress("456 Test St");
        testProject2.setStatus("completed");
        testProject2.setOwner(owner);
        testProject2.setOwnerId(userId);
    }

    @Test
    void getAllProjects_shouldReturnProjects() throws Exception {
        // Given
        List<Project> projects = Arrays.asList(testProject, testProject2);
        when(projectService.getAllProjectsByUserId(userId)).thenReturn(projects);

        // When & Then
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Project 1"))
                .andExpect(jsonPath("$[1].name").value("Test Project 2"));

        verify(projectService).getAllProjectsByUserId(userId);
    }

    @Test
    void getAllProjects_shouldReturnErrorForInvalidToken() throws Exception {
        // Given
        when(authenticationService.getUserIdFromToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    void getProject_shouldReturnProjectWhenExists() throws Exception {
        // Given
        when(projectService.getProjectById(userId, "project-1")).thenReturn(Optional.of(testProject));

        // When & Then
        mockMvc.perform(get("/api/projects/project-1")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project 1"));

        verify(projectService).getProjectById(userId, "project-1");
    }

    @Test
    void getProject_shouldReturnNotFoundWhenProjectDoesNotExist() throws Exception {
        // Given
        when(projectService.getProjectById(userId, "non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/projects/non-existent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());

        verify(projectService).getProjectById(userId, "non-existent");
    }

    @Test
    void createProject_shouldCreateNewProject() throws Exception {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setAddress("789 New St");
        newProject.setStatus("planning");

        when(projectService.createProject(eq(userId), any(Project.class))).thenReturn(testProject);

        // When & Then
        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project 1"));

        verify(projectService).createProject(eq(userId), any(Project.class));
    }

    @Test
    void createProject_shouldReturnErrorForInvalidProject() throws Exception {
        // Given
        Project invalidProject = new Project();
        invalidProject.setName(""); // Empty name

        when(projectService.createProject(eq(userId), any(Project.class)))
                .thenThrow(new RuntimeException("Project validation failed"));

        // When & Then
        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProject)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project validation failed"));
    }

    @Test
    void updateProject_shouldUpdateExistingProject() throws Exception {
        // Given
        Project updateDetails = new Project();
        updateDetails.setAddress("Updated Address");
        updateDetails.setStatus("in-progress");

        Project updatedProject = new Project();
        updatedProject.setId("project-1");
        updatedProject.setName("Test Project 1");
        updatedProject.setAddress("Updated Address");
        updatedProject.setStatus("in-progress");
        updatedProject.setOwnerId(userId);

        when(projectService.updateProject(eq(userId), eq("project-1"), any(Project.class)))
                .thenReturn(updatedProject);

        // When & Then
        mockMvc.perform(put("/api/projects/project-1")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Updated Address"))
                .andExpect(jsonPath("$.status").value("in-progress"));

        verify(projectService).updateProject(eq(userId), eq("project-1"), any(Project.class));
    }

    @Test
    void updateProject_shouldReturnErrorWhenProjectNotFound() throws Exception {
        // Given
        Project updateDetails = new Project();
        updateDetails.setAddress("Updated Address");

        when(projectService.updateProject(eq(userId), eq("non-existent"), any(Project.class)))
                .thenThrow(new RuntimeException("Project not found"));

        // When & Then
        mockMvc.perform(put("/api/projects/non-existent")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void deleteProject_shouldDeleteProject() throws Exception {
        // Given
        doNothing().when(projectService).deleteProject(userId, "project-1");

        // When & Then
        mockMvc.perform(delete("/api/projects/project-1")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(projectService).deleteProject(userId, "project-1");
    }

    @Test
    void deleteProject_shouldReturnErrorWhenProjectNotFound() throws Exception {
        // Given
        doThrow(new RuntimeException("Project not found"))
                .when(projectService).deleteProject(userId, "non-existent");

        // When & Then
        mockMvc.perform(delete("/api/projects/non-existent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getProjectsByStatus_shouldReturnFilteredProjects() throws Exception {
        // Given
        List<Project> activeProjects = Arrays.asList(testProject);
        when(projectService.getProjectsByStatus(userId, "active")).thenReturn(activeProjects);

        // When & Then
        mockMvc.perform(get("/api/projects/status/active")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Project 1"))
                .andExpect(jsonPath("$[0].status").value("active"));

        verify(projectService).getProjectsByStatus(userId, "active");
    }

    @Test
    void transferDemoProject_shouldTransferProject() throws Exception {
        // Given
        when(projectService.transferDemoProject(userId, "demo-project-1")).thenReturn(testProject);

        // When & Then
        mockMvc.perform(post("/api/projects/demo/demo-project-1/transfer")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Project 1"));

        verify(projectService).transferDemoProject(userId, "demo-project-1");
    }

    @Test
    void transferDemoProject_shouldReturnErrorWhenProjectNotFound() throws Exception {
        // Given
        when(projectService.transferDemoProject(userId, "non-existent"))
                .thenThrow(new RuntimeException("Project not found"));

        // When & Then
        mockMvc.perform(post("/api/projects/demo/non-existent/transfer")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getAllProjects_shouldReturnErrorForMissingAuthorizationHeader() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid authorization header"));
    }

    @Test
    void getAllProjects_shouldReturnErrorForInvalidAuthorizationFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid authorization header"));
    }
}
