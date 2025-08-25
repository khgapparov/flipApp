package com.lovablecline.service;

import com.lovablecline.entity.User;
import com.lovablecline.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private final String testJwtSecret = "test-secret-key-that-is-long-enough-for-hs256-algorithm";
    private final Long testJwtExpiration = 1800000L; // 30 minutes

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationService();
        
        // Use reflection to set private fields for testing
        try {
            var userRepositoryField = AuthenticationService.class.getDeclaredField("userRepository");
            userRepositoryField.setAccessible(true);
            userRepositoryField.set(authenticationService, userRepository);

            var passwordEncoderField = AuthenticationService.class.getDeclaredField("passwordEncoder");
            passwordEncoderField.setAccessible(true);
            passwordEncoderField.set(authenticationService, passwordEncoder);

            var jwtSecretField = AuthenticationService.class.getDeclaredField("jwtSecret");
            jwtSecretField.setAccessible(true);
            jwtSecretField.set(authenticationService, testJwtSecret);

            var jwtExpirationField = AuthenticationService.class.getDeclaredField("jwtExpiration");
            jwtExpirationField.setAccessible(true);
            jwtExpirationField.set(authenticationService, testJwtExpiration);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private fields for testing", e);
        }
    }

    @Test
    void login_Success() {
        // Arrange
        User user = new User();
        user.setId("123");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        // Act
        Map<String, Object> result = authenticationService.login("testuser", "password");

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("access_token"));
        assertEquals("bearer", result.get("token_type"));
        assertEquals("123", result.get("user_id"));
        
        Map<?, ?> userMap = (Map<?, ?>) result.get("user");
        assertNotNull(userMap);
        assertEquals("123", userMap.get("id"));
        assertEquals("testuser", userMap.get("username"));
        assertEquals("test@example.com", userMap.get("email"));

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password", "encoded-password");
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.login("nonexistent", "password");
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        User user = new User();
        user.setId("123");
        user.setUsername("testuser");
        user.setPassword("encoded-password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.login("testuser", "wrongpassword");
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpassword", "encoded-password");
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        User savedUser = new User();
        savedUser.setId("456");
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setPassword("encoded-password");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        Map<String, Object> result = authenticationService.register("newuser", "new@example.com", "password");

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("access_token"));
        assertEquals("bearer", result.get("token_type"));
        assertEquals("456", result.get("user_id"));
        
        Map<?, ?> userMap = (Map<?, ?>) result.get("user");
        assertNotNull(userMap);
        assertEquals("456", userMap.get("id"));
        assertEquals("newuser", userMap.get("username"));
        assertEquals("new@example.com", userMap.get("email"));

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("new@example.com");
        verify(passwordEncoder, times(1)).encode("password");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.register("existinguser", "new@example.com", "password");
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("existinguser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.register("newuser", "existing@example.com", "password");
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateToken_ValidToken() {
        // Arrange
        String validToken = generateTestToken("testuser", "123");

        // Act
        boolean result = authenticationService.validateToken(validToken);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateToken_InvalidToken() {
        // Act
        boolean result = authenticationService.validateToken("invalid-token");

        // Assert
        assertFalse(result);
    }

    @Test
    void getUsernameFromToken_ValidToken() {
        // Arrange
        String validToken = generateTestToken("testuser", "123");

        // Act
        String username = authenticationService.getUsernameFromToken(validToken);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void getUsernameFromToken_InvalidToken() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.getUsernameFromToken("invalid-token");
        });

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void getUserIdFromToken_ValidToken() {
        // Arrange
        String validToken = generateTestToken("testuser", "123");

        // Act
        String userId = authenticationService.getUserIdFromToken(validToken);

        // Assert
        assertEquals("123", userId);
    }

    @Test
    void getUserIdFromToken_InvalidToken() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.getUserIdFromToken("invalid-token");
        });

        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    void anonymousLogin_ExistingAnonymousUser() {
        // Arrange
        User existingAnonymousUser = new User();
        existingAnonymousUser.setId("anon-123");
        existingAnonymousUser.setUsername("guest_123");
        existingAnonymousUser.setEmail("guest_123@anonymous.com");
        existingAnonymousUser.setIsAnonymous(true);

        when(userRepository.findByIsAnonymous(true)).thenReturn(List.of(existingAnonymousUser));

        // Act
        Map<String, Object> result = authenticationService.anonymousLogin();

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("access_token"));
        assertEquals("bearer", result.get("token_type"));
        assertEquals("anon-123", result.get("user_id"));
        assertEquals(true, result.get("is_anonymous"));
        
        Map<?, ?> userMap = (Map<?, ?>) result.get("user");
        assertNotNull(userMap);
        assertEquals("anon-123", userMap.get("id"));
        assertEquals("guest_123", userMap.get("username"));
        assertEquals(true, userMap.get("is_anonymous"));

        verify(userRepository, times(1)).findByIsAnonymous(true);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void anonymousLogin_NewAnonymousUser() {
        // Arrange
        when(userRepository.findByIsAnonymous(true)).thenReturn(new ArrayList<>());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        User newUser = new User();
        newUser.setId("new-anon-id");
        newUser.setUsername("guest_123456789");
        newUser.setEmail("guest_123456789@anonymous.com");
        newUser.setPassword("encoded-password");
        newUser.setIsAnonymous(true);

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        Map<String, Object> result = authenticationService.anonymousLogin();

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("access_token"));
        assertEquals("bearer", result.get("token_type"));
        assertEquals("new-anon-id", result.get("user_id"));
        assertEquals(true, result.get("is_anonymous"));

        verify(userRepository, times(1)).findByIsAnonymous(true);
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    private String generateTestToken(String username, String userId) {
        SecretKey key = Keys.hmacShaKeyFor(testJwtSecret.getBytes());
        
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + testJwtExpiration))
                .signWith(key)
                .compact();
    }
}
