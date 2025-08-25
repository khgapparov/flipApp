package com.lovablecline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovablecline.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_Success() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("access_token", "test-token");
        mockResponse.put("token_type", "bearer");
        mockResponse.put("user_id", "123");

        when(authenticationService.login(anyString(), anyString())).thenReturn(mockResponse);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        credentials.put("password", "password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-token"))
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.user_id").value("123"));

        verify(authenticationService, times(1)).login("testuser", "password");
    }

    @Test
    void login_MissingCredentials() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        // Missing password

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username and password are required"));

        verify(authenticationService, never()).login(anyString(), anyString());
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        when(authenticationService.login(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "testuser");
        credentials.put("password", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        verify(authenticationService, times(1)).login("testuser", "wrongpassword");
    }

    @Test
    void register_Success() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("access_token", "test-token");
        mockResponse.put("token_type", "bearer");
        mockResponse.put("user_id", "123");

        when(authenticationService.register(anyString(), anyString(), anyString())).thenReturn(mockResponse);

        Map<String, String> userData = new HashMap<>();
        userData.put("username", "newuser");
        userData.put("email", "newuser@example.com");
        userData.put("password", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-token"))
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.user_id").value("123"));

        verify(authenticationService, times(1)).register("newuser", "newuser@example.com", "password");
    }

    @Test
    void register_MissingFields() throws Exception {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "newuser");
        userData.put("email", "newuser@example.com");
        // Missing password

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username, email, and password are required"));

        verify(authenticationService, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    void register_DuplicateUsername() throws Exception {
        when(authenticationService.register(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Username already exists"));

        Map<String, String> userData = new HashMap<>();
        userData.put("username", "existinguser");
        userData.put("email", "existinguser@example.com");
        userData.put("password", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));

        verify(authenticationService, times(1)).register("existinguser", "existinguser@example.com", "password");
    }

    @Test
    void validateToken_Post_Success() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(true);

        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("token", "valid-token");

        mockMvc.perform(post("/api/auth/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        verify(authenticationService, times(1)).validateToken("valid-token");
    }

    @Test
    void validateToken_Post_MissingToken() throws Exception {
        Map<String, String> tokenData = new HashMap<>();
        // Missing token

        mockMvc.perform(post("/api/auth/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token is required"));

        verify(authenticationService, never()).validateToken(anyString());
    }

    @Test
    void validateToken_Get_WithAuthorizationHeader() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate-token")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        verify(authenticationService, times(1)).validateToken("valid-token");
    }

    @Test
    void validateToken_Get_WithQueryParameter() throws Exception {
        when(authenticationService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate-token")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        verify(authenticationService, times(1)).validateToken("valid-token");
    }

    @Test
    void validateToken_Get_MissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Token is required. Provide either Authorization header or token query parameter"));

        verify(authenticationService, never()).validateToken(anyString());
    }

    @Test
    void anonymousLogin_Success() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("access_token", "anonymous-token");
        mockResponse.put("token_type", "bearer");
        mockResponse.put("user_id", "456");
        mockResponse.put("is_anonymous", true);

        when(authenticationService.anonymousLogin()).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/anonymous"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("anonymous-token"))
                .andExpect(jsonPath("$.is_anonymous").value(true));

        verify(authenticationService, times(1)).anonymousLogin();
    }

    @Test
    void anonymousLogin_Failure() throws Exception {
        when(authenticationService.anonymousLogin())
                .thenThrow(new RuntimeException("Anonymous login failed"));

        mockMvc.perform(post("/api/auth/anonymous"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Anonymous login failed"));

        verify(authenticationService, times(1)).anonymousLogin();
    }
}
