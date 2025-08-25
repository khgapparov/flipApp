// Form validation utilities with comprehensive validation rules
export const validationRules = {
  required: (value) => {
    if (value === null || value === undefined || value === '') {
      return 'This field is required';
    }
    return null;
  },
  
  minLength: (min) => (value) => {
    if (value && value.length < min) {
      return `Must be at least ${min} characters`;
    }
    return null;
  },
  
  maxLength: (max) => (value) => {
    if (value && value.length > max) {
      return `Must be less than ${max} characters`;
    }
    return null;
  },
  
  email: (value) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (value && !emailRegex.test(value)) {
      return 'Please enter a valid email address';
    }
    return null;
  },
  
  phone: (value) => {
    const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
    if (value && !phoneRegex.test(value.replace(/[\s\-\(\)]/g, ''))) {
      return 'Please enter a valid phone number';
    }
    return null;
  },
  
  url: (value) => {
    try {
      if (value) {
        new URL(value);
      }
      return null;
    } catch {
      return 'Please enter a valid URL';
    }
  },
  
  numeric: (value) => {
    if (value && isNaN(Number(value))) {
      return 'Must be a number';
    }
    return null;
  },
  
  minValue: (min) => (value) => {
    if (value !== null && value !== undefined && Number(value) < min) {
      return `Must be at least ${min}`;
    }
    return null;
  },
  
  maxValue: (max) => (value) => {
    if (value !== null && value !== undefined && Number(value) > max) {
      return `Must be less than ${max}`;
    }
    return null;
  },
  
  pattern: (regex, message) => (value) => {
    if (value && !regex.test(value)) {
      return message || 'Invalid format';
    }
    return null;
  },
  
  date: (value) => {
    if (value && isNaN(Date.parse(value))) {
      return 'Please enter a valid date';
    }
    return null;
  },
  
  futureDate: (value) => {
    if (value && new Date(value) <= new Date()) {
      return 'Date must be in the future';
    }
    return null;
  },
  
  pastDate: (value) => {
    if (value && new Date(value) >= new Date()) {
      return 'Date must be in the past';
    }
    return null;
  },
  
  fileType: (allowedTypes) => (file) => {
    if (file && !allowedTypes.includes(file.type)) {
      return `File type must be one of: ${allowedTypes.join(', ')}`;
    }
    return null;
  },
  
  fileSize: (maxSizeMB) => (file) => {
    if (file && file.size > maxSizeMB * 1024 * 1024) {
      return `File size must be less than ${maxSizeMB}MB`;
    }
    return null;
  }
};

// Schema validation for different entities
export const projectSchema = {
  name: [
    validationRules.required,
    validationRules.minLength(3),
    validationRules.maxLength(100)
  ],
  address: [
    validationRules.required,
    validationRules.minLength(5),
    validationRules.maxLength(200)
  ],
  budget: [
    validationRules.numeric,
    validationRules.minValue(0)
  ],
  startDate: [
    validationRules.required,
    validationRules.date
  ],
  endDate: [
    validationRules.date,
    (value, values) => {
      if (values.startDate && value && new Date(value) <= new Date(values.startDate)) {
        return 'End date must be after start date';
      }
      return null;
    }
  ]
};

export const updateSchema = {
  title: [
    validationRules.required,
    validationRules.minLength(5),
    validationRules.maxLength(200)
  ],
  description: [
    validationRules.required,
    validationRules.minLength(10),
    validationRules.maxLength(5000)
  ],
  importance: [
    validationRules.required,
    validationRules.pattern(/^(low|medium|high)$/, 'Must be low, medium, or high')
  ]
};

export const gallerySchema = {
  caption: [
    validationRules.maxLength(200)
  ],
  room: [
    validationRules.maxLength(50)
  ],
  stage: [
    validationRules.maxLength(50)
  ]
};

export const chatSchema = {
  content: [
    validationRules.required,
    validationRules.minLength(1),
    validationRules.maxLength(1000)
  ]
};

// Validation utility functions
export const validateField = (value, rules, formValues = {}) => {
  for (const rule of rules) {
    const error = rule(value, formValues);
    if (error) return error;
  }
  return null;
};

export const validateForm = (values, schema) => {
  const errors = {};
  
  for (const [field, rules] of Object.entries(schema)) {
    const error = validateField(values[field], rules, values);
    if (error) {
      errors[field] = error;
    }
  }
  
  return errors;
};

export const hasErrors = (errors) => {
  return Object.values(errors).some(error => error !== null);
};

// Real-time validation with debouncing
export const createDebouncedValidator = (validator, delay = 300) => {
  let timeout;
  
  return (value, ...args) => {
    return new Promise((resolve) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        resolve(validator(value, ...args));
      }, delay);
    });
  };
};

// File validation utilities
export const validateFiles = (files, options = {}) => {
  const {
    maxFiles = 10,
    maxSizeMB = 10,
    allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  } = options;
  
  const errors = [];
  
  if (files.length > maxFiles) {
    errors.push(`Maximum ${maxFiles} files allowed`);
  }
  
  for (const file of files) {
    const typeError = validationRules.fileType(allowedTypes)(file);
    if (typeError) errors.push(`${file.name}: ${typeError}`);
    
    const sizeError = validationRules.fileSize(maxSizeMB)(file);
    if (sizeError) errors.push(`${file.name}: ${sizeError}`);
  }
  
  return errors;
};

// Async validation for checking uniqueness
export const createAsyncValidator = (checkFn, errorMessage) => {
  return async (value) => {
    if (!value) return null;
    
    try {
      const isUnique = await checkFn(value);
      if (!isUnique) {
        return errorMessage;
      }
      return null;
    } catch (error) {
      return 'Validation error occurred';
    }
  };
};

// Cross-field validation utilities
export const validatePasswordConfirmation = (password, confirmation) => {
  if (password !== confirmation) {
    return 'Passwords do not match';
  }
  return null;
};

export const validateDateRange = (startDate, endDate) => {
  if (startDate && endDate && new Date(endDate) <= new Date(startDate)) {
    return 'End date must be after start date';
  }
  return null;
};

// Export all validation utilities
export default {
  rules: validationRules,
  schemas: {
    project: projectSchema,
    update: updateSchema,
    gallery: gallerySchema,
    chat: chatSchema
  },
  validateField,
  validateForm,
  hasErrors,
  createDebouncedValidator,
  validateFiles,
  createAsyncValidator,
  validatePasswordConfirmation,
  validateDateRange
};
