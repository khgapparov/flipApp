package com.lovablecline.repository;

import com.lovablecline.entity.Project;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private Project project1;
    private Project project2;
    private Project project3;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create owner user
        owner = new User("projectowner", "owner@example.com", "password123");
        owner.setIsActive(true);
        owner.setIsAnonymous(false);
        entityManager.persist(owner);

        // Create another user for testing
        User otherUser = new User("otheruser", "other@example.com", "password456");
        otherUser.setIsActive(true);
        otherUser.setIsAnonymous(false);
        entityManager.persist(otherUser);

        // Create test projects
        project1 = new Project("House Renovation", "Renovation", "123 Main St", 
                              LocalDateTime.now().minusMonths(1), 
                              LocalDateTime.now().plusMonths(2), 
                              "on track", owner.getId());
        entityManager.persist(project1);

        project2 = new Project("Apartment Flip", "Planning", "456 Oak Ave", 
                              LocalDateTime.now(), 
                              LocalDateTime.now().plusMonths(3), 
                              "ahead", owner.getId());
        entityManager.persist(project2);

        project3 = new Project("Office Building", "Completed", "789 Pine Rd", 
                              LocalDateTime.now().minusMonths(6), 
                              LocalDateTime.now().minusMonths(1), 
                              "on track", otherUser.getId());
        entityManager.persist(project3);

        entityManager.flush();
    }

    @Test
    void findByOwner_WhenOwnerHasProjects_ReturnsProjects() {
        // When
        List<Project> ownerProjects = projectRepository.findByOwner(owner);

        // Then
        assertEquals(2, ownerProjects.size());
        assertTrue(ownerProjects.stream().allMatch(p -> p.getOwnerId().equals(owner.getId())));
    }

    @Test
    void findByOwner_WhenOwnerHasNoProjects_ReturnsEmptyList() {
        // Given - create a user with no projects
        User newUser = new User("noprojects", "noprojects@example.com", "password");
        newUser.setIsActive(true);
        entityManager.persist(newUser);
        entityManager.flush();

        // When
        List<Project> projects = projectRepository.findByOwner(newUser);

        // Then
        assertTrue(projects.isEmpty());
    }

    @Test
    void findByStatus_WhenProjectsExist_ReturnsProjects() {
        // When
        List<Project> renovationProjects = projectRepository.findByStatus("Renovation");

        // Then
        assertEquals(1, renovationProjects.size());
        assertEquals("House Renovation", renovationProjects.get(0).getName());
        assertEquals("Renovation", renovationProjects.get(0).getStatus());
    }

    @Test
    void findByStatus_WhenNoProjectsWithStatus_ReturnsEmptyList() {
        // When
        List<Project> projects = projectRepository.findByStatus("NonExistentStatus");

        // Then
        assertTrue(projects.isEmpty());
    }

    @Test
    void findByAddress_WhenProjectExists_ReturnsProject() {
        // When
        Optional<Project> foundProject = projectRepository.findByAddress("123 Main St");

        // Then
        assertTrue(foundProject.isPresent());
        assertEquals("House Renovation", foundProject.get().getName());
        assertEquals("123 Main St", foundProject.get().getAddress());
    }

    @Test
    void findByAddress_WhenProjectDoesNotExist_ReturnsEmpty() {
        // When
        Optional<Project> foundProject = projectRepository.findByAddress("Nonexistent Address");

        // Then
        assertFalse(foundProject.isPresent());
    }

    @Test
    void findByUserIdAndProjectId_WhenProjectExists_ReturnsProject() {
        // When
        Optional<Project> foundProject = projectRepository.findByUserIdAndProjectId(owner.getId(), project1.getId());

        // Then
        assertTrue(foundProject.isPresent());
        assertEquals(project1.getId(), foundProject.get().getId());
        assertEquals(owner.getId(), foundProject.get().getOwnerId());
    }

    @Test
    void findByUserIdAndProjectId_WhenProjectDoesNotBelongToUser_ReturnsEmpty() {
        // When - try to find project3 (belongs to other user) with owner's ID
        Optional<Project> foundProject = projectRepository.findByUserIdAndProjectId(owner.getId(), project3.getId());

        // Then
        assertFalse(foundProject.isPresent());
    }

    @Test
    void findByUserIdAndProjectId_WhenProjectDoesNotExist_ReturnsEmpty() {
        // When
        Optional<Project> foundProject = projectRepository.findByUserIdAndProjectId(owner.getId(), "nonexistent-id");

        // Then
        assertFalse(foundProject.isPresent());
    }

    @Test
    void findAllByUserId_WhenUserHasProjects_ReturnsProjects() {
        // When
        List<Project> userProjects = projectRepository.findAllByUserId(owner.getId());

        // Then
        assertEquals(2, userProjects.size());
        assertTrue(userProjects.stream().allMatch(p -> p.getOwnerId().equals(owner.getId())));
    }

    @Test
    void findAllByUserId_WhenUserHasNoProjects_ReturnsEmptyList() {
        // Given - create a user with no projects
        User newUser = new User("noprojects2", "noprojects2@example.com", "password");
        newUser.setIsActive(true);
        entityManager.persist(newUser);
        entityManager.flush();

        // When
        List<Project> projects = projectRepository.findAllByUserId(newUser.getId());

        // Then
        assertTrue(projects.isEmpty());
    }

    @Test
    void existsByAddressAndOwnerId_WhenProjectExists_ReturnsTrue() {
        // When
        boolean exists = projectRepository.existsByAddressAndOwnerId("123 Main St", owner.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByAddressAndOwnerId_WhenProjectDoesNotExist_ReturnsFalse() {
        // When
        boolean exists = projectRepository.existsByAddressAndOwnerId("Nonexistent Address", owner.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void existsByAddressAndOwnerId_WhenAddressExistsButDifferentOwner_ReturnsFalse() {
        // When - check if owner has project at project3's address (which belongs to other user)
        boolean exists = projectRepository.existsByAddressAndOwnerId("789 Pine Rd", owner.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void save_NewProject_SavesSuccessfully() {
        // Given
        Project newProject = new Project("New Project", "Planning", "999 New St", 
                                       LocalDateTime.now(), 
                                       LocalDateTime.now().plusMonths(6), 
                                       "on track", owner.getId());

        // When
        Project savedProject = projectRepository.save(newProject);

        // Then
        assertNotNull(savedProject.getId());
        assertEquals("New Project", savedProject.getName());
        assertEquals("999 New St", savedProject.getAddress());
        assertEquals(owner.getId(), savedProject.getOwnerId());
        assertNotNull(savedProject.getCreatedAt());
    }

    @Test
    void delete_ExistingProject_RemovesProject() {
        // Given
        String projectId = project1.getId();

        // When
        projectRepository.delete(project1);
        entityManager.flush();

        // Then
        Optional<Project> deletedProject = projectRepository.findById(projectId);
        assertFalse(deletedProject.isPresent());
    }

    @Test
    void findAll_ReturnsAllProjects() {
        // When
        List<Project> allProjects = projectRepository.findAll();

        // Then
        assertEquals(3, allProjects.size());
    }

    @Test
    void findById_WhenProjectExists_ReturnsProject() {
        // When
        Optional<Project> foundProject = projectRepository.findById(project1.getId());

        // Then
        assertTrue(foundProject.isPresent());
        assertEquals(project1.getId(), foundProject.get().getId());
        assertEquals("House Renovation", foundProject.get().getName());
    }

    @Test
    void findById_WhenProjectDoesNotExist_ReturnsEmpty() {
        // When
        Optional<Project> foundProject = projectRepository.findById("nonexistent-id");

        // Then
        assertFalse(foundProject.isPresent());
    }
}
