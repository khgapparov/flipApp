// Custom hooks for API data fetching with TanStack Query
import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-hot-toast';
import { 
  enhancedProjectService, 
  enhancedUpdatesService, 
  enhancedGalleryService, 
  enhancedChatService,
  subscribeToEvent
} from '../services/enhancedApi';

// Query keys for consistent caching
export const queryKeys = {
  projects: ['projects'],
  project: (id) => ['projects', id],
  updates: (projectId) => ['updates', projectId],
  gallery: (projectId) => ['gallery', projectId],
  chat: (projectId) => ['chat', projectId],
  user: ['user']
};

// Custom hook for projects
export const useProjects = (params = {}) => {
  return useQuery({
    queryKey: [...queryKeys.projects, params],
    queryFn: () => enhancedProjectService.getProjects(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 2,
    onError: (error) => {
      toast.error(error.message || 'Failed to load projects');
    }
  });
};

export const useProject = (projectId) => {
  return useQuery({
    queryKey: queryKeys.project(projectId),
    queryFn: () => enhancedProjectService.getProject(projectId),
    enabled: !!projectId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    onError: (error) => {
      toast.error(error.message || 'Failed to load project');
    }
  });
};

export const useCreateProject = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: enhancedProjectService.createProject,
    onSuccess: (data) => {
      // Invalidate projects list
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
      toast.success('Project created successfully');
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to create project');
    }
  });
};

export const useUpdateProject = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, projectData, version }) => 
      enhancedProjectService.updateProject(projectId, projectData, version),
    onSuccess: (data, variables) => {
      // Update the specific project in cache
      queryClient.setQueryData(queryKeys.project(variables.projectId), data);
      // Invalidate projects list
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to update project');
    }
  });
};

export const useDeleteProject = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: enhancedProjectService.deleteProject,
    onSuccess: (data, projectId) => {
      // Remove project from cache
      queryClient.removeQueries({ queryKey: queryKeys.project(projectId) });
      // Invalidate projects list
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to delete project');
    }
  });
};

// Custom hook for project updates
export const useUpdates = (projectId, params = {}) => {
  return useQuery({
    queryKey: [...queryKeys.updates(projectId), params],
    queryFn: () => enhancedUpdatesService.getUpdates(projectId, params),
    enabled: !!projectId,
    staleTime: 1 * 60 * 1000, // 1 minute
    onError: (error) => {
      toast.error(error.message || 'Failed to load updates');
    }
  });
};

export const useCreateUpdate = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, updateData }) => 
      enhancedUpdatesService.createUpdate(projectId, updateData),
    onSuccess: (data, variables) => {
      // Invalidate updates for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.updates(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to create update');
    }
  });
};

export const useDeleteUpdate = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, updateId }) => 
      enhancedUpdatesService.deleteUpdate(projectId, updateId),
    onSuccess: (data, variables) => {
      // Invalidate updates for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.updates(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to delete update');
    }
  });
};

// Custom hook for gallery
export const useGallery = (projectId, params = {}) => {
  return useQuery({
    queryKey: [...queryKeys.gallery(projectId), params],
    queryFn: () => enhancedGalleryService.getGallery(projectId, params),
    enabled: !!projectId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    onError: (error) => {
      toast.error(error.message || 'Failed to load gallery');
    }
  });
};

export const useUploadImage = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, file, metadata }) => 
      enhancedGalleryService.uploadImage(projectId, file, metadata),
    onSuccess: (data, variables) => {
      // Invalidate gallery for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.gallery(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to upload image');
    }
  });
};

export const useDeleteImage = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, imageId }) => 
      enhancedGalleryService.deleteImage(projectId, imageId),
    onSuccess: (data, variables) => {
      // Invalidate gallery for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.gallery(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to delete image');
    }
  });
};

// Custom hook for chat
export const useChatMessages = (projectId, params = {}) => {
  return useQuery({
    queryKey: [...queryKeys.chat(projectId), params],
    queryFn: () => enhancedChatService.getMessages(projectId, params),
    enabled: !!projectId,
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // Auto-refresh every minute
    onError: (error) => {
      toast.error(error.message || 'Failed to load messages');
    }
  });
};

export const useSendMessage = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, messageData }) => 
      enhancedChatService.sendMessage(projectId, messageData),
    onSuccess: (data, variables) => {
      // Invalidate chat messages for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.chat(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to send message');
    }
  });
};

export const useDeleteMessage = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ projectId, messageId }) => 
      enhancedChatService.deleteMessage(projectId, messageId),
    onSuccess: (data, variables) => {
      // Invalidate chat messages for this project
      queryClient.invalidateQueries({ 
        queryKey: queryKeys.chat(variables.projectId) 
      });
    },
    onError: (error) => {
      toast.error(error.message || 'Failed to delete message');
    }
  });
};

// Real-time subscription hooks
export const useRealTimeUpdates = (entity, action, callback) => {
  const callbackRef = useRef(callback);
  
  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    const unsubscribe = subscribeToEvent(entity, action, (data) => {
      callbackRef.current(data);
    });
    
    return unsubscribe;
  }, [entity, action]);
};

// Optimistic update utilities
export const useOptimisticUpdate = (queryKey, updateFn) => {
  const queryClient = useQueryClient();
  
  return async (variables) => {
    // Cancel any outgoing refetches
    await queryClient.cancelQueries({ queryKey });
    
    // Snapshot the previous value
    const previousData = queryClient.getQueryData(queryKey);
    
    try {
      // Optimistically update to the new value
      queryClient.setQueryData(queryKey, (old) => updateFn(old, variables));
      
      // Return a context object with the snapshotted value
      return { previousData };
    } catch (error) {
      // If the mutation fails, use the context returned from onMutate 
      // to roll back
      return { previousData };
    }
  };
};

export const useOptimisticMutation = (mutationFn, queryKey, updateFn) => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn,
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey });
      
      const previousData = queryClient.getQueryData(queryKey);
      
      queryClient.setQueryData(queryKey, (old) => updateFn(old, variables));
      
      return { previousData };
    },
    onError: (error, variables, context) => {
      queryClient.setQueryData(queryKey, context.previousData);
      toast.error(error.message || 'Operation failed');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    }
  });
};

// Loading state utilities
export const useLoadingState = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const execute = async (asyncFn) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const result = await asyncFn();
      return result;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };
  
  return { isLoading, error, execute };
};

export default {
  useProjects,
  useProject,
  useCreateProject,
  useUpdateProject,
  useDeleteProject,
  useUpdates,
  useCreateUpdate,
  useDeleteUpdate,
  useGallery,
  useUploadImage,
  useDeleteImage,
  useChatMessages,
  useSendMessage,
  useDeleteMessage,
  useRealTimeUpdates,
  useOptimisticUpdate,
  useOptimisticMutation,
  useLoadingState,
  queryKeys
};
