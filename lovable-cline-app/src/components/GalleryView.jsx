import React from 'react';
import { Image, Camera, Calendar, User, LogIn } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const GalleryView = ({ gallery, formatDate }) => {
  const { user, loading: authLoading } = useAuth();
  if (gallery.length === 0) {
    return (
      <div className="text-center py-16">
        <div className="bg-blue-100 p-6 rounded-full inline-block mb-6">
          <Camera size={40} className="text-blue-600" />
        </div>
        <h3 className="text-2xl font-semibold text-gray-900 mb-3">No Photos Yet</h3>
        <p className="text-gray-600 max-w-md mx-auto">
          Project photos will appear here once they are available. Check back soon to see the visual progress of your project.
        </p>
        
        {/* Auth status banners */}
        {!user && !authLoading && (
          <div className="mt-8 bg-blue-50 border border-blue-200 rounded-md p-4 max-w-md mx-auto">
            <div className="flex items-center">
              <LogIn size={20} className="text-blue-600 mr-3" />
              <div>
                <p className="text-sm text-blue-800">
                  <strong>Sign in</strong> to upload photos and contribute to the project gallery.
                </p>
              </div>
            </div>
          </div>
        )}
        
        {user && user.isAnonymous && (
          <div className="mt-8 bg-yellow-50 border border-yellow-200 rounded-md p-4 max-w-md mx-auto">
            <div className="flex items-center">
              <User size={20} className="text-yellow-600 mr-3" />
              <div>
                <p className="text-sm text-yellow-800">
                  You're viewing the gallery as a <strong>guest</strong>. Create an account to upload photos and share your progress.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">Photo Gallery</h2>
        <div className="flex items-center text-gray-600">
          <Image size={18} className="mr-2" />
          <span className="text-sm">{gallery.length} photos</span>
        </div>
      </div>

      {/* Gallery Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {gallery.map((image) => (
          <div key={image.id} className="card overflow-hidden group">
            <div className="relative overflow-hidden">
              <img
                src={image.image_url}
                alt={image.caption || 'Project progress photo'}
                className="w-full h-56 object-cover transition-transform duration-300 group-hover:scale-105"
              />
              <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-opacity"></div>
            </div>
            
            <div className="p-4">
              {image.caption && (
                <h3 className="text-sm font-medium text-gray-900 mb-2 line-clamp-2">
                  {image.caption}
                </h3>
              )}
              
              <div className="flex items-center justify-between text-xs text-gray-500">
                <div className="flex items-center">
                  <Calendar size={12} className="mr-1" />
                  <span>{formatDate(image.timestamp)}</span>
                </div>
                
                {image.category && (
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
                    {image.category}
                  </span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Gallery Summary */}
      <div className="card bg-blue-50 border-blue-200">
        <div className="flex items-center">
          <Camera size={20} className="text-blue-600 mr-3" />
          <div>
            <h3 className="text-sm font-medium text-blue-900">Gallery Summary</h3>
            <p className="text-sm text-blue-700">
              {gallery.length} photos documenting the project progress
            </p>
          </div>
        </div>
      </div>

      {/* Auth status banners when gallery exists */}
      {!user && !authLoading && (
        <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
          <div className="flex items-center">
            <LogIn size={20} className="text-blue-600 mr-3" />
            <div>
              <h4 className="text-sm font-medium text-blue-900 mb-1">Share Your Progress</h4>
              <p className="text-sm text-blue-800">
                Sign in to upload your own photos and contribute to the project gallery.
              </p>
            </div>
          </div>
        </div>
      )}
      
      {user && user.isAnonymous && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4">
          <div className="flex items-center">
            <User size={20} className="text-yellow-600 mr-3" />
            <div>
              <h4 className="text-sm font-medium text-yellow-900 mb-1">Guest Gallery Access</h4>
              <p className="text-sm text-yellow-800">
                You're viewing photos as a guest. Create an account to upload your own progress photos and organize them in albums.
              </p>
            </div>
          </div>
        </div>
      )}
      
      {user && !user.isAnonymous && (
        <div className="bg-green-50 border border-green-200 rounded-md p-4">
          <div className="flex items-center">
            <User size={20} className="text-green-600 mr-3" />
            <div>
              <h4 className="text-sm font-medium text-green-900 mb-1">Welcome to the Gallery, {user.username}!</h4>
              <p className="text-sm text-green-800">
                You can now upload photos, create albums, and share your project progress with the team.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default GalleryView;
