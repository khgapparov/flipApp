package com.ecolightcline.controller;

import com.ecolightcline.entity.UserProfile;
import com.ecolightcline.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        Optional<UserProfile> profile = userProfileService.getUserProfileById(userId);
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserProfileByUsername(@PathVariable String username) {
        Optional<UserProfile> profile = userProfileService.getUserProfileByUsername(username);
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserProfileByEmail(@PathVariable String email) {
        Optional<UserProfile> profile = userProfileService.getUserProfileByEmail(email);
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUserProfile(@RequestBody UserProfile userProfile) {
        try {
            UserProfile createdProfile = userProfileService.createUserProfile(userProfile);
            return ResponseEntity.ok(createdProfile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserProfile(@PathVariable String userId, @RequestBody UserProfile updatedProfile) {
        try {
            UserProfile profile = userProfileService.updateUserProfile(userId, updatedProfile);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUserProfile(@PathVariable String userId) {
        try {
            userProfileService.deleteUserProfile(userId);
            return ResponseEntity.ok().body(Map.of("message", "User profile deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUserProfiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        Page<UserProfile> profiles = userProfileService.getUserProfilesPaginated(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", profiles.getContent());
        response.put("currentPage", page);
        response.put("totalPages", profiles.getTotalPages());
        response.put("totalCount", profiles.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        Page<UserProfile> results = userProfileService.searchUsersPaginated(query, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", results.getContent());
        response.put("currentPage", page);
        response.put("totalPages", results.getTotalPages());
        response.put("totalCount", results.getTotalElements());
        response.put("query", query);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUserCount() {
        long count = userProfileService.getTotalUserCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/search/count")
    public ResponseEntity<?> getSearchCount(@RequestParam String query) {
        long count = userProfileService.getSearchResultCount(query);
        return ResponseEntity.ok(Map.of("count", count, "query", query));
    }

    @PatchMapping("/{userId}/last-login")
    public ResponseEntity<?> updateLastLogin(@PathVariable String userId) {
        try {
            userProfileService.updateLastLogin(userId);
            return ResponseEntity.ok().body(Map.of("message", "Last login updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
