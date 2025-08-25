import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useCreateProject, useUpdateProject, useProject } from '../../hooks/useApi';
import { validateForm, projectSchema } from '../../utils/validation';
import FormField from '../forms/FormField';
import FileUpload from '../forms/FileUpload';
import { toast } from 'react-hot-toast';

const CreateProjectModal = ({ 
  isOpen, 
  onClose, 
  projectId = null,
  onSuccess 
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  
  const { data: existingProject } = useProject(projectId);
  const createProjectMutation = useCreateProject();
  const updateProjectMutation = useUpdateProject();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    control,
    setValue,
    watch
  } = useForm({
    defaultValues: projectId && existingProject ? {
      name: existingProject.name || '',
      address: existingProject.address || '',
      budget: '', // Budget field not available in API response
      startDate: existingProject.startDate ? new Date(existingProject.startDate).toISOString().split('T')[0] : '',
      endDate: existingProject.estimatedEndDate ? new Date(existingProject.estimatedEndDate).toISOString().split('T')[0] : '',
      description: '', // Description field not available in API response
      status: existingProject.status || 'planning'
    } : {
      name: '',
      address: '',
      budget: '',
      startDate: '',
      endDate: '',
      description: '',
      status: 'planning'
    }
  });

  // Reset form when modal opens/closes or project changes
  React.useEffect(() => {
    if (isOpen && projectId && existingProject) {
      reset({
        name: existingProject.name || '',
        address: existingProject.address || '',
        budget: '', // Budget field not available in API response
        startDate: existingProject.startDate ? new Date(existingProject.startDate).toISOString().split('T')[0] : '',
        endDate: existingProject.estimatedEndDate ? new Date(existingProject.estimatedEndDate).toISOString().split('T')[0] : '',
        description: '', // Description field not available in API response
        status: existingProject.status || 'planning'
      });
    } else if (isOpen && !projectId) {
      reset({
        name: '',
        address: '',
        budget: '',
        startDate: '',
        endDate: '',
        description: '',
        status: 'planning'
      });
    }
  }, [isOpen, projectId, existingProject, reset]);

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    setValidationErrors({});

    try {
      // Client-side validation
      const clientErrors = validateForm(data, projectSchema);
      if (Object.keys(clientErrors).length > 0) {
        setValidationErrors(clientErrors);
        setIsSubmitting(false);
        return;
      }

      // Prepare project data for submission (JSON format)
      const projectData = {
        name: data.name,
        address: data.address,
        startDate: data.startDate,
        estimatedEndDate: data.endDate, // Map endDate to estimatedEndDate
        status: data.status,
        // Note: budget and description fields are not supported by backend
      };

      if (projectId) {
        // Update existing project
        await updateProjectMutation.mutateAsync({
          projectId,
          projectData,
          version: existingProject.version || 0
        });
        toast.success('Project updated successfully');
      } else {
        // Create new project
        await createProjectMutation.mutateAsync(projectData);
        toast.success('Project created successfully');
      }

      onSuccess?.();
      onClose();
      reset();

    } catch (error) {
      console.error('Error submitting project:', error);
      
      if (error.response?.data?.errors) {
        // Server-side validation errors
        setValidationErrors(error.response.data.errors);
      } else {
        toast.error(error.message || 'Failed to save project');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    reset();
    setValidationErrors({});
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-semibold text-gray-900">
            {projectId ? 'Edit Project' : 'Create New Project'}
          </h3>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField
              name="name"
              label="Project Name"
              type="text"
              placeholder="Enter project name"
              required
              rules={{
                required: 'Project name is required',
                minLength: { value: 3, message: 'Minimum 3 characters required' },
                maxLength: { value: 100, message: 'Maximum 100 characters allowed' }
              }}
            />

            <FormField
              name="address"
              label="Project Address"
              type="text"
              placeholder="Enter project address"
              required
              rules={{
                required: 'Address is required',
                minLength: { value: 5, message: 'Minimum 5 characters required' }
              }}
            />
          </div>

          <FormField
            name="description"
            label="Description"
            type="textarea"
            placeholder="Describe the project..."
            rules={{
              maxLength: { value: 1000, message: 'Maximum 1000 characters allowed' }
            }}
          />

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <FormField
              name="budget"
              label="Budget ($)"
              type="number"
              placeholder="0.00"
              rules={{
                min: { value: 0, message: 'Budget cannot be negative' }
              }}
            />

            <FormField
              name="startDate"
              label="Start Date"
              type="date"
              required
              rules={{
                required: 'Start date is required',
                validate: (value) => {
                  if (value && watch('endDate') && new Date(value) > new Date(watch('endDate'))) {
                    return 'Start date must be before end date';
                  }
                  return true;
                }
              }}
            />

            <FormField
              name="endDate"
              label="End Date"
              type="date"
              rules={{
                validate: (value) => {
                  if (value && watch('startDate') && new Date(value) < new Date(watch('startDate'))) {
                    return 'End date must be after start date';
                  }
                  return true;
                }
              }}
            />
          </div>

          <FormField
            name="status"
            label="Status"
            type="select"
            required
            rules={{ required: 'Status is required' }}
          >
            <option value="">Select status</option>
            <option value="planning">Planning</option>
            <option value="in_progress">In Progress</option>
            <option value="on_hold">On Hold</option>
            <option value="completed">Completed</option>
            <option value="cancelled">Cancelled</option>
          </FormField>

          <FileUpload
            name="documents"
            label="Project Documents"
            multiple={true}
            maxFiles={5}
            maxSizeMB={10}
            allowedTypes={[
              'application/pdf',
              'application/msword',
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
              'image/jpeg',
              'image/png'
            ]}
          />

          {/* Display validation errors */}
          {Object.keys(validationErrors).length > 0 && (
            <div className="bg-red-50 border border-red-200 rounded-md p-4">
              <h4 className="text-sm font-medium text-red-800 mb-2">
                Please fix the following errors:
              </h4>
              <ul className="text-sm text-red-600 space-y-1">
                {Object.entries(validationErrors).map(([field, error]) => (
                  <li key={field}>• {error}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex justify-end space-x-3 pt-4 border-t">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <div className="flex items-center">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  {projectId ? 'Updating...' : 'Creating...'}
                </div>
              ) : (
                projectId ? 'Update Project' : 'Create Project'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateProjectModal;
