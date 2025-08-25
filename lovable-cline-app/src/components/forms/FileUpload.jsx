import React, { useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { useFormContext, Controller } from 'react-hook-form';
import { validateFiles } from '../../utils/validation';

const FileUpload = ({
  name,
  label,
  multiple = false,
  maxFiles = 10,
  maxSizeMB = 10,
  allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  disabled = false,
  className = '',
  onFilesChange,
  ...props
}) => {
  const [uploadProgress, setUploadProgress] = useState({});
  const [dragOver, setDragOver] = useState(false);
  
  const {
    control,
    setValue,
    formState: { errors },
    watch
  } = useFormContext();

  const files = watch(name) || [];

  const onDrop = useCallback((acceptedFiles, rejectedFiles) => {
    // Validate files
    const validationErrors = validateFiles(acceptedFiles, {
      maxFiles: multiple ? maxFiles : 1,
      maxSizeMB,
      allowedTypes
    });

    if (validationErrors.length > 0) {
      // Handle validation errors
      console.error('File validation errors:', validationErrors);
      return;
    }

    if (rejectedFiles.length > 0) {
      console.error('Rejected files:', rejectedFiles);
      return;
    }

    const newFiles = multiple ? [...files, ...acceptedFiles] : acceptedFiles;
    setValue(name, newFiles);
    
    if (onFilesChange) {
      onFilesChange(newFiles);
    }
  }, [files, multiple, maxFiles, maxSizeMB, allowedTypes, name, setValue, onFilesChange]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple,
    maxFiles: multiple ? maxFiles : 1,
    maxSize: maxSizeMB * 1024 * 1024,
    accept: allowedTypes.reduce((acc, type) => {
      acc[type] = [];
      return acc;
    }, {}),
    disabled,
    onDragEnter: () => setDragOver(true),
    onDragLeave: () => setDragOver(false),
    onDropAccepted: () => setDragOver(false),
    onDropRejected: () => setDragOver(false),
  });

  const removeFile = (index) => {
    const newFiles = files.filter((_, i) => i !== index);
    setValue(name, newFiles);
    
    if (onFilesChange) {
      onFilesChange(newFiles);
    }
  };

  const updateFileProgress = (fileName, progress) => {
    setUploadProgress(prev => ({
      ...prev,
      [fileName]: progress
    }));
  };

  const getDropzoneClasses = () => {
    const baseClasses = 'border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-all duration-200';
    const dragActiveClasses = isDragActive || dragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300';
    const errorClasses = errors[name] ? 'border-red-500 bg-red-50' : '';
    const disabledClasses = disabled ? 'bg-gray-100 cursor-not-allowed opacity-50' : 'hover:border-gray-400';
    
    return `${baseClasses} ${dragActiveClasses} ${errorClasses} ${disabledClasses}`;
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className={`mb-4 ${className}`}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-2">
          {label}
        </label>
      )}

      <Controller
        name={name}
        control={control}
        render={({ field }) => (
          <>
            <div
              {...getRootProps()}
              className={getDropzoneClasses()}
            >
              <input {...getInputProps()} {...props} />
              
              <div className="space-y-2">
                <svg
                  className="mx-auto h-12 w-12 text-gray-400"
                  stroke="currentColor"
                  fill="none"
                  viewBox="0 0 48 48"
                  aria-hidden="true"
                >
                  <path
                    d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                
                <div className="flex text-sm text-gray-600">
                  <p className="pl-1">
                    {isDragActive ? (
                      'Drop the files here...'
                    ) : (
                      <>
                        Drag and drop files here, or{' '}
                        <span className="font-medium text-blue-600 hover:text-blue-500">
                          click to browse
                        </span>
                      </>
                    )}
                  </p>
                </div>
                
                <p className="text-xs text-gray-500">
                  {multiple ? `Up to ${maxFiles} files` : 'Single file'} • Max {maxSizeMB}MB each • {allowedTypes.join(', ')}
                </p>
              </div>
            </div>

            {files.length > 0 && (
              <div className="mt-4 space-y-2">
                <h4 className="text-sm font-medium text-gray-700">
                  Selected files ({files.length})
                </h4>
                
                {files.map((file, index) => (
                  <div
                    key={`${file.name}-${index}`}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-md border"
                  >
                    <div className="flex items-center space-x-3 flex-1 min-w-0">
                      {file.type.startsWith('image/') ? (
                        <img
                          src={URL.createObjectURL(file)}
                          alt={file.name}
                          className="h-10 w-10 object-cover rounded"
                        />
                      ) : (
                        <div className="h-10 w-10 bg-gray-200 rounded flex items-center justify-center">
                          <span className="text-xs text-gray-600">
                            {file.name.split('.').pop()?.toUpperCase()}
                          </span>
                        </div>
                      )}
                      
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {file.name}
                        </p>
                        <p className="text-xs text-gray-500">
                          {formatFileSize(file.size)}
                        </p>
                        
                        {uploadProgress[file.name] !== undefined && (
                          <div className="w-full bg-gray-200 rounded-full h-1.5 mt-1">
                            <div
                              className="bg-blue-600 h-1.5 rounded-full transition-all duration-300"
                              style={{ width: `${uploadProgress[file.name]}%` }}
                            />
                          </div>
                        )}
                      </div>
                    </div>
                    
                    {!disabled && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeFile(index);
                        }}
                        className="ml-2 p-1 text-gray-400 hover:text-red-500 transition-colors"
                      >
                        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      />

      {errors[name] && (
        <p className="mt-1 text-sm text-red-600">
          {errors[name].message}
        </p>
      )}
    </div>
  );
};

export default FileUpload;
