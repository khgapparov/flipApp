package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.entity.Project;
import com.lovablecline.entity.ProjectUpdate;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.ProjectUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectUpdateService projectUpdateService;

    @MockBean
    private AuthenticationService authenticationService;

    private String userId;
    private String projectId;
    private String updateId;
    private String validToken;
    private ProjectUpdate testUpdate;
    private Project testProject;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
        updateId = UUID.randomUUID().toString();
        validToken = "valid-jwt-token";

        testProject = new Project();
        testProject.setId(projectId);
        testProject.setOwnerId(userId);

        testUpdate = new ProjectUpdate();
        testUpdate.setId(updateId);
        testUpdate.setTitle("Test Update");
        testUpdate.setDescription("Test description");
        testUpdate.setProject(testProject);
        testUpdate.setCreatedAt(LocalDateTime.now());

        when(authenticationService.getUserIdFromToken(validToken)).thenReturn(userId);
    }

    @Test
    void getAllUpdates_WithValidRequest_ReturnsUpdates() throws Exception {
        when(projectUpdateService.getAllUpdatesByProjectId(eq(userId), eq(projectId)))
                .thenReturn(List.of(testUpdate));

        mockMvc.perform(get("/api/projects/{projectId}/updates", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(updateId))
                .andExpect(jsonPath("$[0].title").value("Test Update"));

        verify(projectUpdateService).getAllUpdatesByProjectId(eq(userId), eq(projectId));
    }

    @Test
    void getAllUpdates_WithServiceException_ReturnsBadRequest() throws Exception {
        when(projectUpdateService.getAllUpdatesByProjectId(eq(userId), eq(projectId)))
                .thenThrow(new RuntimeException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}/updates", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getAllUpdates_WithNoAuthHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/updates", projectId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUpdate_WithValidRequest_ReturnsUpdate() throws Exception {
        when(projectUpdateService.getUpdateById(eq(userId), eq(projectId), eq(updateId)))
                .thenReturn(Optional.of(testUpdate));

        mockMvc.perform(get("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updateId))
                .andExpect(jsonPath("$.title").value("Test Update"));

        verify(projectUpdateService).getUpdateById(eq(userId), eq(projectId), eq(updateId));
    }

    @Test
    void getUpdate_WithNonExistentUpdate_ReturnsNotFound() throws Exception {
        when(projectUpdateService.getUpdateById(eq(userId), eq(projectId), eq(updateId)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUpdate_WithValidRequest_CreatesUpdate() throws Exception {
        when(projectUpdateService.createUpdate(eq(userId), eq(projectId), any(ProjectUpdate.class)))
                .thenReturn(testUpdate);

        mockMvc.perform(post("/api/projects/{projectId}/updates", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updateId))
                .andExpect(jsonPath("$.title").value("Test Update"));

        verify(projectUpdateService).createUpdate(eq(userId), eq(projectId), any(ProjectUpdate.class));
    }

    @Test
    void createUpdate_WithServiceException_ReturnsBadRequest() throws Exception {
        when(projectUpdateService.createUpdate(eq(userId), eq(projectId), any(ProjectUpdate.class)))
                .thenThrow(new RuntimeException("Project not found"));

        mockMvc.perform(post("/api/projects/{projectId}/updates", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void updateUpdate_WithValidRequest_UpdatesUpdate() throws Exception {
        ProjectUpdate updatedUpdate = new ProjectUpdate();
        updatedUpdate.setId(updateId);
        updatedUpdate.setTitle("Updated Title");
        updatedUpdate.setDescription("Updated description");

        when(projectUpdateService.updateUpdate(eq(userId), eq(projectId), eq(updateId), any(ProjectUpdate.class)))
                .thenReturn(updatedUpdate);

        mockMvc.perform(put("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        verify(projectUpdateService).updateUpdate(eq(userId), eq(projectId), eq(updateId), any(ProjectUpdate.class));
    }

    @Test
    void updateUpdate_WithServiceException_ReturnsBadRequest() throws Exception {
        when(projectUpdateService.updateUpdate(eq(userId), eq(projectId), eq(updateId), any(ProjectUpdate.class)))
                .thenThrow(new RuntimeException("Update not found"));

        mockMvc.perform(put("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Update not found"));
    }

    @Test
    void deleteUpdate_WithValidRequest_DeletesUpdate() throws Exception {
        doNothing().when(projectUpdateService).deleteUpdate(eq(userId), eq(projectId), eq(updateId));

        mockMvc.perform(delete("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(projectUpdateService).deleteUpdate(eq(userId), eq(projectId), eq(updateId));
    }

    @Test
    void deleteUpdate_WithServiceException_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException("Update not found"))
                .when(projectUpdateService).deleteUpdate(eq(userId), eq(projectId), eq(updateId));

        mockMvc.perform(delete("/api/projects/{projectId}/updates/{id}", projectId, updateId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Update not found"));
    }

    @Test
    void getAllUpdatesByUser_WithValidRequest_ReturnsUserUpdates() throws Exception {
        when(projectUpdateService.getAllUpdatesByUserId(eq(userId)))
                .thenReturn(List.of(testUpdate));

        mockMvc.perform(get("/api/projects/{projectId}/updates/user/all", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(updateId));

        verify(projectUpdateService).getAllUpdatesByUserId(eq(userId));
    }

    @Test
    void getAllUpdatesByUser_WithServiceException_ReturnsBadRequest() throws Exception {
        when(projectUpdateService.getAllUpdatesByUserId(eq(userId)))
                .thenThrow(new RuntimeException("Error fetching updates"));

        mockMvc.perform(get("/api/projects/{projectId}/updates/user/all", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error fetching updates"));
    }

    @Test
    void getUpdatesOrdered_WithValidRequest_ReturnsOrderedUpdates() throws Exception {
        when(projectUpdateService.getUpdatesByProjectIdOrdered(eq(projectId)))
                .thenReturn(List.of(testUpdate));

        mockMvc.perform(get("/api/projects/{projectId}/updates/ordered", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(updateId));

        verify(projectUpdateService).getUpdatesByProjectIdOrdered(eq(projectId));
    }

    @Test
    void getUpdatesOrdered_WithServiceException_ReturnsBadRequest() throws Exception {
        when(projectUpdateService.getUpdatesByProjectIdOrdered(eq(projectId)))
                .thenThrow(new RuntimeException("Error fetching ordered updates"));

        mockMvc.perform(get("/api/projects/{projectId}/updates/ordered", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error fetching ordered updates"));
    }

    @Test
    void extractToken_WithInvalidHeader_ThrowsException() throws Exception {
        when(authenticationService.getUserIdFromToken(anyString()))
                .thenThrow(new RuntimeException("Invalid authorization header"));

        mockMvc.perform(get("/api/projects/{projectId}/updates", projectId)
                .header("Authorization", "Invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid authorization header"));
    }
}
