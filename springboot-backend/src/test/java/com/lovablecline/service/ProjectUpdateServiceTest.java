package com.lovablecline.service;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.ProjectUpdate;
import com.lovablecline.repository.ProjectRepository;
import com.lovablecline.repository.ProjectUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectUpdateServiceTest {

    @Mock
    private ProjectUpdateRepository projectUpdateRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectUpdateService projectUpdateService;

    private String userId;
    private String projectId;
    private String updateId;
    private Project testProject;
    private ProjectUpdate testUpdate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
        updateId = UUID.randomUUID().toString();

        testProject = new Project();
        testProject.setId(projectId);
        testProject.setOwnerId(userId);

        testUpdate = new ProjectUpdate();
        testUpdate.setId(updateId);
        testUpdate.setTitle("Test Update");
        testUpdate.setDescription("Test description");
        testUpdate.setProject(testProject);
        testUpdate.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getAllUpdatesByProjectId_WithValidRequest_ReturnsUpdates() {
        when(projectUpdateRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(List.of(testUpdate));

        List<ProjectUpdate> result = projectUpdateService.getAllUpdatesByProjectId(userId, projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(updateId, result.get(0).getId());
        verify(projectUpdateRepository).findByUserIdAndProjectId(userId, projectId);
    }

    @Test
    void getUpdateById_WithExistingUpdate_ReturnsUpdate() {
        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.of(testUpdate));

        Optional<ProjectUpdate> result = projectUpdateService.getUpdateById(userId, projectId, updateId);

        assertTrue(result.isPresent());
        assertEquals(updateId, result.get().getId());
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
    }

    @Test
    void getUpdateById_WithNonExistentUpdate_ReturnsEmpty() {
        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.empty());

        Optional<ProjectUpdate> result = projectUpdateService.getUpdateById(userId, projectId, updateId);

        assertFalse(result.isPresent());
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
    }

    @Test
    void createUpdate_WithValidProject_CreatesUpdate() {
        when(projectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(testProject));
        when(projectUpdateRepository.save(any(ProjectUpdate.class)))
                .thenReturn(testUpdate);

        ProjectUpdate newUpdate = new ProjectUpdate();
        newUpdate.setTitle("New Update");
        newUpdate.setDescription("New description");

        ProjectUpdate result = projectUpdateService.createUpdate(userId, projectId, newUpdate);

        assertNotNull(result);
        assertEquals(testProject, result.getProject());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(projectUpdateRepository).save(any(ProjectUpdate.class));
    }

    @Test
    void createUpdate_WithNonExistentProject_ThrowsException() {
        when(projectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.empty());

        ProjectUpdate newUpdate = new ProjectUpdate();
        newUpdate.setTitle("New Update");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectUpdateService.createUpdate(userId, projectId, newUpdate);
        });

        assertEquals("Project not found", exception.getMessage());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(projectUpdateRepository, never()).save(any(ProjectUpdate.class));
    }

    @Test
    void updateUpdate_WithExistingUpdate_UpdatesFields() {
        ProjectUpdate existingUpdate = new ProjectUpdate();
        existingUpdate.setId(updateId);
        existingUpdate.setTitle("Old Title");
        existingUpdate.setDescription("Old description");
        existingUpdate.setProject(testProject);

        ProjectUpdate updateDetails = new ProjectUpdate();
        updateDetails.setTitle("Updated Title");
        updateDetails.setDescription("Updated description");

        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.of(existingUpdate));
        when(projectUpdateRepository.save(any(ProjectUpdate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdate result = projectUpdateService.updateUpdate(userId, projectId, updateId, updateDetails);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(testProject, result.getProject());
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository).save(existingUpdate);
    }

    @Test
    void updateUpdate_WithPartialFields_UpdatesOnlyProvidedFields() {
        ProjectUpdate existingUpdate = new ProjectUpdate();
        existingUpdate.setId(updateId);
        existingUpdate.setTitle("Old Title");
        existingUpdate.setDescription("Old description");
        existingUpdate.setProject(testProject);

        ProjectUpdate updateDetails = new ProjectUpdate();
        updateDetails.setTitle("Updated Title");
        // Description not provided

        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.of(existingUpdate));
        when(projectUpdateRepository.save(any(ProjectUpdate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdate result = projectUpdateService.updateUpdate(userId, projectId, updateId, updateDetails);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Old description", result.getDescription()); // Should remain unchanged
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository).save(existingUpdate);
    }

    @Test
    void updateUpdate_WithNonExistentUpdate_ThrowsException() {
        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.empty());

        ProjectUpdate updateDetails = new ProjectUpdate();
        updateDetails.setTitle("Updated Title");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectUpdateService.updateUpdate(userId, projectId, updateId, updateDetails);
        });

        assertEquals("Update not found", exception.getMessage());
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository, never()).save(any(ProjectUpdate.class));
    }

    @Test
    void deleteUpdate_WithExistingUpdate_DeletesUpdate() {
        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.of(testUpdate));
        doNothing().when(projectUpdateRepository).delete(testUpdate);

        assertDoesNotThrow(() -> {
            projectUpdateService.deleteUpdate(userId, projectId, updateId);
        });

        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository).delete(testUpdate);
    }

    @Test
    void deleteUpdate_WithNonExistentUpdate_ThrowsException() {
        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectUpdateService.deleteUpdate(userId, projectId, updateId);
        });

        assertEquals("Update not found", exception.getMessage());
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository, never()).delete(any(ProjectUpdate.class));
    }

    @Test
    void getAllUpdatesByUserId_WithValidRequest_ReturnsUserUpdates() {
        when(projectUpdateRepository.findAllByUserId(userId))
                .thenReturn(List.of(testUpdate));

        List<ProjectUpdate> result = projectUpdateService.getAllUpdatesByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(updateId, result.get(0).getId());
        verify(projectUpdateRepository).findAllByUserId(userId);
    }

    @Test
    void getUpdatesByProjectIdOrdered_WithValidRequest_ReturnsOrderedUpdates() {
        when(projectUpdateRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(testUpdate));

        List<ProjectUpdate> result = projectUpdateService.getUpdatesByProjectIdOrdered(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(updateId, result.get(0).getId());
        verify(projectUpdateRepository).findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Test
    void createUpdate_WithNullFields_HandlesGracefully() {
        when(projectRepository.findByUserIdAndProjectId(userId, projectId))
                .thenReturn(Optional.of(testProject));
        when(projectUpdateRepository.save(any(ProjectUpdate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdate newUpdate = new ProjectUpdate();
        // No fields set

        ProjectUpdate result = projectUpdateService.createUpdate(userId, projectId, newUpdate);

        assertNotNull(result);
        assertEquals(testProject, result.getProject());
        verify(projectRepository).findByUserIdAndProjectId(userId, projectId);
        verify(projectUpdateRepository).save(any(ProjectUpdate.class));
    }

    @Test
    void updateUpdate_WithNullUpdateDetails_HandlesGracefully() {
        ProjectUpdate existingUpdate = new ProjectUpdate();
        existingUpdate.setId(updateId);
        existingUpdate.setTitle("Old Title");
        existingUpdate.setDescription("Old description");
        existingUpdate.setProject(testProject);

        when(projectUpdateRepository.findByIdAndProjectId(updateId, projectId))
                .thenReturn(Optional.of(existingUpdate));
        when(projectUpdateRepository.save(any(ProjectUpdate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdate updateDetails = new ProjectUpdate();
        // No fields set

        ProjectUpdate result = projectUpdateService.updateUpdate(userId, projectId, updateId, updateDetails);

        assertNotNull(result);
        assertEquals("Old Title", result.getTitle()); // Should remain unchanged
        assertEquals("Old description", result.getDescription()); // Should remain unchanged
        verify(projectUpdateRepository).findByIdAndProjectId(updateId, projectId);
        verify(projectUpdateRepository).save(existingUpdate);
    }
}
