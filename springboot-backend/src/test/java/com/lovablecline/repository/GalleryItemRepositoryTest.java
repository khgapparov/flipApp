package com.lovablecline.repository;

import com.lovablecline.entity.GalleryItem;
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
class GalleryItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GalleryItemRepository galleryItemRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private Project project1;
    private Project project2;
    private GalleryItem galleryItem1;
    private GalleryItem galleryItem2;
    private GalleryItem galleryItem3;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create owner user
        owner = new User("galleryowner", "gallery@example.com", "password123");
        owner.setIsActive(true);
        owner.setIsAnonymous(false);
        entityManager.persist(owner);

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

        // Create test gallery items
        galleryItem1 = new GalleryItem("https://example.com/image1.jpg", "Kitchen before", "Kitchen before renovation", 
                                      "Kitchen", "Before", project1.getId());
        entityManager.persist(galleryItem1);

        galleryItem2 = new GalleryItem("https://example.com/image2.jpg", "Kitchen during", "Kitchen during renovation", 
                                      "Kitchen", "During", project1.getId());
        entityManager.persist(galleryItem2);

        galleryItem3 = new GalleryItem("https://example.com/image3.jpg", "Living room", "Living room planning", 
                                      "Living Room", "Planning", project2.getId());
        entityManager.persist(galleryItem3);

        entityManager.flush();
    }

    @Test
    void findByProject_WhenProjectHasGalleryItems_ReturnsItems() {
        // When
        List<GalleryItem> projectItems = galleryItemRepository.findByProject(project1);

        // Then
        assertEquals(2, projectItems.size());
        assertTrue(projectItems.stream().allMatch(item -> item.getProjectId().equals(project1.getId())));
    }

    @Test
    void findByProject_WhenProjectHasNoGalleryItems_ReturnsEmptyList() {
        // Given - create a project with no gallery items
        Project emptyProject = new Project("Empty Project", "Planning", "999 Empty St", 
                                         LocalDateTime.now(), 
                                         LocalDateTime.now().plusMonths(6), 
                                         "on track", owner.getId());
        entityManager.persist(emptyProject);
        entityManager.flush();

        // When
        List<GalleryItem> items = galleryItemRepository.findByProject(emptyProject);

        // Then
        assertTrue(items.isEmpty());
    }

    @Test
    void findByProjectId_WhenProjectHasGalleryItems_ReturnsItems() {
        // When
        List<GalleryItem> projectItems = galleryItemRepository.findByProjectId(project1.getId());

        // Then
        assertEquals(2, projectItems.size());
        assertTrue(projectItems.stream().allMatch(item -> item.getProjectId().equals(project1.getId())));
    }

    @Test
    void findByProjectId_WhenProjectHasNoGalleryItems_ReturnsEmptyList() {
        // Given - create a project with no gallery items
        Project emptyProject = new Project("Empty Project 2", "Planning", "888 Empty St", 
                                         LocalDateTime.now(), 
                                         LocalDateTime.now().plusMonths(6), 
                                         "on track", owner.getId());
        entityManager.persist(emptyProject);
        entityManager.flush();

        // When
        List<GalleryItem> items = galleryItemRepository.findByProjectId(emptyProject.getId());

        // Then
        assertTrue(items.isEmpty());
    }

    @Test
    void findByProjectIdOrderByCreatedAtDesc_ReturnsItemsInCorrectOrder() {
        // Given - create additional items with different timestamps
        GalleryItem olderItem = new GalleryItem("https://example.com/older.jpg", "Older", "Older item", 
                                              "Bathroom", "Before", project1.getId());
        olderItem.setCreatedAt(LocalDateTime.now().minusDays(2));
        entityManager.persist(olderItem);

        GalleryItem newerItem = new GalleryItem("https://example.com/newer.jpg", "Newer", "Newer item", 
                                              "Bathroom", "After", project1.getId());
        newerItem.setCreatedAt(LocalDateTime.now().minusDays(1));
        entityManager.persist(newerItem);

        entityManager.flush();

        // When
        List<GalleryItem> items = galleryItemRepository.findByProjectIdOrderByCreatedAtDesc(project1.getId());

        // Then - should be ordered by createdAt descending
        assertEquals(4, items.size());
        assertTrue(items.get(0).getCreatedAt().isAfter(items.get(1).getCreatedAt()) || 
                  items.get(0).getCreatedAt().isEqual(items.get(1).getCreatedAt()));
    }

    @Test
    void findByUserIdAndProjectId_WhenUserOwnsProject_ReturnsItems() {
        // When
        List<GalleryItem> items = galleryItemRepository.findByUserIdAndProjectId(owner.getId(), project1.getId());

        // Then
        assertEquals(2, items.size());
        assertTrue(items.stream().allMatch(item -> item.getProjectId().equals(project1.getId())));
    }

    @Test
    void findByUserIdAndProjectId_WhenUserDoesNotOwnProject_ReturnsEmptyList() {
        // Given - create another user
        User otherUser = new User("otheruser", "other@example.com", "password456");
        otherUser.setIsActive(true);
        entityManager.persist(otherUser);
        entityManager.flush();

        // When
        List<GalleryItem> items = galleryItemRepository.findByUserIdAndProjectId(otherUser.getId(), project1.getId());

        // Then
        assertTrue(items.isEmpty());
    }


    @Test
    void findAllByUserId_WhenUserHasNoGalleryItems_ReturnsEmptyList() {
        // Given - create a user with no projects/gallery items
        User newUser = new User("nogallery", "nogallery@example.com", "password");
        newUser.setIsActive(true);
        entityManager.persist(newUser);
        entityManager.flush();

        // When
        List<GalleryItem> items = galleryItemRepository.findAllByUserId(newUser.getId());

        // Then
        assertTrue(items.isEmpty());
    }

    @Test
    void findByIdAndProjectId_WhenItemExists_ReturnsItem() {
        // When
        Optional<GalleryItem> foundItem = galleryItemRepository.findByIdAndProjectId(galleryItem1.getId(), project1.getId());

        // Then
        assertTrue(foundItem.isPresent());
        assertEquals(galleryItem1.getId(), foundItem.get().getId());
        assertEquals(project1.getId(), foundItem.get().getProjectId());
    }

    @Test
    void findByIdAndProjectId_WhenItemDoesNotBelongToProject_ReturnsEmpty() {
        // When - try to find galleryItem1 with project2's ID
        Optional<GalleryItem> foundItem = galleryItemRepository.findByIdAndProjectId(galleryItem1.getId(), project2.getId());

        // Then
        assertFalse(foundItem.isPresent());
    }

    @Test
    void findByIdAndProjectId_WhenItemDoesNotExist_ReturnsEmpty() {
        // When
        Optional<GalleryItem> foundItem = galleryItemRepository.findByIdAndProjectId("nonexistent-id", project1.getId());

        // Then
        assertFalse(foundItem.isPresent());
    }

    @Test
    void findByProjectIdAndRoom_WhenItemsExist_ReturnsItems() {
        // When
        List<GalleryItem> kitchenItems = galleryItemRepository.findByProjectIdAndRoom(project1.getId(), "Kitchen");

        // Then
        assertEquals(2, kitchenItems.size());
        assertTrue(kitchenItems.stream().allMatch(item -> "Kitchen".equals(item.getRoom())));
    }

    @Test
    void findByProjectIdAndRoom_WhenNoItemsForRoom_ReturnsEmptyList() {
        // When
        List<GalleryItem> items = galleryItemRepository.findByProjectIdAndRoom(project1.getId(), "NonExistentRoom");

        // Then
        assertTrue(items.isEmpty());
    }

    @Test
    void findByProjectIdAndStage_WhenItemsExist_ReturnsItems() {
        // When
        List<GalleryItem> beforeItems = galleryItemRepository.findByProjectIdAndStage(project1.getId(), "Before");

        // Then
        assertEquals(1, beforeItems.size());
        assertEquals("Before", beforeItems.get(0).getStage());
        assertEquals("Kitchen before", beforeItems.get(0).getTitle());
        assertEquals("Kitchen before renovation", beforeItems.get(0).getDescription());
    }

    @Test
    void findByProjectIdAndStage_WhenNoItemsForStage_ReturnsEmptyList() {
        // When
        List<GalleryItem> items = galleryItemRepository.findByProjectIdAndStage(project1.getId(), "NonExistentStage");

        // Then
        assertTrue(items.isEmpty());
    }

    @Test
    void save_NewGalleryItem_SavesSuccessfully() {
        // Given
        GalleryItem newItem = new GalleryItem("https://example.com/new.jpg", "New bathroom", "New bathroom description", 
                                            "Bathroom", "During", project1.getId());

        // When
        GalleryItem savedItem = galleryItemRepository.save(newItem);

        // Then
        assertNotNull(savedItem.getId());
        assertEquals("Bathroom", savedItem.getRoom());
        assertEquals("During", savedItem.getStage());
        assertEquals(project1.getId(), savedItem.getProjectId());
        assertNotNull(savedItem.getCreatedAt());
    }

    @Test
    void delete_ExistingGalleryItem_RemovesItem() {
        // Given
        String itemId = galleryItem1.getId();

        // When
        galleryItemRepository.delete(galleryItem1);
        entityManager.flush();

        // Then
        Optional<GalleryItem> deletedItem = galleryItemRepository.findById(itemId);
        assertFalse(deletedItem.isPresent());
    }

    @Test
    void findAll_ReturnsAllGalleryItems() {
        // When
        List<GalleryItem> allItems = galleryItemRepository.findAll();

        // Then
        assertEquals(3, allItems.size());
    }

    @Test
    void findById_WhenItemExists_ReturnsItem() {
        // When
        Optional<GalleryItem> foundItem = galleryItemRepository.findById(galleryItem1.getId());

        // Then
        assertTrue(foundItem.isPresent());
        assertEquals(galleryItem1.getId(), foundItem.get().getId());
        assertEquals("Kitchen before", foundItem.get().getTitle());
        assertEquals("Kitchen before renovation", foundItem.get().getDescription());
    }

    @Test
    void findById_WhenItemDoesNotExist_ReturnsEmpty() {
        // When
        Optional<GalleryItem> foundItem = galleryItemRepository.findById("nonexistent-id");

        // Then
        assertFalse(foundItem.isPresent());
    }
}
