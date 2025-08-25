import React from 'react';
import { ClipboardList, Calendar, Clock, User, LogIn } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const UpdatesView = ({ updates, formatDate }) => {
  const { user, loading: authLoading } = useAuth();
  if (updates.length === 0) {
    return (
      <div className="text-center py-16">
        <div className="bg-blue-100 p-6 rounded-full inline-block mb-6">
          <ClipboardList size={40} className="text-blue-600" />
        </div>
        <h3 className="text-2xl font-semibold text-gray-900 mb-3">No Updates Yet</h3>
        <p className="text-gray-600 max-w-md mx-auto">
          Project updates will appear here once they are available. Check back soon for the latest progress reports.
        </p>
        
        {/* Auth status banners */}
        {!user && !authLoading && (
          <div className="mt-8 bg-blue-50 border border-blue-200 rounded-md p-4 max-w-md mx-auto">
            <div className="flex items-center">
              <LogIn size={20} className="text-blue-600 mr-3" />
              <div>
                <p className="text-sm text-blue-800">
                  <strong>Sign in</strong> to receive personalized project updates and notifications.
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
                  You're viewing updates as a <strong>guest</strong>. Create an account to save your preferences and get customized updates.
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
        <h2 className="text-3xl font-bold text-gray-900">Project Updates</h2>
        <div className="flex items-center text-gray-600">
          <Calendar size={18} className="mr-2" />
          <span className="text-sm">{updates.length} updates</span>
        </div>
      </div>

      {/* Updates Timeline */}
      <div className="space-y-6">
        {updates.map((update, index) => (
          <div key={update.id} className="card">
            <div className="flex items-start space-x-4">
              {/* Timeline indicator */}
              <div className="flex flex-col items-center">
                <div className={`w-3 h-3 rounded-full ${
                  index === 0 ? 'bg-blue-600' : 'bg-gray-300'
                }`}></div>
                {index < updates.length - 1 && (
                  <div className="w-0.5 h-16 bg-gray-200 mt-1"></div>
                )}
              </div>

              {/* Update content */}
              <div className="flex-1">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">
                    Progress Update
                  </h3>
                  <div className="flex items-center text-sm text-gray-500">
                    <Clock size={14} className="mr-1" />
                    <span>{formatDate(update.timestamp)}</span>
                  </div>
                </div>
                
                <p className="text-gray-700 leading-relaxed mb-4">
                  {update.description}
                </p>

                {/* Update metadata */}
                <div className="flex items-center space-x-4 text-sm text-gray-500">
                  {update.category && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {update.category}
                    </span>
                  )}
                  {update.author && (
                    <span>By {update.author}</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Summary */}
      <div className="card bg-blue-50 border-blue-200">
        <div className="flex items-center">
          <ClipboardList size={20} className="text-blue-600 mr-3" />
          <div>
            <h3 className="text-sm font-medium text-blue-900">Update Summary</h3>
            <p className="text-sm text-blue-700">
              {updates.length} total updates spanning the project timeline
            </p>
          </div>
        </div>
      </div>

      {/* Auth status banners when updates exist */}
      {!user && !authLoading && (
        <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
          <div className="flex items-center">
            <LogIn size={20} className="text-blue-600 mr-3" />
            <div>
              <h4 className="text-sm font-medium text-blue-900 mb-1">Get Personalized Updates</h4>
              <p className="text-sm text-blue-800">
                Sign in to receive customized project notifications and track updates that matter to you.
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
              <h4 className="text-sm font-medium text-yellow-900 mb-1">Guest Account</h4>
              <p className="text-sm text-yellow-800">
                You're viewing updates as a guest. Create a permanent account to save your preferences and receive email notifications.
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
              <h4 className="text-sm font-medium text-green-900 mb-1">Welcome back, {user.username}!</h4>
              <p className="text-sm text-green-800">
                You're all set to receive personalized project updates and notifications.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UpdatesView;
