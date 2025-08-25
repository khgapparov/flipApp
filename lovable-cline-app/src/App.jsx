import React, { useState, useEffect, useRef } from 'react';
import { 
  AlertCircle, 
  Home,
  Calendar,
  TrendingUp,
  Image,
  ClipboardList,
  MessageCircle,
  Send,
  ZoomIn,
  User as UserIcon,
  X
} from 'lucide-react';
import { 
  enhancedAuthService, 
  enhancedProjectService, 
  enhancedUpdatesService, 
  enhancedGalleryService, 
  enhancedChatService
} from './services/enhancedApi';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginModal from './components/auth/LoginModal';
import SignupModal from './components/auth/SignupModal';
import UserProfile from './components/auth/UserProfile';
import DashboardView from './components/DashboardView';
import UpdatesView from './components/UpdatesView';
import GalleryView from './components/GalleryView';
import ChatView from './components/ChatView';
import './App.css';

// Global variables provided by the environment
const __app_id = import.meta.env.VITE_APP_ID || 'lovable-cline-app';
const __initial_auth_token = import.meta.env.VITE_INITIAL_AUTH_TOKEN || null;

function AppContent() {
  const { user: authUser, loading: authLoading, anonymousLogin: authAnonymousLogin } = useAuth();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showSignupModal, setShowSignupModal] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [updates, setUpdates] = useState([]);
  const [gallery, setGallery] = useState([]);
  const [chatMessages, setChatMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [activeTab, setActiveTab] = useState('dashboard');
  const [error, setError] = useState(null);
  
  const chatEndRef = useRef(null);
  const unsubscribeRefs = useRef([]);

  // Initialize app and set up API connections
  useEffect(() => {
    const initializeApp = async () => {
      try {
        if (authUser) {
          // User is logged in, setup app
          await setupApiConnection();
        }
        // If no user is logged in, don't automatically login - show auth buttons instead
        setLoading(false);
      } catch (error) {
        console.error('App initialization error:', error);
        setError('Failed to initialize application. Please refresh the page.');
        setLoading(false);
      }
    };

    initializeApp();

    // Cleanup function to unsubscribe from listeners
    return () => {
      unsubscribeRefs.current.forEach(unsubscribe => unsubscribe && unsubscribe());
    };
  }, [authUser]);

  // Scroll to bottom of chat when new messages arrive
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  const setupApiConnection = async () => {
    try {
      // User is already authenticated via auth context, setup app
      await setupRealTimeListeners();
      setLoading(false);
    } catch (error) {
      console.error('API connection error:', error);
      setError('Failed to connect to server. Please refresh the page.');
      setLoading(false);
    }
  };

  const setupRealTimeListeners = async () => {
    try {
      // Get user's projects from the API
      const projects = await enhancedProjectService.getProjects();
      
      if (projects.length === 0) {
        // No projects found - set project to null to show empty state
        setProject(null);
        setUpdates([]);
        setGallery([]);
        setChatMessages([]);
        return;
      }
      
      // Use the first project (in a real app, you might let the user choose)
      const projectId = projects[0].id;
      
      // Set up polling listeners
      setupProjectListener(projectId);
      setupUpdatesListener(projectId);
      setupGalleryListener(projectId);
      setupChatListener(projectId);
    } catch (error) {
      console.error('Error setting up listeners:', error);
      setError('Failed to load project data.');
    }
  };

  const setupProjectListener = (projectId) => {
    // First load initial data
    enhancedProjectService.getProject(projectId)
      .then(projectData => {
        // Convert backend date strings to Firebase Timestamp-like objects
        const processedProject = {
          ...projectData,
          startDate: { toDate: () => new Date(projectData.start_date) },
          estimatedEndDate: { toDate: () => new Date(projectData.estimated_end_date) }
        };
        setProject(processedProject);
      })
      .catch(error => {
        console.error('Error loading project:', error);
        setError('Failed to load project data.');
      });
    
    // Then set up polling
    const unsubscribe = enhancedProjectService.subscribeToProject(projectId, (projectData) => {
      // Convert backend date strings to Firebase Timestamp-like objects
      const processedProject = {
        ...projectData,
        startDate: { toDate: () => new Date(projectData.start_date) },
        estimatedEndDate: { toDate: () => new Date(projectData.estimated_end_date) }
      };
      setProject(processedProject);
    });
    
    unsubscribeRefs.current.push(unsubscribe);
  };

  const setupUpdatesListener = (projectId) => {
    // First load initial data
    enhancedUpdatesService.getUpdates(projectId)
      .then(updatesData => {
        setUpdates(updatesData.map(update => ({
          ...update,
          timestamp: new Date(update.date), // Backend uses 'date' field
          description: update.description // Backend provides description
        })));
      })
      .catch(error => {
        console.error('Error loading updates:', error);
        setError('Failed to load project updates.');
      });
    
    // Then set up polling
    const unsubscribe = enhancedUpdatesService.subscribeToUpdates(projectId, (updatesData) => {
      setUpdates(updatesData.map(update => ({
        ...update,
        timestamp: new Date(update.date), // Backend uses 'date' field
        description: update.description // Backend provides description
      })));
    });
    
    unsubscribeRefs.current.push(unsubscribe);
  };

  const setupGalleryListener = (projectId) => {
    // First load initial data
    enhancedGalleryService.getGallery(projectId)
      .then(galleryData => {
        setGallery(galleryData.map(image => ({
          ...image,
          timestamp: new Date(image.created_at), // Backend uses 'created_at' field
          url: image.image_url // Backend uses 'image_url' field
        })));
      })
      .catch(error => {
        console.error('Error loading gallery:', error);
        setError('Failed to load gallery images.');
      });
    
    // Then set up polling
    const unsubscribe = enhancedGalleryService.subscribeToGallery(projectId, (galleryData) => {
      setGallery(galleryData.map(image => ({
        ...image,
        timestamp: new Date(image.created_at), // Backend uses 'created_at' field
        url: image.image_url // Backend uses 'image_url' field
      })));
    });
    
    unsubscribeRefs.current.push(unsubscribe);
  };

  const setupChatListener = (projectId) => {
    // First load initial data
    enhancedChatService.getMessages(projectId)
      .then(messagesData => {
        setChatMessages(messagesData.map(message => ({
          ...message,
          text: message.message, // Map 'message' field to 'text'
          timestamp: new Date(message.created_at), // Map 'created_at' field to 'timestamp'
          senderName: message.is_from_client ? 'Client' : 'Project Manager' // Determine sender name based on is_from_client
        })));
      })
      .catch(error => {
        console.error('Error loading chat messages:', error);
        setError('Failed to load chat messages.');
      });
    
    // Then set up polling
    const unsubscribe = enhancedChatService.subscribeToChat(projectId, (messagesData) => {
      setChatMessages(messagesData.map(message => ({
        ...message,
        text: message.message, // Map 'message' field to 'text'
        timestamp: new Date(message.created_at), // Map 'created_at' field to 'timestamp'
        senderName: message.is_from_client ? 'Client' : 'Project Manager' // Determine sender name based on is_from_client
      })));
    });
    
    unsubscribeRefs.current.push(unsubscribe);
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !project) return;
    
    try {
      // Always use real API connection
      const messageData = {
        message: newMessage.trim(),
        is_from_client: true
      };
      
      await enhancedChatService.sendMessage(project.id, messageData);
      setNewMessage('');
    } catch (error) {
      console.error('Error sending message:', error);
      setError('Failed to send message. Please try again.');
    }
  };

  const getProjectStatus = () => {
    if (!project) return 'Loading...';
    
    const today = new Date();
    const endDate = project.estimatedEndDate?.toDate();
    
    if (!endDate) return 'On Track';
    
    const daysRemaining = Math.ceil((endDate - today) / (1000 * 60 * 60 * 24));
    
    if (daysRemaining < 0) return 'Delayed';
    if (daysRemaining < 7) return 'Potential Delay';
    if (daysRemaining < 14) return 'On Track';
    return 'Ahead of Schedule';
  };

  const formatDate = (date) => {
    if (!date) return 'N/A';
    
    // Convert to Date object if it's a string
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    
    // Check if the date is valid
    if (!(dateObj instanceof Date) || isNaN(dateObj.getTime())) {
      return 'N/A';
    }
    
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(dateObj);
  };

  // Add auth buttons to header
  const renderAuthButtons = () => {
    if (authLoading) {
      return <div className="loading-spinner-small"></div>;
    }

    if (authUser) {
      return (
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowProfile(true)}
            className="flex items-center space-x-2 text-gray-700 hover:text-gray-900"
          >
            <UserIcon size={18} />
            <span>{authUser.username}</span>
            {authUser.isAnonymous && (
              <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">
                Guest
              </span>
            )}
          </button>
        </div>
      );
    }

    return (
      <div className="flex items-center space-x-3">
        <button
          onClick={() => setShowLoginModal(true)}
          className="text-gray-700 hover:text-gray-900"
        >
          Login
        </button>
        <button
          onClick={() => setShowSignupModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          Sign Up
        </button>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="loading-spinner mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Show welcome screen when no user is logged in
  if (!authUser) {
    return (
      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <header className="bg-brand-surface shadow-sm border-b border-gray-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center h-16">
              <div className="flex items-center">
                <div className="bg-brand-primary p-2 rounded-lg">
                  <Home size={24} className="text-white" />
                </div>
                <h1 className="ml-3 text-2xl font-bold text-brand-text-primary">Lovable Cline</h1>
              </div>
              <div className="flex items-center space-x-4">
                <span className="text-sm text-brand-text-secondary">Client Portal</span>
                {renderAuthButtons()}
              </div>
            </div>
          </div>
        </header>

        {/* Welcome Content */}
        <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
          <div className="bg-white rounded-lg shadow-md p-8">
            <div className="w-16 h-16 bg-brand-primary rounded-full flex items-center justify-center mx-auto mb-6">
              <Home size={32} className="text-white" />
            </div>
            <h2 className="text-3xl font-bold text-gray-800 mb-4">Welcome to Lovable Cline</h2>
            <p className="text-gray-600 mb-8 text-lg">
              Your project management portal for tracking progress, updates, and communication.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button
                onClick={() => setShowLoginModal(true)}
                className="bg-brand-primary text-white px-6 py-3 rounded-md hover:bg-blue-700 transition-colors font-medium"
              >
                Sign In
              </button>
              <button
                onClick={() => setShowSignupModal(true)}
                className="border border-brand-primary text-brand-primary px-6 py-3 rounded-md hover:bg-brand-primary hover:text-white transition-colors font-medium"
              >
                Create Account
              </button>
              <button
                onClick={async () => {
                  try {
                    setLoading(true);
                    await authAnonymousLogin();
                  } catch (error) {
                    console.error('Anonymous login failed:', error);
                    setError('Failed to continue as guest. Please try again.');
                    setLoading(false);
                  }
                }}
                className="text-gray-600 px-6 py-3 rounded-md hover:text-gray-800 hover:bg-gray-100 transition-colors font-medium"
              >
                Continue as Guest
              </button>
            </div>
            
            <p className="text-sm text-gray-500 mt-6">
              Guest access provides limited functionality. Sign in for full access to all features.
            </p>
          </div>
        </main>

        {/* Auth Modals */}
        <LoginModal
          isOpen={showLoginModal}
          onClose={() => setShowLoginModal(false)}
          onSwitchToSignup={() => {
            setShowLoginModal(false);
            setShowSignupModal(true);
          }}
        />

        <SignupModal
          isOpen={showSignupModal}
          onClose={() => setShowSignupModal(false)}
          onSwitchToLogin={() => {
            setShowSignupModal(false);
            setShowLoginModal(true);
          }}
        />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-red-500 mb-4">
            <AlertCircle size={48} />
          </div>
          <h2 className="text-xl font-semibold text-gray-800 mb-2">Error</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="bg-brand-primary text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            Refresh Page
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-brand-surface shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <div className="bg-brand-primary p-2 rounded-lg">
                <Home size={24} className="text-white" />
              </div>
              <h1 className="ml-3 text-2xl font-bold text-brand-text-primary">Lovable Cline</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-brand-text-secondary">Client Portal</span>
              {renderAuthButtons()}
            </div>
          </div>
        </div>
      </header>

      {/* Desktop Navigation */}
      <nav className="hidden md:block bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-8">
            {[
              { id: 'dashboard', label: 'Dashboard', icon: Home },
              { id: 'updates', label: 'Updates', icon: ClipboardList },
              { id: 'gallery', label: 'Gallery', icon: Image },
              { id: 'chat', label: 'Messages', icon: MessageCircle }
            ].map((tab) => {
              const IconComponent = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-4 text-sm font-medium border-b-2 transition-all duration-200 flex items-center space-x-2 ${
                    activeTab === tab.id
                      ? 'border-brand-primary text-brand-primary'
                      : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                  }`}
                >
                  <IconComponent size={18} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>
      </nav>

      {/* Mobile Bottom Navigation */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg z-50">
        <div className="grid grid-cols-4 h-16">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: Home },
            { id: 'updates', label: 'Updates', icon: ClipboardList },
            { id: 'gallery', label: 'Gallery', icon: Image },
            { id: 'chat', label: 'Messages', icon: MessageCircle }
          ].map((tab) => {
            const IconComponent = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex flex-col items-center justify-center p-2 text-xs transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'text-brand-primary'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <IconComponent size={20} />
                <span className="mt-1">{tab.label}</span>
                {activeTab === tab.id && (
                  <div className="absolute top-0 w-1/2 h-1 bg-brand-primary rounded-full"></div>
                )}
              </button>
            );
          })}
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 md:pb-8 pb-20">
        {activeTab === 'dashboard' && (
          <DashboardView 
            project={project} 
            updates={updates} 
            gallery={gallery}
            getProjectStatus={getProjectStatus}
            formatDate={formatDate}
          />
        )}

        {activeTab === 'updates' && (
          <UpdatesView updates={updates} formatDate={formatDate} />
        )}

        {activeTab === 'gallery' && (
          <GalleryView gallery={gallery} formatDate={formatDate} />
        )}

        {activeTab === 'chat' && (
          <ChatView 
            chatMessages={chatMessages}
            newMessage={newMessage}
            setNewMessage={setNewMessage}
            sendMessage={sendMessage}
            chatEndRef={chatEndRef}
            formatDate={formatDate}
          />
        )}
      </main>

      {/* Auth Modals */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSwitchToSignup={() => {
          setShowLoginModal(false);
          setShowSignupModal(true);
        }}
      />

      <SignupModal
        isOpen={showSignupModal}
        onClose={() => setShowSignupModal(false)}
        onSwitchToLogin={() => {
          setShowSignupModal(false);
          setShowLoginModal(true);
        }}
      />

      {/* User Profile Modal */}
      {showProfile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-md">
            <div className="flex justify-between items-center p-6 border-b">
              <h2 className="text-xl font-semibold text-gray-800">User Profile</h2>
              <button 
                onClick={() => setShowProfile(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>
            <div className="p-6">
              <UserProfile />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
