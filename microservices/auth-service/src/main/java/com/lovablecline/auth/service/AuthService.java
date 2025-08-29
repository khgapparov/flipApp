package com.ecolightcline.auth.service;

import com.ecolightcline.auth.entity.RefreshToken;
import com.ecolightcline.auth.entity.User;
import com.ecolightcline.auth.repository.RefreshTokenRepository;
import com.ecolightcline.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User registerUser(String username, String email, String password, String firstName, String lastName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);

        return userRepository.save(user);
    }

    public User authenticateUser(String identifier, String password) {
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(identifier, identifier);
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOptional.get();
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000);

        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken refreshAccessToken(String refreshToken) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(refreshToken);
        
        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken token = tokenOptional.get();
        
        if (token.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (token.isExpired()) {
            throw new RuntimeException("Refresh token expired");
        }

        return token;
    }

    public void logout(String refreshToken) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(refreshToken);
        
        if (tokenOptional.isPresent()) {
            RefreshToken token = tokenOptional.get();
            token.revoke();
            refreshTokenRepository.save(token);
        }
    }

    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token) && !jwtService.isTokenExpired(token);
    }

    public Optional<User> getUserFromToken(String token) {
        if (!validateToken(token)) {
            return Optional.empty();
        }

        String userId = jwtService.extractUserId(token);
        return userRepository.findById(userId);
    }

    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
