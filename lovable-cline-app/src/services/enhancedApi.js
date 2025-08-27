// Enhanced API service with comprehensive error handling, loading states, and real-time capabilities
import { toast } from 'react-hot-toast';
import { format } from 'date-fns';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';

// WebSocket connection management
let socket = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

// Event listeners for real-time updates
const eventListeners = {
  project: new Map(),
  update: new Map(),
  gallery: new Map(),
  chat: new Map()
};

// Initialize WebSocket connection
export const initWebSocket = (token) => {
  console.log('WebSocket initialization skipped - no WebSocket server configured');
  // WebSocket connection disabled temporarily to prevent connection errors
  // The backend doesn't have WebSocket support configured yet
  return null;
};

// Handle incoming WebSocket messages
const handleWebSocketMessage = (data) => {
  const { type, entity, action, payload } = data;
  
  switch (type) {
    case 'CREATE':
    case 'UPDATE':
    case 'DELETE':
      const listeners = eventListeners[entity]?.get(action);
      if (listeners) {
        listeners.forEach(callback => callback(payload));
      }
      break;
    case 'ERROR':
      toast.error(payload.message || 'WebSocket error occurred');
      break;
    default:
      console.log('Unknown WebSocket message type:', type);
  }
};

// Subscribe to real-time events
export const subscribeToEvent = (entity, action, callback) => {
  if (!eventListeners[entity]) {
    eventListeners[entity] = new Map();
  }
  
  if (!eventListeners[entity].has(action)) {
    eventListeners[entity].set(action, new Set());
  }
  
  eventListeners[entity].get(action).add(callback);
  
  return () => {
    if (eventListeners[entity]?.get(action)?.has(callback)) {
      eventListeners[entity].get(action).delete(callback);
    }
  };
};

// Check if backend server is reachable
const isBackendReachable = async () => {
  try {
    // Use the actual test endpoint that exists with GET instead of HEAD
    // HEAD requests may be blocked by some security configurations
    const response = await fetch(`${API_BASE_URL}/test/datetime`, {
      method: 'GET',
      signal: AbortSignal.timeout(3000) // 3 second timeout
    });
    // Any response (including 404) means the server is reachable
    // Only network errors (like failed to fetch) indicate the server is down
    return true;
  } catch (error) {
    console.warn('Backend server is not reachable:', error.message);
    return false;
  }
};

// Enhanced API request function with comprehensive error handling
export const enhancedApiRequest = async (endpoint, options = {}) => {
  const token = localStorage.getItem('auth_token');
  const requestId = Math.random().toString(36).substr(2, 9);
  
  const headers = {
    'Content-Type': 'application/json',
    'X-Request-ID': requestId,
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
    signal: options.signal || (options.timeout ? AbortSignal.timeout(options.timeout) : undefined),
  };

  try {
    console.log(`[API] ${options.method || 'GET'} ${endpoint}`, { requestId });
    
    // Check if backend is reachable before making the request
    if (!await isBackendReachable()) {
      throw new ApiError('Backend server is not reachable. Please check if the server is running.', 0);
    }
    
    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    
    console.log(`[API] Response for ${endpoint}:`, response.status, { requestId });
    
    // Handle authentication errors
    if (response.status === 401) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_id');
      localStorage.removeItem('is_anonymous');
      toast.error('Session expired. Please login again.');
      window.location.href = '/login';
      throw new Error('Authentication failed');
    }
    
    // Handle rate limiting
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After');
      toast.error(`Too many requests. Please try again in ${retryAfter || 'a few'} seconds.`);
      throw new Error('Rate limit exceeded');
    }
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new ApiError(
        errorData.message || `HTTP error! status: ${response.status}`,
        response.status,
        errorData
      );
    }
    
    return await response.json();
  } catch (error) {
    console.error(`[API] Request failed for ${endpoint}:`, error, { requestId });
    
    if (error.name === 'AbortError') {
      throw new ApiError('Request timeout', 408);
    }
    
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new ApiError('Network error. Please check your connection and ensure the backend server is running.', 0);
    }
    
    throw error;
  }
};

// Custom error class for API errors
export class ApiError extends Error {
  constructor(message, status, data = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

// Enhanced authentication services
export const enhancedAuthService = {
  login: async (username, password) => {
    try {
      const response = await enhancedApiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
        // WebSocket initialization disabled - backend doesn't support WebSocket yet
        // initWebSocket(response.access_token);
      }
      
      toast.success('Login successful');
      return response;
    } catch (error) {
      toast.error(error.message || 'Login failed');
      throw error;
    }
  },

  register: async (userData) => {
    try {
      const response = await enhancedApiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify(userData)
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
        // WebSocket initialization disabled - backend doesn't support WebSocket yet
        // initWebSocket(response.access_token);
      }
      
      toast.success('Registration successful');
      return response;
    } catch (error) {
      toast.error(error.message || 'Registration failed');
      throw error;
    }
  },

  // Login with existing token (for token validation)
  loginWithToken: async (token) => {
    try {
      const response = await enhancedApiRequest('/auth/validate-token', {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      
      if (response.valid) {
        localStorage.setItem('auth_token', token);
        // WebSocket initialization disabled - backend doesn't support WebSocket yet
        // initWebSocket(token);
        return response;
      } else {
        throw new Error('Invalid token');
      }
    } catch (error) {
      console.error('Token login failed:', error);
      throw new Error('Failed to login with token');
    }
  },

  // Anonymous login - creates a temporary guest user
  anonymousLogin: async () => {
    try {
      const response = await enhancedApiRequest('/auth/anonymous', {
        method: 'POST'
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
        localStorage.setItem('is_anonymous', 'true'); // Mark as anonymous user
        // WebSocket initialization disabled - backend doesn't support WebSocket yet
        // initWebSocket(response.access_token);
      }
      
      toast.success('Anonymous login successful');
      return response;
    } catch (error) {
      console.error('Anonymous login failed:', error);
      toast.error('Failed to login anonymously');
      throw error;
    }
  },

  logout: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('is_anonymous');
    if (socket) {
      socket.close();
      socket = null;
    }
    toast.success('Logged out successfully');
  },

  // Validate JWT token
  validateToken: async (token) => {
    try {
      const response = await enhancedApiRequest('/auth/validate-token', {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      return response.valid === true;
    } catch (error) {
      console.error('Token validation failed:', error);
      return false;
    }
  },

  // Get current user profile
  getCurrentUser: async () => {
    try {
      const response = await enhancedApiRequest('/users/me');
      return response;
    } catch (error) {
      console.error('Failed to get user profile:', error);
      throw new Error('Failed to load user profile');
    }
  },

  // Update user profile
  updateProfile: async (profileData) => {
    try {
      const response = await enhancedApiRequest('/users/profile', {
        method: 'PUT',
        body: JSON.stringify(profileData)
      });
      toast.success('Profile updated successfully');
      return response;
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw new Error('Failed to update profile');
    }
  },

  // Change password
  changePassword: async (currentPassword, newPassword) => {
    try {
      const response = await enhancedApiRequest('/users/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword })
      });
      toast.success('Password changed successfully');
      return response;
    } catch (error) {
      console.error('Failed to change password:', error);
      throw new Error('Failed to change password');
    }
  }
};

// Enhanced project services with comprehensive CRUD operations
export const enhancedProjectService = {
  // Get all projects with pagination and filtering
  getProjects: async (params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value);
      }
    });
    
    return await enhancedApiRequest(`/projects?${queryParams}`);
  },

  // Get project by ID with detailed information
  getProject: async (projectId) => {
    return await enhancedApiRequest(`/projects/${projectId}`);
  },

  // Create new project with validation
  createProject: async (projectData) => {
    const response = await enhancedApiRequest('/projects', {
      method: 'POST',
      body: JSON.stringify({
        ...projectData,
        createdAt: format(new Date(), 'yyyy-MM-dd\'T\'HH:mm:ss.SSS')
      })
    });
    
    toast.success('Project created successfully');
    return response;
  },

  // Update project with optimistic locking
  updateProject: async (projectId, projectData, version) => {
    const headers = version ? { 'If-Match': version } : {};
    
    const response = await enhancedApiRequest(`/projects/${projectId}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(projectData)
    });
    
    toast.success('Project updated successfully');
    return response;
  },

  // Soft delete project
  deleteProject: async (projectId) => {
    const response = await enhancedApiRequest(`/projects/${projectId}`, {
      method: 'DELETE'
    });
    
    toast.success('Project deleted successfully');
    return response;
  },

  // Bulk operations
  bulkDeleteProjects: async (projectIds) => {
    const response = await enhancedApiRequest('/projects/bulk-delete', {
      method: 'POST',
      body: JSON.stringify({ projectIds })
    });
    
    toast.success(`${projectIds.length} projects deleted successfully`);
    return response;
  },

  // Subscribe to project updates (polling)
  subscribeToProject: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const project = await enhancedProjectService.getProject(projectId);
        callback(project);
      } catch (error) {
        console.error('Error polling project:', error);
      }
    }, 5000); // Poll every 5 seconds

    return () => clearInterval(interval);
  }
};

// Enhanced updates services
export const enhancedUpdatesService = {
  getUpdates: async (projectId, params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value);
      }
    });
    
    return await enhancedApiRequest(`/projects/${projectId}/updates?${queryParams}`);
  },

  createUpdate: async (projectId, updateData) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/updates`, {
      method: 'POST',
      body: JSON.stringify({
        ...updateData,
        timestamp: format(new Date(), 'yyyy-MM-dd\'T\'HH:mm:ss.SSS')
      })
    });
    
    toast.success('Update created successfully');
    return response;
  },

  updateUpdate: async (projectId, updateId, updateData) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/updates/${updateId}`, {
      method: 'PUT',
      body: JSON.stringify(updateData)
    });
    
    toast.success('Update modified successfully');
    return response;
  },

  deleteUpdate: async (projectId, updateId) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/updates/${updateId}`, {
      method: 'DELETE'
    });
    
    toast.success('Update deleted successfully');
    return response;
  },

  // Subscribe to updates (polling)
  subscribeToUpdates: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const updates = await enhancedUpdatesService.getUpdates(projectId);
        callback(updates);
      } catch (error) {
        console.error('Error polling updates:', error);
      }
    }, 5000);

    return () => clearInterval(interval);
  }
};

// Enhanced gallery services
export const enhancedGalleryService = {
  getGallery: async (projectId, params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value);
      }
    });
    
    return await enhancedApiRequest(`/projects/${projectId}/gallery?${queryParams}`);
  },

  uploadImage: async (projectId, file, metadata) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('caption', metadata.caption || '');
    formData.append('room', metadata.room || 'General');
    formData.append('stage', metadata.stage || 'During');
    
    const token = localStorage.getItem('auth_token');
    
    const response = await fetch(`${API_BASE_URL}/projects/${projectId}/gallery`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new ApiError(errorData.message || 'Upload failed', response.status, errorData);
    }

    const result = await response.json();
    toast.success('Image uploaded successfully');
    return result;
  },

  updateImage: async (projectId, imageId, metadata) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/gallery/${imageId}`, {
      method: 'PUT',
      body: JSON.stringify(metadata)
    });
    
    toast.success('Image updated successfully');
    return response;
  },

  deleteImage: async (projectId, imageId) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/gallery/${imageId}`, {
      method: 'DELETE'
    });
    
    toast.success('Image deleted successfully');
    return response;
  },

  bulkDeleteImages: async (projectId, imageIds) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/gallery/bulk-delete`, {
      method: 'POST',
      body: JSON.stringify({ imageIds })
    });
    
    toast.success(`${imageIds.length} images deleted successfully`);
    return response;
  },

  // Subscribe to gallery updates (polling)
  subscribeToGallery: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const gallery = await enhancedGalleryService.getGallery(projectId);
        callback(gallery);
      } catch (error) {
        console.error('Error polling gallery:', error);
      }
    }, 5000);

    return () => clearInterval(interval);
  }
};

// Enhanced chat services
export const enhancedChatService = {
  getMessages: async (projectId, params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        queryParams.append(key, value);
      }
    });
    
    return await enhancedApiRequest(`/projects/${projectId}/chat?${queryParams}`);
  },

  sendMessage: async (projectId, messageData) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/chat`, {
      method: 'POST',
      body: JSON.stringify({
        ...messageData,
        createdAt: format(new Date(), 'yyyy-MM-dd\'T\'HH:mm:ss.SSS')
      })
    });
    
    return response;
  },

  editMessage: async (projectId, messageId, messageData) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/chat/${messageId}`, {
      method: 'PUT',
      body: JSON.stringify(messageData)
    });
    
    toast.success('Message edited successfully');
    return response;
  },

  deleteMessage: async (projectId, messageId) => {
    const response = await enhancedApiRequest(`/projects/${projectId}/chat/${messageId}`, {
      method: 'DELETE'
    });
    
    toast.success('Message deleted successfully');
    return response;
  },

  markAsRead: async (projectId, messageIds) => {
    return await enhancedApiRequest(`/projects/${projectId}/chat/mark-read`, {
      method: 'POST',
      body: JSON.stringify({ messageIds })
    });
  },

  // Subscribe to chat messages (polling)
  subscribeToChat: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const messages = await enhancedChatService.getMessages(projectId);
        callback(messages);
      } catch (error) {
        console.error('Error polling chat:', error);
      }
    }, 3000); // Poll more frequently for chat

    return () => clearInterval(interval);
  }
};

// Utility functions
export const apiUtils = {
  // Retry mechanism for failed requests
  withRetry: async (fn, maxRetries = 3, delay = 1000) => {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await fn();
      } catch (error) {
        if (attempt === maxRetries) throw error;
        await new Promise(resolve => setTimeout(resolve, delay * attempt));
      }
    }
  },

  // Debounce API calls
  debounce: (fn, delay) => {
    let timeoutId;
    return (...args) => {
      clearTimeout(timeoutId);
      return new Promise((resolve) => {
        timeoutId = setTimeout(() => resolve(fn(...args)), delay);
      });
    };
  },

  // Validate response schema
  validateResponse: (response, schema) => {
    // Simple validation - in real app, use a validation library like Zod
    const missingFields = [];
    for (const field of schema.required || []) {
      if (response[field] === undefined) {
        missingFields.push(field);
      }
    }
    
    if (missingFields.length > 0) {
      throw new ApiError(`Invalid response: missing fields ${missingFields.join(', ')}`, 500);
    }
    
    return response;
  }
};

export default {
  enhancedApiRequest,
  enhancedAuthService,
  enhancedProjectService,
  enhancedUpdatesService,
  enhancedGalleryService,
  enhancedChatService,
  apiUtils,
  initWebSocket,
  subscribeToEvent,
  ApiError
};
