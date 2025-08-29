package com.ecolightcline.service;

import com.ecolightcline.entity.UserProfile;
import com.ecolightcline.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Autowired
    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfile> getUserProfileById(String userId) {
        return userProfileRepository.findByUserId(userId);
    }

    public Optional<UserProfile> getUserProfileByUsername(String username) {
        return userProfileRepository.findByUsername(username);
    }

    public Optional<UserProfile> getUserProfileByEmail(String email) {
        return userProfileRepository.findByEmail(email);
    }

    public UserProfile createUserProfile(UserProfile userProfile) {
        if (userProfileRepository.existsByUsername(userProfile.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileRepository.existsByEmail(userProfile.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        return userProfileRepository.save(userProfile);
    }

    public UserProfile updateUserProfile(String userId, UserProfile updatedProfile) {
        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));

        // Check if username is being changed and if it's already taken
        if (!existingProfile.getUsername().equals(updatedProfile.getUsername()) &&
            userProfileRepository.existsByUsername(updatedProfile.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email is being changed and if it's already taken
        if (!existingProfile.getEmail().equals(updatedProfile.getEmail()) &&
            userProfileRepository.existsByEmail(updatedProfile.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Update fields
        existingProfile.setUsername(updatedProfile.getUsername());
        existingProfile.setEmail(updatedProfile.getEmail());
        existingProfile.setFirstName(updatedProfile.getFirstName());
        existingProfile.setLastName(updatedProfile.getLastName());
        existingProfile.setAvatarUrl(updatedProfile.getAvatarUrl());
        existingProfile.setBio(updatedProfile.getBio());

        return userProfileRepository.save(existingProfile);
    }

    public void deleteUserProfile(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
        userProfileRepository.delete(profile);
    }

    public List<UserProfile> getAllUserProfiles() {
        return userProfileRepository.findAll();
    }

    public Page<UserProfile> getUserProfilesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return userProfileRepository.findAll(pageable);
    }

    public List<UserProfile> searchUsers(String query) {
        return userProfileRepository.searchUsers(query);
    }

    public Page<UserProfile> searchUsersPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        List<UserProfile> results = userProfileRepository.searchUsers(query);
        
        // Manual pagination since we're using custom query
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), results.size());
        
        return new org.springframework.data.domain.PageImpl<>(
            results.subList(start, end),
            pageable,
            results.size()
        );
    }

    public long getTotalUserCount() {
        return userProfileRepository.count();
    }

    public long getSearchResultCount(String query) {
        return userProfileRepository.countSearchResults(query);
    }

    public void updateLastLogin(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
        profile.setLastLoginAt(LocalDateTime.now());
        userProfileRepository.save(profile);
    }
}
