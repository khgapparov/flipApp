// API service for communicating with Spring Boot backend
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Helper function to handle API requests
const apiRequest = async (endpoint, options = {}) => {
  const token = localStorage.getItem('auth_token');
  
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
    console.log('API Request with token:', endpoint, 'Token present');
  } else {
    console.log('API Request without token:', endpoint);
  }

  const config = {
    ...options,
    headers,
  };

  try {
    console.log('Making API request to:', `${API_BASE_URL}${endpoint}`);
    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    
    console.log('API Response:', endpoint, 'Status:', response.status);
    
    if (response.status === 401) {
      console.log('401 Unauthorized - attempting token refresh');
      // Token is invalid or expired, clear it and try to get a new one
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_id');
      
      // Try to get a new token by redirecting to login
      try {
        console.log('Token expired - redirecting to login');
        // Clear storage and redirect to login
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user_id');
        throw new Error('Session expired. Please login again.');
      } catch (authError) {
        console.error('Failed to refresh token:', authError);
        throw new Error('Authentication failed. Please refresh the page and login again.');
      }
    }
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('API request failed:', error);
    throw error;
  }
};

// Authentication services
export const authService = {
  // Login with username/password
  login: async (username, password) => {
    try {
      const response = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
        localStorage.removeItem('is_anonymous'); // Clear anonymous flag for regular login
      }
      
      return response;
    } catch (error) {
      throw new Error('Login failed');
    }
  },

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
  },

  // Register new user
  register: async (username, email, password) => {
    try {
      const response = await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, email, password })
      });
      
      if (response.access_token) {
        localStorage.setItem('auth_token', response.access_token);
        localStorage.setItem('user_id', response.user_id);
      }
      
      return response;
    } catch (error) {
      throw new Error('Registration failed');
    }
  },

  // Validate existing token
  validateToken: async (token) => {
    try {
      const response = await apiRequest('/auth/validate-token', {
        method: 'POST',
        body: JSON.stringify({ token })
      });
      return response.valid;
    } catch (error) {
      return false;
    }
  },

  // Get current user info
  getCurrentUser: async () => {
    try {
      return await apiRequest('/users/me');
    } catch (error) {
      throw new Error('Failed to get user information');
    }
  }
};

// Project services
export const projectService = {
  // Get project by ID
  getProject: async (projectId) => {
    return await apiRequest(`/projects/${projectId}`);
  },

  // Get all projects for current user
  getUserProjects: async () => {
    return await apiRequest('/projects');
  },

  // Subscribe to project updates (WebSocket or polling)
  subscribeToProject: (projectId, callback) => {
    // For real-time updates, we'll use polling for now
    // In a production app, you might use WebSockets
    const interval = setInterval(async () => {
      try {
        const project = await projectService.getProject(projectId);
        callback(project);
      } catch (error) {
        console.error('Error polling project:', error);
      }
    }, 5000); // Poll every 5 seconds

    return () => clearInterval(interval);
  }
};

// Updates services
export const updatesService = {
  // Get updates for a project
  getUpdates: async (projectId) => {
    return await apiRequest(`/projects/${projectId}/updates`);
  },

  // Create a new update (admin only)
  createUpdate: async (projectId, updateData) => {
    return await apiRequest(`/projects/${projectId}/updates`, {
      method: 'POST',
      body: JSON.stringify(updateData)
    });
  },

  // Subscribe to updates (polling)
  subscribeToUpdates: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const updates = await updatesService.getUpdates(projectId);
        callback(updates);
      } catch (error) {
        console.error('Error polling updates:', error);
      }
    }, 5000);

    return () => clearInterval(interval);
  }
};

// Gallery services
export const galleryService = {
  // Get gallery images for a project
  getGallery: async (projectId) => {
    return await apiRequest(`/projects/${projectId}/gallery`);
  },

  // Upload a new image (admin only)
  uploadImage: async (projectId, file, caption, room = "General", stage = "During") => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('caption', caption);
    formData.append('room', room);
    formData.append('stage', stage);

    const token = localStorage.getItem('auth_token');
    
    const response = await fetch(`${API_BASE_URL}/projects/${projectId}/gallery`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  },

  // Subscribe to gallery updates (polling)
  subscribeToGallery: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const gallery = await galleryService.getGallery(projectId);
        callback(gallery);
      } catch (error) {
        console.error('Error polling gallery:', error);
      }
    }, 5000);

    return () => clearInterval(interval);
  }
};

// Chat services
export const chatService = {
  // Get chat messages for a project
  getMessages: async (projectId) => {
    return await apiRequest(`/projects/${projectId}/chat`);
  },

  // Send a new message
  sendMessage: async (projectId, messageData) => {
    return await apiRequest(`/projects/${projectId}/chat`, {
      method: 'POST',
      body: JSON.stringify(messageData)
    });
  },

  // Subscribe to chat messages (polling)
  subscribeToChat: (projectId, callback) => {
    const interval = setInterval(async () => {
      try {
        const messages = await chatService.getMessages(projectId);
        callback(messages);
      } catch (error) {
        console.error('Error polling chat:', error);
      }
    }, 3000); // Poll more frequently for chat

    return () => clearInterval(interval);
  }
};
