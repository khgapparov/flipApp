# Anonymous Login Fix Prompt

## Problem Analysis
The application is failing with "Failed to login anonymously" error because:
1. Frontend calls `authService.anonymousLogin()` but this method doesn't exist
2. Backend doesn't have an anonymous login endpoint
3. No anonymous user creation/authentication logic exists

## Required Changes

### Frontend Changes (`lovable-cline-app/src/services/api.js`)

Add the `anonymousLogin` method to the `authService`:

```javascript
// Authentication services
export const authService = {
  // ... existing methods ...
  
  // Anonymous login - creates a temporary guest user
  anonymousLogin: async () => {
    try {
      const response = await apiRequest('/auth/anonymous', {
        method: 'POST'
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
        localStorage.setItem('is_anonymous', 'true'); // Mark as anonymous user
      }
      
      return response;
    } catch (error) {
      console.error('Anonymous login failed:', error);
      throw new Error('Failed to login anonymously');
    }
  },

  // Login with existing token (for token validation)
  loginWithToken: async (token) => {
    try {
      const response = await apiRequest('/auth/validate-token', {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      
      if (response.valid) {
        localStorage.setItem('auth_token', token);
        // Note: user_id should be extracted from token or stored separately
        return response;
      } else {
        throw new Error('Invalid token');
      }
    } catch (error) {
      console.error('Token login failed:', error);
      throw new Error('Failed to login with token');
    }
  }
};
```

### Backend Changes

#### 1. Authentication Controller (`springboot-backend/src/main/java/com/lovablecline/controller/AuthenticationController.java`)

Add anonymous login endpoint:

```java
@PostMapping("/anonymous")
public ResponseEntity<?> anonymousLogin() {
    try {
        Map<String, Object> response = authenticationService.anonymousLogin();
        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

#### 2. Authentication Service (`springboot-backend/src/main/java/com/lovablecline/service/AuthenticationService.java`)

Add anonymous login method:

```java
public Map<String, Object> anonymousLogin() {
    // Generate a unique anonymous username
    String anonymousUsername = "guest_" + System.currentTimeMillis();
    String anonymousEmail = anonymousUsername + "@anonymous.com";
    
    // Create a temporary password (not used for anonymous users)
    String tempPassword = UUID.randomUUID().toString();
    
    // Check if anonymous user already exists (optional)
    Optional<User> existingUser = userRepository.findByUsername(anonymousUsername);
    User user;
    
    if (existingUser.isPresent()) {
        user = existingUser.get();
    } else {
        // Create new anonymous user
        user = new User();
        user.setUsername(anonymousUsername);
        user.setEmail(anonymousEmail);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setAnonymous(true); // Add this field to User entity
        
        user = userRepository.save(user);
    }
    
    String token = generateToken(user);
    
    Map<String, Object> response = new HashMap<>();
    response.put("access_token", token);
    response.put("token_type", "bearer");
    response.put("user_id", user.getId());
    response.put("is_anonymous", true);
    response.put("user", Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "email", user.getEmail(),
        "is_anonymous", true
    ));
    
    return response;
}
```

#### 3. User Entity (`springboot-backend/src/main/java/com/lovablecline/entity/User.java`)

Add anonymous field:

```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields ...
    
    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;
    
    // Getter and setter
    public Boolean getAnonymous() {
        return isAnonymous;
    }
    
    public void setAnonymous(Boolean anonymous) {
        isAnonymous = anonymous;
    }
}
```

#### 4. User Repository (`springboot-backend/src/main/java/com/lovablecline/repository/UserRepository.java`)

Add method to find anonymous users (optional):

```java
@Query("SELECT u FROM User u WHERE u.isAnonymous = true AND u.createdAt > :cutoffDate")
List<User> findExpiredAnonymousUsers(@Param("cutoffDate") Date cutoffDate);
```

### Database Migration

Add the `is_anonymous` column to the users table:

```sql
ALTER TABLE users ADD COLUMN is_anonymous BOOLEAN DEFAULT FALSE;
```

### Cleanup Service (Optional)

Create a scheduled task to clean up old anonymous users:

```java
@Service
public class AnonymousUserCleanupService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldAnonymousUsers() {
        Date cutoffDate = new Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)); // 30 days ago
        List<User> oldAnonymousUsers = userRepository.findExpiredAnonymousUsers(cutoffDate);
        
        userRepository.deleteAll(oldAnonymousUsers);
        System.out.println("Cleaned up " + oldAnonymousUsers.size() + " old anonymous users");
    }
}
```

## Alternative Approach: JWT-Only Anonymous Users

If you don't want to store anonymous users in the database:

```java
public Map<String, Object> anonymousLogin() {
    // Generate anonymous user claims without database storage
    String anonymousId = "anon_" + UUID.randomUUID().toString();
    
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    
    String token = Jwts.builder()
            .setSubject(anonymousId)
            .claim("userId", anonymousId)
            .claim("isAnonymous", true)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))) // 24 hour expiry
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    
    Map<String, Object> response = new HashMap<>();
    response.put("access_token", token);
    response.put("token_type", "bearer");
    response.put("user_id", anonymousId);
    response.put("is_anonymous", true);
    
    return response;
}
```

## Testing

1. **Frontend Test**: Verify `authService.anonymousLogin()` works
2. **Backend Test**: Test `/api/auth/anonymous` endpoint
3. **Integration Test**: Full login flow with anonymous user
4. **Token Validation**: Ensure anonymous tokens are properly validated

## Security Considerations

1. Set shorter expiration for anonymous tokens (e.g., 24 hours)
2. Limit anonymous user permissions if needed
3. Consider rate limiting anonymous login endpoints
4. Add cleanup mechanism for old anonymous sessions

## Rollout Plan

1. Implement backend changes first
2. Update frontend API service
3. Test thoroughly
4. Deploy backend
5. Deploy frontend
6. Monitor for issues

This implementation provides a robust anonymous login system that integrates with your existing JWT authentication while maintaining security and scalability.
