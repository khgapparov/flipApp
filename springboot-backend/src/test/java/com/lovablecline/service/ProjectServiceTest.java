package com.lovablecline.service;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.User;
import com.lovablecline.repository.ProjectRepository;
import com.lovablecline.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User testUser;
    private Project testProject;
    private Project testProject2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");

        testProject = new Project();
        testProject.setId("project-1");
        testProject.setName("Test Project 1");
        testProject.setAddress("123 Test St");
        testProject.setStatus("active");
        testProject.setOwner(testUser);
        testProject.setOwnerId(testUser.getId());

        testProject2 = new Project();
        testProject2.setId("project-2");
        testProject2.setName("Test Project 2");
        testProject2.setAddress("456 Test St");
        testProject2.setStatus("completed");
        testProject2.setOwner(testUser);
        testProject2.setOwnerId(testUser.getId());
    }

    @Test
    void getAllProjectsByUserId_shouldReturnUserProjects() {
        // Given
        List<Project> expectedProjects = Arrays.asList(testProject, testProject2);
        when(projectRepository.findAllByUserId(testUser.getId())).thenReturn(expectedProjects);

        // When
        List<Project> result = projectService.getAllProjectsByUserId(testUser.getId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testProject, testProject2);
        verify(projectRepository).findAllByUserId(testUser.getId());
    }

    @Test
    void getProjectById_shouldReturnProjectWhenExists() {
        // Given
        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "project-1"))
                .thenReturn(Optional.of(testProject));

        // When
        Optional<Project> result = projectService.getProjectById(testUser.getId(), "project-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProject);
        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "project-1");
    }

    @Test
    void getProjectById_shouldReturnEmptyWhenProjectNotFound() {
        // Given
        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "non-existent"))
                .thenReturn(Optional.empty());

        // When
        Optional<Project> result = projectService.getProjectById(testUser.getId(), "non-existent");

        // Then
        assertThat(result).isEmpty();
        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "non-existent");
    }

    @Test
    void createProject_shouldCreateNewProjectSuccessfully() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setAddress("789 New St");
        newProject.setStatus("planning");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(projectRepository.existsByAddressAndOwnerId("789 New St", testUser.getId())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project savedProject = invocation.getArgument(0);
            savedProject.setId("new-project-id");
            savedProject.setOwner(testUser);
            return savedProject;
        });

        // When
        Project result = projectService.createProject(testUser.getId(), newProject);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("New Project");
        assertThat(result.getAddress()).isEqualTo("789 New St");
        assertThat(result.getOwner()).isEqualTo(testUser);
        verify(userRepository).findById(testUser.getId());
        verify(projectRepository).existsByAddressAndOwnerId("789 New St", testUser.getId());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_shouldThrowExceptionWhenUserNotFound() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setAddress("789 New St");

        when(userRepository.findById("non-existent-user")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.createProject("non-existent-user", newProject))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository).findById("non-existent-user");
        verifyNoInteractions(projectRepository);
    }

    @Test
    void createProject_shouldThrowExceptionWhenAddressAlreadyExists() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setAddress("123 Test St"); // Same address as existing project

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(projectRepository.existsByAddressAndOwnerId("123 Test St", testUser.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> projectService.createProject(testUser.getId(), newProject))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project with this address already exists for this user");

        verify(userRepository).findById(testUser.getId());
        verify(projectRepository).existsByAddressAndOwnerId("123 Test St", testUser.getId());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_shouldUpdateExistingProject() {
        // Given
        Project updateDetails = new Project();
        updateDetails.setAddress("Updated Address");
        updateDetails.setStatus("in-progress");
        updateDetails.setStartDate(LocalDateTime.now());
        updateDetails.setEstimatedCompletionDate(LocalDateTime.now().plusMonths(6));

        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "project-1"))
                .thenReturn(Optional.of(testProject));
        when(projectRepository.existsByAddressAndOwnerId("Updated Address", testUser.getId())).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Project result = projectService.updateProject(testUser.getId(), "project-1", updateDetails);

        // Then
        assertThat(result.getAddress()).isEqualTo("Updated Address");
        assertThat(result.getStatus()).isEqualTo("in-progress");
        assertThat(result.getStartDate()).isEqualTo(updateDetails.getStartDate());
        assertThat(result.getEstimatedCompletionDate()).isEqualTo(updateDetails.getEstimatedCompletionDate());
        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "project-1");
        verify(projectRepository).existsByAddressAndOwnerId("Updated Address", testUser.getId());
        verify(projectRepository).save(testProject);
    }

    @Test
    void updateProject_shouldThrowExceptionWhenProjectNotFound() {
        // Given
        Project updateDetails = new Project();
        updateDetails.setAddress("Updated Address");

        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "non-existent"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.updateProject(testUser.getId(), "non-existent", updateDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");

        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "non-existent");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_shouldThrowExceptionWhenAddressAlreadyExists() {
        // Given
        Project updateDetails = new Project();
        updateDetails.setAddress("456 Test St"); // Address of testProject2

        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "project-1"))
                .thenReturn(Optional.of(testProject));
        when(projectRepository.existsByAddressAndOwnerId("456 Test St", testUser.getId())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> projectService.updateProject(testUser.getId(), "project-1", updateDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project with this address already exists for this user");

        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "project-1");
        verify(projectRepository).existsByAddressAndOwnerId("456 Test St", testUser.getId());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_shouldDeleteExistingProject() {
        // Given
        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "project-1"))
                .thenReturn(Optional.of(testProject));
        doNothing().when(projectRepository).delete(testProject);

        // When
        projectService.deleteProject(testUser.getId(), "project-1");

        // Then
        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "project-1");
        verify(projectRepository).delete(testProject);
    }

    @Test
    void deleteProject_shouldThrowExceptionWhenProjectNotFound() {
        // Given
        when(projectRepository.findByUserIdAndProjectId(testUser.getId(), "non-existent"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.deleteProject(testUser.getId(), "non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");

        verify(projectRepository).findByUserIdAndProjectId(testUser.getId(), "non-existent");
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void getProjectsByStatus_shouldReturnFilteredProjects() {
        // Given
        List<Project> userProjects = Arrays.asList(testProject, testProject2);
        when(projectRepository.findAllByUserId(testUser.getId())).thenReturn(userProjects);

        // When
        List<Project> result = projectService.getProjectsByStatus(testUser.getId(), "active");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testProject);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
        verify(projectRepository).findAllByUserId(testUser.getId());
    }

    @Test
    void getProjectsByStatus_shouldReturnEmptyListWhenNoMatchingStatus() {
        // Given
        List<Project> userProjects = Arrays.asList(testProject, testProject2);
        when(projectRepository.findAllByUserId(testUser.getId())).thenReturn(userProjects);

        // When
        List<Project> result = projectService.getProjectsByStatus(testUser.getId(), "cancelled");

        // Then
        assertThat(result).isEmpty();
        verify(projectRepository).findAllByUserId(testUser.getId());
    }

    @Test
    void transferDemoProject_shouldTransferProjectSuccessfully() {
        // Given
        Project demoProject = new Project();
        demoProject.setId("demo-project-1");
        demoProject.setName("Demo Project");
        demoProject.setAddress("Demo Address");
        demoProject.setStatus("demo");

        when(projectRepository.findById("demo-project-1")).thenReturn(Optional.of(demoProject));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Project result = projectService.transferDemoProject(testUser.getId(), "demo-project-1");

        // Then
        assertThat(result.getOwnerId()).isEqualTo(testUser.getId());
        assertThat(result.getOwner()).isEqualTo(testUser);
        verify(projectRepository).findById("demo-project-1");
        verify(userRepository).findById(testUser.getId());
        verify(projectRepository).save(demoProject);
    }

    @Test
    void transferDemoProject_shouldThrowExceptionWhenProjectNotFound() {
        // Given
        when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.transferDemoProject(testUser.getId(), "non-existent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");

        verify(projectRepository).findById("non-existent");
        verify(userRepository, never()).findById(any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void transferDemoProject_shouldThrowExceptionWhenUserNotFound() {
        // Given
        Project demoProject = new Project();
        demoProject.setId("demo-project-1");

        when(projectRepository.findById("demo-project-1")).thenReturn(Optional.of(demoProject));
        when(userRepository.findById("non-existent-user")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.transferDemoProject("non-existent-user", "demo-project-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(projectRepository).findById("demo-project-1");
        verify(userRepository).findById("non-existent-user");
        verify(projectRepository, never()).save(any());
    }
}
