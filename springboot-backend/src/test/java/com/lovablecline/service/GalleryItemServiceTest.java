package com.lovablecline.service;

import com.lovablecline.entity.GalleryItem;
import com.lovablecline.entity.Project;
import com.lovablecline.repository.GalleryItemRepository;
import com.lovablecline.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GalleryItemServiceTest {

    @Mock
    private GalleryItemRepository galleryItemRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private GalleryItemService galleryItemService;

    private String userId;
    private String projectId;
    private String itemId;
    private Project project;
    private GalleryItem galleryItem;
    private GalleryItem galleryItem2;

    @BeforeEach
    void setUp() {
        userId = "user123";
        projectId = "project456";
        itemId = "gallery123";

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");

        galleryItem = new GalleryItem();
        galleryItem.setId(itemId);
        galleryItem.setTitle("Test Gallery Item");
        galleryItem.setDescription("Test Description");
        galleryItem.setImageUrl("/uploads/test-image.jpg");
        galleryItem.setRoom("Living Room");
        galleryItem.setStage("Planning");
        galleryItem.setProject(project);
        galleryItem.setCreatedAt(LocalDateTime.now());

        galleryItem2 = new GalleryItem();
        galleryItem2.setId("gallery456");
        galleryItem2.setTitle("Another Gallery Item");
        galleryItem2.setDescription("Another Description");
        galleryItem2.setImageUrl("/uploads/another-image.jpg");
        galleryItem2.setRoom("Kitchen");
        galleryItem2.setStage("Completed");
        galleryItem2.setProject(project);
        galleryItem2.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void getAllGalleryItemsByProjectId_ValidRequest_ReturnsGalleryItems() {
        List<GalleryItem> expectedItems = Arrays.asList(galleryItem, galleryItem2);
        when(galleryItemRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(expectedItems);

        List<GalleryItem> result = galleryItemService.getAllGalleryItemsByProjectId(userId, projectId);

        assertEquals(2, result.size());
        assertEquals("Test Gallery Item", result.get(0).getTitle());
        assertEquals("Another Gallery Item", result.get(1).getTitle());
        verify(galleryItemRepository).findByUserIdAndProjectId(userId, projectId);
    }

    @Test
    void getAllGalleryItemsByProjectId_NoItemsFound_ReturnsEmptyList() {
        when(galleryItemRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(List.of());

        List<GalleryItem> result = galleryItemService.getAllGalleryItemsByProjectId(userId, projectId);

        assertTrue(result.isEmpty());
        verify(galleryItemRepository).findByUserIdAndProjectId(userId, projectId);
    }

    @Test
    void getGalleryItemById_ValidRequest_ReturnsGalleryItem() {
        when(galleryItemRepository.findByIdAndProjectId(itemId, projectId)).thenReturn(Optional.of(galleryItem));

        Optional<GalleryItem> result = galleryItemService.getGalleryItemById(userId, projectId, itemId);

        assertTrue(result.isPresent());
        assertEquals("Test Gallery Item", result.get().getTitle());
        verify(galleryItemRepository).findByIdAndProjectId(itemId, projectId);
    }

    @Test
    void getGalleryItemById_ItemNotFound_ReturnsEmpty() {
        when(galleryItemRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        Optional<GalleryItem> result = galleryItemService.getGalleryItemById(userId, projectId, "nonexistent");

        assertFalse(result.isPresent());
        verify(galleryItemRepository).findByIdAndProjectId("nonexistent", projectId);
    }

    @Test
    void createGalleryItem_ValidRequest_ReturnsCreatedItem() {
        GalleryItem newItem = new GalleryItem();
        newItem.setTitle("New Gallery Item");
        newItem.setDescription("New Description");
        newItem.setRoom("Kitchen");
        newItem.setStage("Completed");

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(project));
        when(galleryItemRepository.save(any(GalleryItem.class))).thenAnswer(invocation -> {
            GalleryItem savedItem = invocation.getArgument(0);
            savedItem.setId("new-gallery-id");
            return savedItem;
        });

        GalleryItem result = galleryItemService.createGalleryItem(userId, projectId, newItem);

        assertNotNull(result.getId());
        assertEquals("New Gallery Item", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals("Kitchen", result.getRoom());
        assertEquals("Completed", result.getStage());
        assertEquals(project, result.getProject());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(galleryItemRepository).save(any(GalleryItem.class));
    }

    @Test
    void createGalleryItem_ProjectNotFound_ThrowsException() {
        GalleryItem newItem = new GalleryItem();
        newItem.setTitle("New Gallery Item");

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            galleryItemService.createGalleryItem(userId, projectId, newItem);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(galleryItemRepository, never()).save(any());
    }

    @Test
    void updateGalleryItem_ValidRequest_ReturnsUpdatedItem() {
        GalleryItem updateDetails = new GalleryItem();
        updateDetails.setTitle("Updated Title");
        updateDetails.setDescription("Updated Description");
        updateDetails.setRoom("Updated Room");
        updateDetails.setStage("Updated Stage");
        updateDetails.setImageUrl("/uploads/updated-image.jpg");

        when(galleryItemRepository.findByIdAndProjectId(itemId, projectId)).thenReturn(Optional.of(galleryItem));
        when(galleryItemRepository.save(any(GalleryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GalleryItem result = galleryItemService.updateGalleryItem(userId, projectId, itemId, updateDetails);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals("Updated Room", result.getRoom());
        assertEquals("Updated Stage", result.getStage());
        assertEquals("/uploads/updated-image.jpg", result.getImageUrl());
        verify(galleryItemRepository).findByIdAndProjectId(itemId, projectId);
        verify(galleryItemRepository).save(galleryItem);
    }

    @Test
    void updateGalleryItem_PartialUpdate_ReturnsUpdatedItem() {
        GalleryItem updateDetails = new GalleryItem();
        updateDetails.setTitle("Updated Title Only");

        when(galleryItemRepository.findByIdAndProjectId(itemId, projectId)).thenReturn(Optional.of(galleryItem));
        when(galleryItemRepository.save(any(GalleryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GalleryItem result = galleryItemService.updateGalleryItem(userId, projectId, itemId, updateDetails);

        assertEquals("Updated Title Only", result.getTitle());
        assertEquals("Test Description", result.getDescription()); // Should remain unchanged
        assertEquals("Living Room", result.getRoom()); // Should remain unchanged
        assertEquals("Planning", result.getStage()); // Should remain unchanged
        verify(galleryItemRepository).findByIdAndProjectId(itemId, projectId);
        verify(galleryItemRepository).save(galleryItem);
    }

    @Test
    void updateGalleryItem_ItemNotFound_ThrowsException() {
        GalleryItem updateDetails = new GalleryItem();
        updateDetails.setTitle("Updated Title");

        when(galleryItemRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            galleryItemService.updateGalleryItem(userId, projectId, "nonexistent", updateDetails);
        });

        assertEquals("Gallery item not found", exception.getMessage());
        verify(galleryItemRepository).findByIdAndProjectId("nonexistent", projectId);
        verify(galleryItemRepository, never()).save(any());
    }

    @Test
    void deleteGalleryItem_ValidRequest_DeletesItem() {
        when(galleryItemRepository.findByIdAndProjectId(itemId, projectId)).thenReturn(Optional.of(galleryItem));

        galleryItemService.deleteGalleryItem(userId, projectId, itemId);

        verify(galleryItemRepository).findByIdAndProjectId(itemId, projectId);
        verify(galleryItemRepository).delete(galleryItem);
    }

    @Test
    void deleteGalleryItem_ItemNotFound_ThrowsException() {
        when(galleryItemRepository.findByIdAndProjectId("nonexistent", projectId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            galleryItemService.deleteGalleryItem(userId, projectId, "nonexistent");
        });

        assertEquals("Gallery item not found", exception.getMessage());
        verify(galleryItemRepository).findByIdAndProjectId("nonexistent", projectId);
        verify(galleryItemRepository, never()).delete(any());
    }

    @Test
    void getAllGalleryItemsByUserId_ValidRequest_ReturnsGalleryItems() {
        List<GalleryItem> expectedItems = Arrays.asList(galleryItem, galleryItem2);
        when(galleryItemRepository.findAllByUserId(userId)).thenReturn(expectedItems);

        List<GalleryItem> result = galleryItemService.getAllGalleryItemsByUserId(userId);

        assertEquals(2, result.size());
        assertEquals("Test Gallery Item", result.get(0).getTitle());
        assertEquals("Another Gallery Item", result.get(1).getTitle());
        verify(galleryItemRepository).findAllByUserId(userId);
    }

    @Test
    void getAllGalleryItemsByUserId_NoItemsFound_ReturnsEmptyList() {
        when(galleryItemRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<GalleryItem> result = galleryItemService.getAllGalleryItemsByUserId(userId);

        assertTrue(result.isEmpty());
        verify(galleryItemRepository).findAllByUserId(userId);
    }

    @Test
    void getGalleryItemsByProjectIdOrdered_ValidRequest_ReturnsOrderedItems() {
        List<GalleryItem> expectedItems = Arrays.asList(galleryItem, galleryItem2);
        when(galleryItemRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(expectedItems);

        List<GalleryItem> result = galleryItemService.getGalleryItemsByProjectIdOrdered(projectId);

        assertEquals(2, result.size());
        verify(galleryItemRepository).findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Test
    void getGalleryItemsByRoom_ValidRequest_ReturnsRoomItems() {
        List<GalleryItem> expectedItems = Arrays.asList(galleryItem);
        when(galleryItemRepository.findByProjectIdAndRoom(projectId, "Living Room")).thenReturn(expectedItems);

        List<GalleryItem> result = galleryItemService.getGalleryItemsByRoom(projectId, "Living Room");

        assertEquals(1, result.size());
        assertEquals("Living Room", result.get(0).getRoom());
        verify(galleryItemRepository).findByProjectIdAndRoom(projectId, "Living Room");
    }

    @Test
    void getGalleryItemsByStage_ValidRequest_ReturnsStageItems() {
        List<GalleryItem> expectedItems = Arrays.asList(galleryItem);
        when(galleryItemRepository.findByProjectIdAndStage(projectId, "Planning")).thenReturn(expectedItems);

        List<GalleryItem> result = galleryItemService.getGalleryItemsByStage(projectId, "Planning");

        assertEquals(1, result.size());
        assertEquals("Planning", result.get(0).getStage());
        verify(galleryItemRepository).findByProjectIdAndStage(projectId, "Planning");
    }

    @Test
    void getGalleryItemsByRoom_NoItemsFound_ReturnsEmptyList() {
        when(galleryItemRepository.findByProjectIdAndRoom(projectId, "Bedroom")).thenReturn(List.of());

        List<GalleryItem> result = galleryItemService.getGalleryItemsByRoom(projectId, "Bedroom");

        assertTrue(result.isEmpty());
        verify(galleryItemRepository).findByProjectIdAndRoom(projectId, "Bedroom");
    }

    @Test
    void getGalleryItemsByStage_NoItemsFound_ReturnsEmptyList() {
        when(galleryItemRepository.findByProjectIdAndStage(projectId, "Design")).thenReturn(List.of());

        List<GalleryItem> result = galleryItemService.getGalleryItemsByStage(projectId, "Design");

        assertTrue(result.isEmpty());
        verify(galleryItemRepository).findByProjectIdAndStage(projectId, "Design");
    }

    @Test
    void createGalleryItem_NullFields_HandlesGracefully() {
        GalleryItem newItem = new GalleryItem();
        newItem.setTitle("Test Item");
        // Other fields are null

        when(projectRepository.findByUserIdAndProjectId(userId, projectId)).thenReturn(Optional.of(project));
        when(galleryItemRepository.save(any(GalleryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GalleryItem result = galleryItemService.createGalleryItem(userId, projectId, newItem);

        assertNotNull(result);
        assertEquals("Test Item", result.getTitle());
        assertNull(result.getDescription());
        assertNull(result.getRoom());
        assertNull(result.getStage());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(galleryItemRepository).save(any(GalleryItem.class));
    }
}
