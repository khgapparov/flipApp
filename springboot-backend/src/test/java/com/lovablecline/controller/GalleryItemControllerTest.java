package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.entity.GalleryItem;
import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import com.lovablecline.service.AuthenticationService;
import com.lovablecline.service.GalleryItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GalleryItemController.class)
public class GalleryItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GalleryItemService galleryItemService;

    @MockBean
    private AuthenticationService authenticationService;

    private String validToken;
    private String userId;
    private String projectId;
    private GalleryItem galleryItem;
    private Project project;

    @BeforeEach
    void setUp() {
        validToken = "valid-jwt-token";
        userId = "user123";
        projectId = "project456";
        
        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        
        galleryItem = new GalleryItem();
        galleryItem.setId("gallery123");
        galleryItem.setTitle("Test Gallery Item");
        galleryItem.setDescription("Test Description");
        galleryItem.setImageUrl("/uploads/test-image.jpg");
        galleryItem.setRoom("Living Room");
        galleryItem.setStage("Planning");
        galleryItem.setProject(project);
        galleryItem.setCreatedAt(LocalDateTime.now());

        when(authenticationService.getUserIdFromToken(validToken)).thenReturn(userId);
        when(authenticationService.getUserIdFromToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));
    }

    @Test
    void getAllGalleryItems_ValidRequest_ReturnsGalleryItems() throws Exception {
        List<GalleryItem> galleryItems = Arrays.asList(galleryItem);
        when(galleryItemService.getAllGalleryItemsByProjectId(userId, projectId)).thenReturn(galleryItems);

        mockMvc.perform(get("/api/projects/{projectId}/gallery", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gallery123"))
                .andExpect(jsonPath("$[0].title").value("Test Gallery Item"));
    }

    @Test
    void getAllGalleryItems_InvalidToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/gallery", projectId)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    void getAllGalleryItems_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        when(galleryItemService.getAllGalleryItemsByProjectId(userId, projectId))
                .thenThrow(new RuntimeException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}/gallery", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getGalleryItem_ValidRequest_ReturnsGalleryItem() throws Exception {
        when(galleryItemService.getGalleryItemById(userId, projectId, "gallery123"))
                .thenReturn(Optional.of(galleryItem));

        mockMvc.perform(get("/api/projects/{projectId}/gallery/{itemId}", projectId, "gallery123")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gallery123"))
                .andExpect(jsonPath("$.title").value("Test Gallery Item"));
    }

    @Test
    void getGalleryItem_ItemNotFound_ReturnsNotFound() throws Exception {
        when(galleryItemService.getGalleryItemById(userId, projectId, "nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/{projectId}/gallery/{itemId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGalleryItem_ValidRequest_ReturnsCreatedItem() throws Exception {
        GalleryItem newItem = new GalleryItem();
        newItem.setTitle("New Gallery Item");
        newItem.setDescription("New Description");
        newItem.setRoom("Kitchen");
        newItem.setStage("Completed");

        when(galleryItemService.createGalleryItem(eq(userId), eq(projectId), any(GalleryItem.class)))
                .thenReturn(galleryItem);

        mockMvc.perform(post("/api/projects/{projectId}/gallery", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gallery123"));
    }

    @Test
    void createGalleryItem_InvalidData_ReturnsBadRequest() throws Exception {
        GalleryItem invalidItem = new GalleryItem(); // Missing required fields

        mockMvc.perform(post("/api/projects/{projectId}/gallery", projectId)
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadGalleryImage_ValidRequest_ReturnsCreatedItem() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test-image.jpg", 
                "image/jpeg", 
                "test image content".getBytes()
        );

        when(galleryItemService.createGalleryItem(eq(userId), eq(projectId), any(GalleryItem.class)))
                .thenReturn(galleryItem);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/projects/{projectId}/gallery/upload", projectId)
                .file(file)
                .param("title", "Test Image")
                .param("description", "Test Description")
                .param("room", "Living Room")
                .param("stage", "Planning")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gallery123"));
    }

    @Test
    void uploadGalleryImage_MissingFile_ReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/projects/{projectId}/gallery/upload", projectId)
                .param("title", "Test Image")
                .param("description", "Test Description")
                .param("room", "Living Room")
                .param("stage", "Planning")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGalleryItem_ValidRequest_ReturnsUpdatedItem() throws Exception {
        GalleryItem updateDetails = new GalleryItem();
        updateDetails.setTitle("Updated Title");
        updateDetails.setDescription("Updated Description");

        when(galleryItemService.updateGalleryItem(eq(userId), eq(projectId), eq("gallery123"), any(GalleryItem.class)))
                .thenReturn(galleryItem);

        mockMvc.perform(put("/api/projects/{projectId}/gallery/{itemId}", projectId, "gallery123")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gallery123"));
    }

    @Test
    void updateGalleryItem_ItemNotFound_ReturnsBadRequest() throws Exception {
        GalleryItem updateDetails = new GalleryItem();
        updateDetails.setTitle("Updated Title");

        when(galleryItemService.updateGalleryItem(eq(userId), eq(projectId), eq("nonexistent"), any(GalleryItem.class)))
                .thenThrow(new RuntimeException("Gallery item not found"));

        mockMvc.perform(put("/api/projects/{projectId}/gallery/{itemId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Gallery item not found"));
    }

    @Test
    void deleteGalleryItem_ValidRequest_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/projects/{projectId}/gallery/{itemId}", projectId, "gallery123")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGalleryItem_ItemNotFound_ReturnsBadRequest() throws Exception {
        Mockito.doThrow(new RuntimeException("Gallery item not found"))
                .when(galleryItemService).deleteGalleryItem(userId, projectId, "nonexistent");

        mockMvc.perform(delete("/api/projects/{projectId}/gallery/{itemId}", projectId, "nonexistent")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Gallery item not found"));
    }

    @Test
    void getAllGalleryItemsByUser_ValidRequest_ReturnsGalleryItems() throws Exception {
        List<GalleryItem> galleryItems = Arrays.asList(galleryItem);
        when(galleryItemService.getAllGalleryItemsByUserId(userId)).thenReturn(galleryItems);

        mockMvc.perform(get("/api/projects/{projectId}/gallery/user/all", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gallery123"));
    }

    @Test
    void getGalleryItemsOrdered_ValidRequest_ReturnsOrderedItems() throws Exception {
        List<GalleryItem> galleryItems = Arrays.asList(galleryItem);
        when(galleryItemService.getGalleryItemsByProjectIdOrdered(projectId)).thenReturn(galleryItems);

        mockMvc.perform(get("/api/projects/{projectId}/gallery/ordered", projectId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gallery123"));
    }

    @Test
    void getGalleryItemsByRoom_ValidRequest_ReturnsRoomItems() throws Exception {
        List<GalleryItem> galleryItems = Arrays.asList(galleryItem);
        when(galleryItemService.getGalleryItemsByRoom(projectId, "Living Room")).thenReturn(galleryItems);

        mockMvc.perform(get("/api/projects/{projectId}/gallery/room/{room}", projectId, "Living Room")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gallery123"));
    }

    @Test
    void getGalleryItemsByStage_ValidRequest_ReturnsStageItems() throws Exception {
        List<GalleryItem> galleryItems = Arrays.asList(galleryItem);
        when(galleryItemService.getGalleryItemsByStage(projectId, "Planning")).thenReturn(galleryItems);

        mockMvc.perform(get("/api/projects/{projectId}/gallery/stage/{stage}", projectId, "Planning")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gallery123"));
    }

    @Test
    void getGalleryItemsByRoom_NoItemsFound_ReturnsEmptyList() throws Exception {
        when(galleryItemService.getGalleryItemsByRoom(projectId, "Bedroom")).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/{projectId}/gallery/room/{room}", projectId, "Bedroom")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getGalleryItemsByStage_NoItemsFound_ReturnsEmptyList() throws Exception {
        when(galleryItemService.getGalleryItemsByStage(projectId, "Design")).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/{projectId}/gallery/stage/{stage}", projectId, "Design")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
