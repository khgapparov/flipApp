import React from 'react';
import { useFormContext, Controller } from 'react-hook-form';

const FormField = ({
  name,
  label,
  type = 'text',
  placeholder,
  required = false,
  disabled = false,
  className = '',
  labelClassName = '',
  inputClassName = '',
  errorClassName = '',
  validationRules = [],
  children,
  ...props
}) => {
  const {
    control,
    formState: { errors },
    watch
  } = useFormContext();

  const error = errors[name];
  const value = watch(name);

  const getInputClasses = () => {
    const baseClasses = 'w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors';
    const errorClasses = error ? 'border-red-500 bg-red-50' : 'border-gray-300';
    const disabledClasses = disabled ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : 'bg-white';
    
    return `${baseClasses} ${errorClasses} ${disabledClasses} ${inputClassName}`;
  };

  const renderInput = () => {
    switch (type) {
      case 'textarea':
        return (
          <Controller
            name={name}
            control={control}
            render={({ field }) => (
              <textarea
                {...field}
                id={name}
                placeholder={placeholder}
                disabled={disabled}
                className={getInputClasses()}
                rows={4}
                {...props}
              />
            )}
          />
        );

      case 'select':
        return (
          <Controller
            name={name}
            control={control}
            render={({ field }) => (
              <select
                {...field}
                id={name}
                disabled={disabled}
                className={getInputClasses()}
                {...props}
              >
                {children}
              </select>
            )}
          />
        );

      case 'checkbox':
        return (
          <Controller
            name={name}
            control={control}
            render={({ field }) => (
              <input
                {...field}
                type="checkbox"
                id={name}
                disabled={disabled}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                checked={field.value}
                {...props}
              />
            )}
          />
        );

      case 'file':
        return (
          <Controller
            name={name}
            control={control}
            render={({ field: { value, onChange, ...field } }) => (
              <input
                {...field}
                type="file"
                id={name}
                disabled={disabled}
                className={getInputClasses()}
                onChange={(e) => onChange(e.target.files[0])}
                {...props}
              />
            )}
          />
        );

      default:
        return (
          <Controller
            name={name}
            control={control}
            render={({ field }) => (
              <input
                {...field}
                type={type}
                id={name}
                placeholder={placeholder}
                disabled={disabled}
                className={getInputClasses()}
                {...props}
              />
            )}
          />
        );
    }
  };

  return (
    <div className={`mb-4 ${className}`}>
      {label && (
        <label
          htmlFor={name}
          className={`block text-sm font-medium text-gray-700 mb-1 ${labelClassName}`}
        >
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
      )}
      
      {renderInput()}
      
      {error && (
        <p className={`mt-1 text-sm text-red-600 ${errorClassName}`}>
          {error.message}
        </p>
      )}
      
      {type === 'file' && value && (
        <p className="mt-1 text-sm text-gray-500">
          Selected: {value.name}
        </p>
      )}
    </div>
  );
};

export default FormField;
