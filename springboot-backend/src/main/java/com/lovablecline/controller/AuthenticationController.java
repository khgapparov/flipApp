package com.lovablecline.controller;

import com.lovablecline.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }

            Map<String, Object> response = authenticationService.login(username, password);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        try {
            String username = userData.get("username");
            String email = userData.get("email");
            String password = userData.get("password");
            
            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username, email, and password are required"));
            }

            Map<String, Object> response = authenticationService.register(username, email, password);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> tokenData) {
        try {
            String token = tokenData.get("token");
            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
            }

            boolean isValid = authenticationService.validateToken(token);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateTokenGet(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                             @RequestParam(value = "token", required = false) String tokenParam) {
        try {
            String token = null;
            
            // Check Authorization header first (Bearer token)
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            // If no header, check query parameter
            else if (tokenParam != null) {
                token = tokenParam;
            }
            
            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token is required. Provide either Authorization header or token query parameter"));
            }

            boolean isValid = authenticationService.validateToken(token);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
    }

    @PostMapping("/anonymous")
    public ResponseEntity<?> anonymousLogin() {
        try {
            Map<String, Object> response = authenticationService.anonymousLogin();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
