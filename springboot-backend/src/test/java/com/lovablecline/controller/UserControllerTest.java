package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.entity.User;
import com.lovablecline.repository.UserRepository;
import com.lovablecline.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String validToken;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        validToken = "valid-jwt-token";

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
        testUser.setIsAnonymous(false);
        testUser.setCreatedAt(LocalDateTime.now());

        when(authenticationService.getUserIdFromToken(validToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    void getCurrentUser_WithValidToken_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.isAnonymous").value(false))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(authenticationService).getUserIdFromToken(validToken);
        verify(userRepository).findById(userId);
    }

    @Test
    void getCurrentUser_WithNoAuthHeader_ReturnsOkForPreflight() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentUser_WithInvalidToken_ReturnsBadRequest() throws Exception {
        when(authenticationService.getUserIdFromToken("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    void getCurrentUser_WithNonExistentUser_ReturnsNotFound() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_WithValidData_UpdatesProfile() throws Exception {
        Map<String, String> profileData = Map.of(
                "username", "newusername",
                "email", "newemail@example.com"
        );

        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"));

        verify(userRepository).existsByUsername("newusername");
        verify(userRepository).existsByEmail("newemail@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_WithExistingUsername_ReturnsBadRequest() throws Exception {
        Map<String, String> profileData = Map.of("username", "existinguser");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));

        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_WithExistingEmail_ReturnsBadRequest() throws Exception {
        Map<String, String> profileData = Map.of("email", "existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_WithSameUsername_UpdatesSuccessfully() throws Exception {
        Map<String, String> profileData = Map.of("username", "testuser");

        // Should not check existence for same username
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_WithSameEmail_UpdatesSuccessfully() throws Exception {
        Map<String, String> profileData = Map.of("email", "test@example.com");

        // Should not check existence for same email
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_WithNoAuthHeader_ReturnsBadRequest() throws Exception {
        Map<String, String> profileData = Map.of("username", "newuser");

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_WithValidCredentials_UpdatesPassword() throws Exception {
        Map<String, String> passwordData = Map.of(
                "currentPassword", "oldpassword",
                "newPassword", "newpassword"
        );

        when(passwordEncoder.matches("oldpassword", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/users/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(passwordEncoder).matches("oldpassword", "encoded-password");
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_WithIncorrectCurrentPassword_ReturnsBadRequest() throws Exception {
        Map<String, String> passwordData = Map.of(
                "currentPassword", "wrongpassword",
                "newPassword", "newpassword"
        );

        when(passwordEncoder.matches("wrongpassword", "encoded-password")).thenReturn(false);

        mockMvc.perform(post("/api/users/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));

        verify(passwordEncoder).matches("wrongpassword", "encoded-password");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithNoAuthHeader_ReturnsBadRequest() throws Exception {
        Map<String, String> passwordData = Map.of(
                "currentPassword", "oldpassword",
                "newPassword", "newpassword"
        );

        mockMvc.perform(post("/api/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordData)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_WithMissingFields_ReturnsBadRequest() throws Exception {
        Map<String, String> passwordData = Map.of("currentPassword", "oldpassword");

        mockMvc.perform(post("/api/users/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordData)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_WithNonExistentUser_ReturnsNotFound() throws Exception {
        Map<String, String> passwordData = Map.of(
                "currentPassword", "oldpassword",
                "newPassword", "newpassword"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/users/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordData)))
                .andExpect(status().isNotFound());

        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
