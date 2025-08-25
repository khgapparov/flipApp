package com.lovablecline.repository;

import com.lovablecline.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User regularUser;
    private User anonymousUser;
    private User expiredAnonymousUser;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test users
        regularUser = new User("testuser", "test@example.com", "password123");
        regularUser.setIsActive(true);
        regularUser.setIsAnonymous(false);
        entityManager.persist(regularUser);

        anonymousUser = new User("anon123", "anon@example.com", "anonpass");
        anonymousUser.setIsActive(true);
        anonymousUser.setIsAnonymous(true);
        anonymousUser.setCreatedAt(LocalDateTime.now().minusHours(1));
        entityManager.persist(anonymousUser);

        expiredAnonymousUser = new User("expiredanon", "expired@example.com", "expiredpass");
        expiredAnonymousUser.setIsActive(true);
        expiredAnonymousUser.setIsAnonymous(true);
        expiredAnonymousUser.setCreatedAt(LocalDateTime.now().minusDays(2)); // 2 days old
        entityManager.persist(expiredAnonymousUser);

        entityManager.flush();
    }

    @Test
    void findByUsername_WhenUserExists_ReturnsUser() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    void findByUsername_WhenUserDoesNotExist_ReturnsEmpty() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void findByEmail_WhenUserExists_ReturnsUser() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("test@example.com", foundUser.get().getEmail());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    void findByEmail_WhenUserDoesNotExist_ReturnsEmpty() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void existsByUsername_WhenUsernameExists_ReturnsTrue() {
        // When
        Boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUsername_WhenUsernameDoesNotExist_ReturnsFalse() {
        // When
        Boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void existsByEmail_WhenEmailExists_ReturnsTrue() {
        // When
        Boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmail_WhenEmailDoesNotExist_ReturnsFalse() {
        // When
        Boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertFalse(exists);
    }

    @Test
    void findExpiredAnonymousUsers_WhenUsersExist_ReturnsExpiredUsers() {
        // Given - cutoff date is 1 day ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);

        // When
        List<User> expiredUsers = userRepository.findExpiredAnonymousUsers(cutoffDate);

        // Then
        assertEquals(1, expiredUsers.size());
        assertEquals("expiredanon", expiredUsers.get(0).getUsername());
        assertTrue(expiredUsers.get(0).getIsAnonymous());
    }

    @Test
    void findExpiredAnonymousUsers_WhenNoExpiredUsers_ReturnsEmptyList() {
        // Given - cutoff date is 3 days ago (should include no users)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);

        // When
        List<User> expiredUsers = userRepository.findExpiredAnonymousUsers(cutoffDate);

        // Then
        assertTrue(expiredUsers.isEmpty());
    }

    @Test
    void findByIsAnonymous_WhenAnonymousUsersExist_ReturnsAnonymousUsers() {
        // When
        List<User> anonymousUsers = userRepository.findByIsAnonymous(true);

        // Then
        assertEquals(2, anonymousUsers.size());
        assertTrue(anonymousUsers.stream().allMatch(User::getIsAnonymous));
    }

    @Test
    void findByIsAnonymous_WhenNonAnonymousUsersExist_ReturnsNonAnonymousUsers() {
        // When
        List<User> nonAnonymousUsers = userRepository.findByIsAnonymous(false);

        // Then
        assertEquals(1, nonAnonymousUsers.size());
        assertFalse(nonAnonymousUsers.get(0).getIsAnonymous());
        assertEquals("testuser", nonAnonymousUsers.get(0).getUsername());
    }

    @Test
    void save_NewUser_SavesSuccessfully() {
        // Given
        User newUser = new User("newuser", "new@example.com", "newpassword");
        newUser.setIsActive(true);
        newUser.setIsAnonymous(false);

        // When
        User savedUser = userRepository.save(newUser);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void delete_ExistingUser_RemovesUser() {
        // Given
        String userId = regularUser.getId();

        // When
        userRepository.delete(regularUser);
        entityManager.flush();

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void findAll_ReturnsAllUsers() {
        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertEquals(3, allUsers.size());
    }

    @Test
    void findById_WhenUserExists_ReturnsUser() {
        // When
        Optional<User> foundUser = userRepository.findById(regularUser.getId());

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(regularUser.getId(), foundUser.get().getId());
    }

    @Test
    void findById_WhenUserDoesNotExist_ReturnsEmpty() {
        // When
        Optional<User> foundUser = userRepository.findById("nonexistent-id");

        // Then
        assertFalse(foundUser.isPresent());
    }
}
