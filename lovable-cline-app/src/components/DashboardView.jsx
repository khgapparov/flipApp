import React from 'react';
import { 
  Calendar, 
  TrendingUp, 
  Image, 
  Clock,
  MapPin,
  AlertCircle,
  CheckCircle,
  Clock as ClockIcon,
  User,
  LogIn
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const DashboardView = ({ project, updates, gallery, getProjectStatus, formatDate }) => {
  const { user: authUser, loading: authLoading } = useAuth();

  if (!project) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">No project assigned yet.</p>
      </div>
    );
  }

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <div className="text-center py-12">
        <div className="loading-spinner mx-auto mb-4"></div>
        <p className="text-gray-500">Loading user information...</p>
      </div>
    );
  }

  const latestUpdate = updates[0];
  const recentImages = gallery.slice(0, 3);
  const projectStatus = getProjectStatus();

  const getStatusIcon = (status) => {
    switch (status) {
      case 'On Track':
        return <CheckCircle size={16} className="text-green-600" />;
      case 'Potential Delay':
        return <AlertCircle size={16} className="text-yellow-600" />;
      case 'Delayed':
        return <AlertCircle size={16} className="text-red-600" />;
      default:
        return <ClockIcon size={16} className="text-gray-600" />;
    }
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'On Track':
        return 'status-on-track';
      case 'Potential Delay':
        return 'status-potential-delay';
      case 'Delayed':
        return 'status-delayed';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="space-y-8">
      {/* User Status Banner */}
      {authUser && authUser.isAnonymous && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-center">
            <User size={20} className="text-yellow-600 mr-3" />
            <div>
              <h3 className="text-sm font-medium text-yellow-800">Guest Account</h3>
              <p className="text-sm text-yellow-700">
                You're using a temporary guest account. Create a permanent account to save your preferences and access all features.
              </p>
            </div>
          </div>
        </div>
      )}

      {!authUser && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center">
            <LogIn size={20} className="text-blue-600 mr-3" />
            <div>
              <h3 className="text-sm font-medium text-blue-800">Welcome!</h3>
              <p className="text-sm text-blue-700">
                Sign in or create an account to personalize your experience and access additional features.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Project Header */}
      <div className="card">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex-1">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">{project.name}</h2>
            <div className="flex items-center text-gray-600 mb-4">
              <MapPin size={16} className="mr-2" />
              <span>{project.address}</span>
            </div>
            <p className="text-gray-600 leading-relaxed">{project.description}</p>
          </div>
          <div className="flex flex-col items-end gap-2">
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusClass(projectStatus)}`}>
              {getStatusIcon(projectStatus)}
              <span className="ml-2">{projectStatus}</span>
            </span>
            <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
              {project.status}
            </span>
          </div>
        </div>
      </div>

      {/* Status Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Timeline Card */}
        <div className="card">
          <div className="flex items-center">
            <div className="bg-blue-100 p-3 rounded-lg">
              <Calendar size={20} className="text-blue-600" />
            </div>
            <div className="ml-4">
              <h3 className="text-sm font-medium text-gray-500">Timeline Status</h3>
              <p className="text-lg font-semibold text-gray-900">
                {projectStatus}
              </p>
            </div>
          </div>
        </div>

        {/* Progress Card */}
        <div className="card">
          <div className="flex items-center">
            <div className="bg-orange-100 p-3 rounded-lg">
              <TrendingUp size={20} className="text-orange-600" />
            </div>
            <div className="ml-4">
              <h3 className="text-sm font-medium text-gray-500">Progress Updates</h3>
              <p className="text-lg font-semibold text-gray-900">
                {updates.length} Updates
              </p>
            </div>
          </div>
        </div>

        {/* Gallery Card */}
        <div className="card">
          <div className="flex items-center">
            <div className="bg-green-100 p-3 rounded-lg">
              <Image size={20} className="text-green-600" />
            </div>
            <div className="ml-4">
              <h3 className="text-sm font-medium text-gray-500">Photo Gallery</h3>
              <p className="text-lg font-semibold text-gray-900">
                {gallery.length} Photos
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left Column */}
        <div className="space-y-6">
          {/* Latest Update */}
          {latestUpdate && (
            <div className="card">
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Latest Update</h3>
              <div className="border-l-4 border-blue-500 pl-4">
                <p className="text-gray-700 leading-relaxed">{latestUpdate.description}</p>
                <div className="flex items-center mt-3 text-sm text-gray-500">
                  <Clock size={14} className="mr-2" />
                  <span>{formatDate(latestUpdate.timestamp)}</span>
                </div>
              </div>
            </div>
          )}

          {/* Project Timeline */}
          <div className="card">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Project Timeline</h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Start Date</span>
                <span className="text-sm font-medium text-gray-900">
                  {formatDate(project.startDate?.toDate())}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Estimated Completion</span>
                <span className="text-sm font-medium text-gray-900">
                  {formatDate(project.estimatedEndDate?.toDate())}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div 
                  className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: '65%' }}
                ></div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column - Recent Images */}
        {recentImages.length > 0 && (
          <div className="card">
            <h3 className="text-xl font-semibold text-gray-900 mb-4">Recent Photos</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {recentImages.map((image, index) => (
                <div key={image.id} className="relative group overflow-hidden rounded-lg">
                  <img
                    src={image.image_url}
                    alt={`Project progress ${index + 1}`}
                    className="w-full h-48 object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity flex items-center justify-center">
                    <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                      <div className="bg-white p-2 rounded-full">
                        <Image size={16} className="text-gray-800" />
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardView;
