package com.lovablecline.repository;

import com.lovablecline.entity.Project;
import com.lovablecline.entity.ProjectUpdate;
import com.lovablecline.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ProjectUpdateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectUpdateRepository projectUpdateRepository;

    private User testUser;
    private Project testProject;
    private ProjectUpdate testUpdate1;
    private ProjectUpdate testUpdate2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        
        testUser = entityManager.persist(testUser);

        // Create test project
        testProject = new Project();
        testProject.setName("Test Project");
        testProject.setAddress("123 Test St");
        testProject.setStatus("active");
        testProject.setOwner(testUser);
        testProject.setOwnerId(testUser.getId()); // Ensure ownerId is set
        testProject = entityManager.persist(testProject);

        // Create test project updates
        testUpdate1 = new ProjectUpdate();
        testUpdate1.setTitle("First Update");
        testUpdate1.setDescription("This is the first project update");
        testUpdate1.setProjectId(testProject.getId());
        testUpdate1.setProject(testProject); // Set the relationship
        testUpdate1.setDate(LocalDateTime.now().minusDays(2));
        testUpdate1.setCreatedAt(LocalDateTime.now().minusDays(2));
        testUpdate1 = entityManager.persist(testUpdate1);

        testUpdate2 = new ProjectUpdate();
        testUpdate2.setTitle("Second Update");
        testUpdate2.setDescription("This is the second project update");
        testUpdate2.setProjectId(testProject.getId());
        testUpdate2.setProject(testProject); // Set the relationship
        testUpdate2.setDate(LocalDateTime.now().minusDays(1));
        testUpdate2.setCreatedAt(LocalDateTime.now().minusDays(1));
        testUpdate2 = entityManager.persist(testUpdate2);

        entityManager.flush();
        entityManager.clear(); // Clear the persistence context to force reloading
    }

    @Test
    void findByProject_shouldReturnUpdatesForProject() {
        // When
        List<ProjectUpdate> updates = projectUpdateRepository.findByProject(testProject);

        // Then
        assertThat(updates).hasSize(2);
        assertThat(updates).extracting(ProjectUpdate::getTitle)
                .containsExactlyInAnyOrder("First Update", "Second Update");
    }

    @Test
    void findByProjectId_shouldReturnUpdatesForProjectId() {
        // When
        List<ProjectUpdate> updates = projectUpdateRepository.findByProjectId(testProject.getId());

        // Then
        assertThat(updates).hasSize(2);
        assertThat(updates).extracting(ProjectUpdate::getTitle)
                .containsExactlyInAnyOrder("First Update", "Second Update");
    }

    @Test
    void findByProjectIdOrderByCreatedAtDesc_shouldReturnUpdatesOrderedByCreatedAtDesc() {
        // When
        List<ProjectUpdate> updates = projectUpdateRepository.findByProjectIdOrderByCreatedAtDesc(testProject.getId());

        // Then
        assertThat(updates).hasSize(2);
        assertThat(updates.get(0).getTitle()).isEqualTo("Second Update"); // Newest first
        assertThat(updates.get(1).getTitle()).isEqualTo("First Update");  // Oldest last
        assertThat(updates.get(0).getCreatedAt()).isAfter(updates.get(1).getCreatedAt());
    }

    @Test
    void findByUserIdAndProjectId_shouldReturnUpdatesForUserAndProject() {
        // When
        List<ProjectUpdate> updates = projectUpdateRepository.findByUserIdAndProjectId(testUser.getId(), testProject.getId());

        // Then
        assertThat(updates).hasSize(2);
        assertThat(updates).extracting(ProjectUpdate::getTitle)
                .containsExactlyInAnyOrder("First Update", "Second Update");
    }

    @Test
    void findAllByUserId_shouldReturnAllUpdatesForUser() {
        // Create another project for the same user
        Project anotherProject = new Project();
        anotherProject.setName("Another Project");
        anotherProject.setAddress("456 Another St");
        anotherProject.setStatus("active");
        anotherProject.setOwner(testUser);
        anotherProject.setOwnerId(testUser.getId()); // Ensure ownerId is set
        anotherProject = entityManager.persist(anotherProject);

        // Create update for another project
        ProjectUpdate anotherUpdate = new ProjectUpdate();
        anotherUpdate.setTitle("Another Update");
        anotherUpdate.setDescription("Update for another project");
        anotherUpdate.setProjectId(anotherProject.getId());
        anotherUpdate.setProject(anotherProject); // Set the relationship
        anotherUpdate = entityManager.persist(anotherUpdate);

        entityManager.flush();
        entityManager.clear(); // Clear the persistence context to force reloading

        // When
        List<ProjectUpdate> updates = projectUpdateRepository.findAllByUserId(testUser.getId());

        // Then
        assertThat(updates).hasSize(3);
        assertThat(updates).extracting(ProjectUpdate::getTitle)
                .containsExactlyInAnyOrder("First Update", "Second Update", "Another Update");
    }

    @Test
    void findByIdAndProjectId_shouldReturnUpdateWhenExists() {
        // When
        Optional<ProjectUpdate> foundUpdate = projectUpdateRepository.findByIdAndProjectId(
                testUpdate1.getId(), testProject.getId());

        // Then
        assertThat(foundUpdate).isPresent();
        assertThat(foundUpdate.get().getTitle()).isEqualTo("First Update");
    }

    @Test
    void findByIdAndProjectId_shouldReturnEmptyWhenIdDoesNotMatchProject() {
        // Create another project
        Project anotherProject = new Project();
        anotherProject.setName("Another Project");
        anotherProject.setAddress("456 Another St");
        anotherProject.setStatus("active");
        anotherProject.setOwner(testUser);
        anotherProject.setOwnerId(testUser.getId()); // Ensure ownerId is set
        anotherProject = entityManager.persist(anotherProject);

        entityManager.flush();

        // When
        Optional<ProjectUpdate> foundUpdate = projectUpdateRepository.findByIdAndProjectId(
                testUpdate1.getId(), anotherProject.getId());

        // Then
        assertThat(foundUpdate).isEmpty();
    }

    @Test
    void save_shouldPersistNewProjectUpdate() {
        // Given
        ProjectUpdate newUpdate = new ProjectUpdate();
        newUpdate.setTitle("New Update");
        newUpdate.setDescription("A new project update");
        newUpdate.setProjectId(testProject.getId());

        // When
        ProjectUpdate savedUpdate = projectUpdateRepository.save(newUpdate);
        entityManager.flush();

        // Then
        assertThat(savedUpdate.getId()).isNotNull();
        assertThat(savedUpdate.getTitle()).isEqualTo("New Update");
        assertThat(projectUpdateRepository.count()).isEqualTo(3);
    }

    @Test
    void delete_shouldRemoveProjectUpdate() {
        // When
        projectUpdateRepository.delete(testUpdate1);
        entityManager.flush();

        // Then
        assertThat(projectUpdateRepository.count()).isEqualTo(1);
        assertThat(projectUpdateRepository.findById(testUpdate1.getId())).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllProjectUpdates() {
        // When
        List<ProjectUpdate> allUpdates = projectUpdateRepository.findAll();

        // Then
        assertThat(allUpdates).hasSize(2);
        assertThat(allUpdates).extracting(ProjectUpdate::getTitle)
                .containsExactlyInAnyOrder("First Update", "Second Update");
    }

    @Test
    void findById_shouldReturnUpdateWhenExists() {
        // When
        Optional<ProjectUpdate> foundUpdate = projectUpdateRepository.findById(testUpdate1.getId());

        // Then
        assertThat(foundUpdate).isPresent();
        assertThat(foundUpdate.get().getTitle()).isEqualTo("First Update");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        // When
        Optional<ProjectUpdate> foundUpdate = projectUpdateRepository.findById("non-existent-id");

        // Then
        assertThat(foundUpdate).isEmpty();
    }
}
