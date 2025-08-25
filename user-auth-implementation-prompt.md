# Comprehensive Prompt: User Login/Signup Implementation with Profile-Based Rendering

## Objective
Implement a complete user authentication system with login/signup pages and profile-based UI rendering for the Lovable Cline application. The system should handle both authenticated users and anonymous guests, with appropriate UI differentiation based on user status.

## Current State Analysis
The backend already has:
- JWT-based authentication (Spring Boot)
- User entity with profile fields
- Authentication controller and service
- Anonymous login functionality

The frontend needs:
- Proper login/signup UI components
- Auth state management
- Profile-based rendering logic
- Enhanced API services for user management

## Implementation Requirements

### Phase 1: Frontend Authentication Components

#### 1. Create Auth Context and Hook
```javascript
// src/contexts/AuthContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import { enhancedAuthService } from '../services/enhancedApi';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = localStorage.getItem('auth_token');
      const userId = localStorage.getItem('user_id');
      const isAnonymous = localStorage.getItem('is_anonymous') === 'true';

      if (token && userId) {
        // Validate token and get user profile
        const isValid = await enhancedAuthService.validateToken(token);
        if (isValid) {
          const userProfile = await enhancedAuthService.getCurrentUser();
          setUser({
            ...userProfile,
            isAnonymous,
            token
          });
        } else {
          // Token invalid, clear storage
          clearAuth();
        }
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      clearAuth();
    } finally {
      setLoading(false);
    }
  };

  const login = async (username, password) => {
    try {
      setLoading(true);
      setError(null);
      const response = await enhancedAuthService.login(username, password);
      
      const userProfile = await enhancedAuthService.getCurrentUser();
      setUser({
        ...userProfile,
        isAnonymous: false,
        token: response.access_token
      });
      
      return response;
    } catch (error) {
      setError(error.message || 'Login failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const register = async (username, email, password) => {
    try {
      setLoading(true);
      setError(null);
      const response = await enhancedAuthService.register(username, email, password);
      
      const userProfile = await enhancedAuthService.getCurrentUser();
      setUser({
        ...userProfile,
        isAnonymous: false,
        token: response.access_token
      });
      
      return response;
    } catch (error) {
      setError(error.message || 'Registration failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const anonymousLogin = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await enhancedAuthService.anonymousLogin();
      
      setUser({
        id: response.user_id,
        username: response.user.username,
        email: response.user.email,
        isAnonymous: true,
        token: response.access_token
      });
      
      return response;
    } catch (error) {
      setError(error.message || 'Anonymous login failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    clearAuth();
    setUser(null);
  };

  const clearAuth = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('is_anonymous');
  };

  const value = {
    user,
    loading,
    error,
    login,
    register,
    anonymousLogin,
    logout,
    clearAuth
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
```

#### 2. Create Login/Signup Modal Components
```javascript
// src/components/auth/LoginModal.jsx
import React, { useState } from 'react';
import { X, Mail, Lock, User } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const LoginModal = ({ isOpen, onClose, onSwitchToSignup }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login, loading, error } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await login(username, password);
      onClose();
    } catch (error) {
      // Error handled by context
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-md">
        <div className="flex justify-between items-center p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-800">Login</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-700 rounded-md">
              {error}
            </div>
          )}
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Username
            </label>
            <div className="relative">
              <User size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter your username"
                required
              />
            </div>
          </div>
          
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <div className="relative">
              <Lock size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter your password"
                required
              />
            </div>
          </div>
          
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
          
          <div className="mt-4 text-center">
            <button
              type="button"
              onClick={onSwitchToSignup}
              className="text-blue-600 hover:text-blue-700 text-sm"
            >
              Don't have an account? Sign up
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginModal;
```

```javascript
// src/components/auth/SignupModal.jsx
import React, { useState } from 'react';
import { X, Mail, Lock, User } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const SignupModal = ({ isOpen, onClose, onSwitchToLogin }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [errors, setErrors] = useState({});
  const { register, loading, error } = useAuth();

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    }
    
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Email is invalid';
    }
    
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) return;
    
    try {
      await register(formData.username, formData.email, formData.password);
      onClose();
    } catch (error) {
      // Error handled by context
    }
  };

  const handleChange = (e) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
    
    // Clear error when user starts typing
    if (errors[e.target.name]) {
      setErrors(prev => ({
        ...prev,
        [e.target.name]: ''
      }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-md">
        <div className="flex justify-between items-center p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-800">Create Account</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-700 rounded-md">
              {error}
            </div>
          )}
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Username
            </label>
            <div className="relative">
              <User size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className={`w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.username ? 'border-red-300' : 'border-gray-300'
                }`}
                placeholder="Choose a username"
              />
            </div>
            {errors.username && (
              <p className="mt-1 text-sm text-red-600">{errors.username}</p>
            )}
          </div>
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email
            </label>
            <div className="relative">
              <Mail size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className={`w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.email ? 'border-red-300' : 'border-gray-300'
                }`}
                placeholder="Enter your email"
              />
            </div>
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">{errors.email}</p>
            )}
          </div>
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <div className="relative">
              <Lock size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className={`w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.password ? 'border-red-300' : 'border-gray-300'
                }`}
                placeholder="Create a password"
              />
            </div>
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password}</p>
            )}
          </div>
          
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Confirm Password
            </label>
            <div className="relative">
              <Lock size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className={`w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.confirmPassword ? 'border-red-300' : 'border-gray-300'
                }`}
                placeholder="Confirm your password"
              />
            </div>
            {errors.confirmPassword && (
              <p className="mt-1 text-sm text-red-600">{errors.confirmPassword}</p>
            )}
          </div>
          
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating account...' : 'Sign Up'}
          </button>
          
          <div className="mt-4 text-center">
            <button
              type="button"
              onClick={onSwitchToLogin}
              className="text-blue-600 hover:text-blue-700 text-sm"
            >
              Already have an account? Login
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SignupModal;
```

#### 3. Create User Profile Component
```javascript
// src/components/auth/UserProfile.jsx
import React, { useState } from 'react';
import { User, LogOut, Edit3, Save, X } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const UserProfile = () => {
  const { user, logout } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState({
    username: user?.username || '',
    email: user?.email || ''
  });

  const handleSave = async () => {
    // Implement profile update logic
    try {
      // await enhancedAuthService.updateProfile(editData);
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update profile:', error);
    }
  };

  const handleCancel = () => {
    setEditData({
      username: user?.username || '',
      email: user?.email || ''
    });
    setIsEditing(false);
  };

  if (!user) return null;

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-800">User Profile</h3>
        {user.isAnonymous && (
          <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">
            Guest User
          </span>
        )}
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Username
          </label>
          {isEditing ? (
            <input
              type="text"
              value={editData.username}
              onChange={(e) => setEditData(prev => ({ ...prev, username: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          ) : (
            <p className="text-gray-900">{user.username}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Email
          </label>
          {isEditing ? (
            <input
              type="email"
              value={editData.email}
              onChange={(e) => setEditData(prev => ({ ...prev, email: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          ) : (
            <p className="text-gray-900">{user.email}</p>
          )}
        </div>

        <div className="flex space-x-3 pt-4">
          {isEditing ? (
            <>
              <button
                onClick={handleSave}
                className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
              >
                <Save size={16} />
                <span>Save</span>
              </button>
              <button
                onClick={handleCancel}
                className="flex items-center space-x-2 bg-gray-300 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-400"
              >
                <X size={16} />
                <span>Cancel</span>
              </button>
            </>
          ) : (
            <>
              <button
                onClick={() => setIsEditing(true)}
                className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
              >
                <Edit3 size={16} />
                <span>Edit Profile</span>
              </button>
              <button
                onClick={logout}
                className="flex items-center space-x-2 bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700"
              >
                <LogOut size={16} />
                <span>Logout</span>
              </button>
            </>
          )}
        </div>

        {user.isAnonymous && (
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md">
            <p className="text-sm text-blue-800">
              You are using a guest account. Create a permanent account to save your progress and access additional features.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserProfile;
```

### Phase 2: Enhanced API Services

#### 4. Update Enhanced API Service
```javascript
// src/services/enhancedApi.js - Add user management methods
export const enhancedAuthService = {
  // ... existing methods ...
  
  getCurrentUser: async () => {
    try {
      const response = await apiRequest('/users/me');
      return response;
    } catch (error) {
      console.error('Failed to get user profile:', error);
      throw new Error('Failed to load user profile');
    }
  },

  updateProfile: async (profileData) => {
    try {
      const response = await apiRequest('/users/profile', {
        method: 'PUT',
        body: JSON.stringify(profileData)
      });
      return response;
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw new Error('Failed to update profile');
    }
  },

  changePassword: async (currentPassword, newPassword) => {
    try {
      const response = await apiRequest('/users/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword })
      });
      return response;
    } catch (error) {
      console.error('Failed to change password:', error);
      throw new Error('Failed to change password');
    }
  }
};
```

### Phase 3: Backend Enhancements

#### 5. Add User Profile Endpoints
```java
// springboot-backend/src/main/java/com/lovablecline/controller/UserController.java
package com.lovablecline.controller;

import com.lovablecline.entity.User;
import com.lovablecline.repository.UserRepository;
import com.lovablecline.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String userId = authenticationService.getUserIdFromToken(token);
            
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "isAnonymous", user.getIsAnonymous(),
                "createdAt", user.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> profileData) {
        try {
            String token = authHeader.substring(7);
            String userId = authenticationService.getUserIdFromToken(token);
            
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();
            String username = profileData.get("username");
            String email = profileData.get("email");

            // Validate username uniqueness (if changed)
            if (username != null && !username.equals(user.getUsername())) {
                if (userRepository.existsByUsername(username)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
                }
                user.setUsername(username);
            }

            // Validate email uniqueness (if changed)
            if (email != null && !email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
                }
                user.setEmail(email);
            }

            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "id", savedUser.getId(),
                "username", savedUser.getUsername(),
                "email", savedUser.getEmail(),
                "isAnonymous", savedUser.getIsAnonymous()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> passwordData) {
        try {
            String token = authHeader.substring(7);
            String userId = authenticationService.getUserIdFromToken(token);
            
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

### Phase 4: App Integration

#### 6. Update Main App Component
```javascript
// src/App.jsx - Updated with auth integration
import React, { useState, useEffect, useRef } from 'react';
import { 
  AlertCircle, 
  Home,
  Calendar,
  TrendingUp,
  Image,
  ClipboardList,
  MessageCircle,
  Send,
  ZoomIn,
  User as UserIcon
} from 'lucide-react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginModal from './components/auth/LoginModal';
import SignupModal from './components/auth/SignupModal';
import UserProfile from './components/auth/UserProfile';
// ... other imports ...

function AppContent() {
  const { user, loading: authLoading, anonymousLogin } = useAuth();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showSignupModal, setShowSignupModal] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  // ... other states ...

  useEffect(() => {
    const initializeApp = async () => {
      try {
        if (!user) {
          // No user logged in, try anonymous login
          await anonymousLogin();
        } else {
          // User is logged in, setup app
          await setupApiConnection();
        }
      } catch (error) {
        console.error('App initialization error:', error);
        setError('Failed to initialize application. Please refresh the page.');
        setLoading(false);
      }
    };

    initializeApp();
  }, [user]);

  // Add auth buttons to header
  const renderAuthButtons = () => {
    if (authLoading) {
      return <div className="loading-spinner-small"></div>;
    }

    if (user) {
      return (
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowProfile(true)}
            className="flex items-center space-x-2 text-gray-700 hover:text-gray-900"
          >
            <UserIcon size={18} />
            <span>{user.username}</span>
            {user.isAnonymous && (
              <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">
                Guest
              </span>
            )}
          </button>
        </div>
      );
    }

    return (
      <div className="flex items-center space-x-3">
        <button
          onClick={() => setShowLoginModal(true)}
          className="text-gray-700 hover:text-gray-900"
        >
          Login
        </button>
        <button
          onClick={() => setShowSignupModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          Sign Up
        </button>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header with auth buttons */}
      <header className="bg-brand-surface shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              {/* ... logo ... */}
            </div>
            {renderAuthButtons()}
          </div>
        </div>
      </header>

      {/* Main content */}
      {/* ... existing content ... */}

      {/* Auth Modals */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSwitchToSignup={() => {
          setShowLoginModal(false);
          setShowSignupModal(true);
        }}
      />

      <SignupModal
        isOpen={showSignupModal}
        onClose={() => setShowSignupModal(false)}
        onSwitchToLogin={() => {
          setShowSignupModal(false);
          setShowLoginModal(true);
        }}
      />

      {/* User Profile Modal */}
      {showProfile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-md">
            <div className="flex justify-between items-center p-6 border-b">
              <h2 className="text-xl font-semibold text-gray-800">User Profile</h2>
              <button 
                onClick={() => setShowProfile(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>
            <div className="p-6">
              <UserProfile />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
```

### Phase 5: Profile-Based Rendering

#### 7. Implement Conditional UI Based on User Status
```javascript
// Example: Enhanced components with user-specific rendering
const EnhancedComponent = () => {
  const { user } = useAuth();

  return (
    <div>
      {user && !user.isAnonymous && (
        <div className="premium-features">
          {/* Show premium features for authenticated users */}
          <h3>Premium Features</h3>
          <p>Welcome back, {user.username}!</p>
        </div>
      )}
      
      {user && user.isAnonymous && (
        <div className="guest-features">
          {/* Show guest-specific features */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4 mb-4">
            <p className="text-yellow-800">
              You're using a guest account. <button 
                onClick={() => setShowSignupModal(true)}
                className="text-yellow-900 underline hover:text-yellow-700"
              >
                Create a permanent account
              </button> to save your progress.
            </p>
          </div>
        </div>
      )}
      
      {!user && (
        <div className="auth-required">
          {/* Show login prompt for unauthenticated users */}
          <div className="bg-blue-50 border border-blue-200 rounded-md p-4 mb-4">
            <p className="text-blue-800">
              Please <button 
                onClick={() => setShowLoginModal(true)}
                className="text-blue-900 underline hover:text-blue-700"
              >
                login
              </button> or <button 
                onClick={() => setShowSignupModal(true)}
                className="text-blue-900 underline hover:text-blue-700"
              >
                sign up
              </button> to access all features.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
```

## Error Handling and Validation

### Comprehensive Error Types
1. **Network Errors**: Handle API connectivity issues
2. **Authentication Errors**: Invalid credentials, expired tokens
3. **Validation Errors**: Form validation, duplicate usernames/emails
4. **Server Errors**: Backend service failures
5. **Client Errors**: Browser storage issues, CORS problems

### Error Recovery Strategies
1. **Token Refresh**: Automatic token validation and refresh
2. **Fallback Authentication**: Graceful fallback to anonymous mode
3. **Local Storage Recovery**: Handle corrupted localStorage data
4. **Network Retry**: Exponential backoff for failed requests

## Testing Requirements

### Unit Tests
- Auth context functionality
- Form validation logic
- API service error handling
- Component rendering based on auth state

### Integration Tests
- Login/register flow
- Token management
- Profile updates
- Anonymous vs authenticated user experience

### E2E Tests
- Complete user registration flow
- Login/logout functionality
- Profile management
- Error scenarios handling

## Deployment Considerations

1. **Environment Variables**: Configure JWT secrets, API URLs
2. **CORS Settings**: Ensure proper cross-origin requests
3. **SSL/TLS**: Secure token transmission
4. **Rate Limiting**: Protect authentication endpoints
5. **Logging**: Comprehensive auth activity logging

## Security Best Practices

1. **JWT Security**: Secure token storage, short expiration times
2. **Password Hashing**: Strong bcrypt hashing with proper salts
3. **Input Validation**: Server-side validation for all inputs
4. **XSS Protection**: Sanitize user inputs, secure cookies
5. **CSRF Protection**: Token-based CSRF protection
6. **Rate Limiting**: Prevent brute force attacks

## Performance Optimization

1. **Token Caching**: Efficient token validation caching
2. **Lazy Loading**: Code splitting for auth components
3. **Optimistic UI**: Immediate feedback for user actions
4. **Bundle Optimization**: Tree-shaking unused auth code

This comprehensive implementation provides a robust, error-resistant authentication system with profile-based rendering that seamlessly integrates with your existing Lovable Cline application architecture.
