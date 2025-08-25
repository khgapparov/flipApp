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
          try {
            const userProfile = await enhancedAuthService.getCurrentUser();
            setUser({
              ...userProfile,
              isAnonymous,
              token
            });
          } catch (profileError) {
            console.error('Failed to get user profile:', profileError);
            // Token is valid but profile fetch failed, use basic user info
            setUser({
              id: userId,
              username: 'User',
              email: '',
              isAnonymous,
              token
            });
          }
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
      
      // Try to get user profile, but fallback to basic info if it fails
      let userProfile;
      try {
        userProfile = await enhancedAuthService.getCurrentUser();
      } catch (profileError) {
        console.warn('Failed to get user profile, using basic info:', profileError);
        userProfile = {
          id: response.user_id,
          username: username,
          email: ''
        };
      }
      
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
      const response = await enhancedAuthService.register({ username, email, password });
      
      // Try to get user profile, but fallback to basic info if it fails
      let userProfile;
      try {
        userProfile = await enhancedAuthService.getCurrentUser();
      } catch (profileError) {
        console.warn('Failed to get user profile, using basic info:', profileError);
        userProfile = {
          id: response.user_id,
          username: username,
          email: email
        };
      }
      
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
        username: response.user?.username || 'Guest',
        email: response.user?.email || '',
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
