import React, { useState, useEffect, useRef } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TooltipProvider } from "./components/ui/tooltip";
import { Toaster } from "./components/ui/toaster";
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
  X,
  FolderOpen,
  MapPin,
  CheckCircle2,
  Clock,
  Users,
  DollarSign
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
import ProjectsPage from './components/ProjectsPage';
import { Card } from "./components/ui/card";
import { Badge } from "./components/ui/badge";
import { Progress } from "./components/ui/progress";
import { Avatar, AvatarFallback } from "./components/ui/avatar";
import './App.css';

const queryClient = new QueryClient();

// Global variables provided by the environment
const __app_id = import.meta.env.VITE_APP_ID || 'ecolight-cline-app';
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
  const [error, setError] = useState(null);
  
  const navigate = useNavigate();
  const location = useLocation();
  
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
          startDate: { toDate: () => new Date(projectData.startDate) },
          estimatedEndDate: { toDate: () => new Date(projectData.estimatedEndDate) }
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
        startDate: { toDate: () => new Date(projectData.startDate) },
        estimatedEndDate: { toDate: () => new Date(projectData.estimatedEndDate) }
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
          sender: message.user_id, // Map 'user_id' field to 'sender'
          createdAt: new Date(message.created_at), // Map 'created_at' field to 'createdAt'
          senderName: message.isFromClient ? 'Client' : 'Project Manager' // Determine sender name based on isFromClient
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
        sender: message.user_id, // Map 'user_id' field to 'sender'
        createdAt: new Date(message.created_at), // Map 'created_at' field to 'createdAt'
        senderName: message.isFromClient ? 'Client' : 'Project Manager' // Determine sender name based on isFromClient
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
        isFromClient: true
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

  const getStatusColor = (status) => {
    switch (status) {
      case 'On Track': return 'bg-success text-success-foreground';
      case 'Potential Delay': return 'bg-warning text-warning-foreground';
      case 'Delayed': return 'bg-destructive text-destructive-foreground';
      default: return 'bg-muted text-muted-foreground';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'On Track': return <CheckCircle2 className="h-4 w-4" />;
      case 'Potential Delay': return <Clock className="h-4 w-4" />;
      case 'Delayed': return <AlertCircle className="h-4 w-4" />;
      default: return <Clock className="h-4 w-4" />;
    }
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
            className="flex items-center space-x-2 text-foreground hover:text-primary transition-colors"
          >
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-primary text-primary-foreground">
                {authUser.username?.charAt(0)?.toUpperCase() || 'U'}
              </AvatarFallback>
            </Avatar>
            <span>{authUser.username}</span>
            {authUser.isAnonymous && (
              <Badge variant="outline" className="bg-warning/10 text-warning-foreground">
                Guest
              </Badge>
            )}
          </button>
        </div>
      );
    }

    return (
      <div className="flex items-center space-x-3">
        <button
          onClick={() => setShowLoginModal(true)}
          className="text-foreground hover:text-primary transition-colors"
        >
          Login
        </button>
        <button
          onClick={() => setShowSignupModal(true)}
          className="bg-primary text-primary-foreground px-4 py-2 rounded-md hover:bg-primary/90 transition-colors"
        >
          Sign Up
        </button>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="loading-spinner mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Show welcome screen when no user is logged in
  if (!authUser) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-background to-muted/20">
        {/* Header */}
        <header className="border-b bg-card/50 backdrop-blur-sm sticky top-0 z-50">
          <div className="container mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="bg-gradient-primary p-2 rounded-lg shadow-glow">
                  <Home size={24} className="text-primary-foreground" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold bg-gradient-primary bg-clip-text text-transparent">
                    Lovable Cline
                  </h1>
                  <p className="text-sm text-muted-foreground">Client Portal</p>
                </div>
              </div>
              <div className="flex items-center space-x-4">
                <span className="text-sm text-muted-foreground">Client Portal</span>
                {renderAuthButtons()}
              </div>
            </div>
          </div>
        </header>

        {/* Welcome Content */}
        <main className="container mx-auto px-4 py-16 text-center">
          <Card className="p-8 animate-fade-in">
            <div className="w-16 h-16 bg-gradient-primary rounded-full flex items-center justify-center mx-auto mb-6">
              <Home size={32} className="text-primary-foreground" />
            </div>
            <h2 className="text-3xl font-bold text-foreground mb-4">Welcome to Lovable Cline</h2>
            <p className="text-muted-foreground mb-8 text-lg">
              Your project management portal for tracking progress, updates, and communication.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button
                onClick={() => setShowLoginModal(true)}
                className="bg-primary text-primary-foreground px-6 py-3 rounded-md hover:bg-primary/90 transition-colors font-medium"
              >
                Sign In
              </button>
              <button
                onClick={() => setShowSignupModal(true)}
                className="border border-primary text-primary px-6 py-3 rounded-md hover:bg-primary hover:text-primary-foreground transition-colors font-medium"
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
                className="text-muted-foreground px-6 py-3 rounded-md hover:text-foreground hover:bg-muted transition-colors font-medium"
              >
                Continue as Guest
              </button>
            </div>
            
            <p className="text-sm text-muted-foreground mt-6">
              Guest access provides limited functionality. Sign in for full access to all features.
            </p>
          </Card>
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
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Card className="text-center p-6">
          <div className="text-destructive mb-4">
            <AlertCircle size={48} />
          </div>
          <h2 className="text-xl font-semibold text-foreground mb-2">Error</h2>
          <p className="text-muted-foreground mb-4">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="bg-primary text-primary-foreground px-4 py-2 rounded-md hover:bg-primary/90 transition-colors"
          >
            Refresh Page
          </button>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted/20">
      {/* Header */}
      <header className="border-b bg-card/50 backdrop-blur-sm sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="bg-gradient-primary p-2 rounded-lg shadow-glow">
                <Home size={24} className="text-primary-foreground" />
              </div>
              <div>
                <h1 className="text-2xl font-bold bg-gradient-primary bg-clip-text text-transparent">
                  Lovable Cline
                </h1>
                <p className="text-sm text-muted-foreground">Client Portal</p>
              </div>
            </div>
            
            <div className="flex items-center gap-4">
              {project && (
                <Badge variant="outline" className="gap-2">
                  {getStatusIcon(getProjectStatus())}
                  {getProjectStatus().toUpperCase()}
                </Badge>
              )}
              {renderAuthButtons()}
            </div>
          </div>
        </div>
      </header>

      {/* Project Header */}
      {project && (
        <div className="bg-gradient-hero text-primary-foreground">
          <div className="container mx-auto px-4 py-8">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
              <div className="animate-fade-in">
                <h2 className="text-3xl font-bold mb-2">{project.name}</h2>
                <div className="flex items-center gap-2 text-primary-foreground/80">
                  <MapPin className="h-4 w-4" />
                  <span>{project.address}</span>
                </div>
              </div>
              
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 animate-slide-up">
                <Card className="p-4 bg-card/10 border-primary-foreground/20 backdrop-blur-sm">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-5 w-5 text-success" />
                    <div>
                      <p className="text-sm text-primary-foreground/80">Progress</p>
                      <p className="text-lg font-bold">{project.progress || 65}%</p>
                    </div>
                  </div>
                </Card>
                
                <Card className="p-4 bg-card/10 border-primary-foreground/20 backdrop-blur-sm">
                  <div className="flex items-center gap-2">
                    <DollarSign className="h-5 w-5 text-warning" />
                    <div>
                      <p className="text-sm text-primary-foreground/80">Budget</p>
                      <p className="text-lg font-bold">${project.budget ? (project.budget / 1000).toFixed(0) + 'K' : '350K'}</p>
                    </div>
                  </div>
                </Card>
                
                <Card className="p-4 bg-card/10 border-primary-foreground/20 backdrop-blur-sm">
                  <div className="flex items-center gap-2">
                    <Users className="h-5 w-5 text-info" />
                    <div>
                      <p className="text-sm text-primary-foreground/80">Team</p>
                      <p className="text-lg font-bold">{project.team_size || 4}</p>
                    </div>
                  </div>
                </Card>
                
                <Card className="p-4 bg-card/10 border-primary-foreground/20 backdrop-blur-sm">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-5 w-5 text-primary-glow" />
                    <div>
                      <p className="text-sm text-primary-foreground/80">Completion</p>
                      <p className="text-sm font-medium">{formatDate(project.estimatedEndDate?.toDate())}</p>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
            
            <div className="mt-6 animate-fade-in">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-primary-foreground/80">Overall Progress</span>
                <span className="text-sm font-medium">{project.progress || 65}%</span>
              </div>
              <Progress value={project.progress || 65} className="h-3 bg-primary-foreground/20" />
            </div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="container mx-auto px-4 py-6">
        <div className="flex flex-wrap gap-4 mb-6 bg-card p-4 rounded-lg shadow-card">
          {[
            { path: '/', label: 'Dashboard', icon: Home },
            { path: '/projects', label: 'Projects', icon: FolderOpen },
            { path: '/updates', label: 'Updates', icon: ClipboardList },
            { path: '/gallery', label: 'Gallery', icon: Image },
            { path: '/chat', label: 'Messages', icon: MessageCircle }
          ].map((tab) => {
            const IconComponent = tab.icon;
            const isActive = location.pathname === tab.path;
            return (
              <button
                key={tab.path}
                onClick={() => navigate(tab.path)}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 flex items-center space-x-2 ${
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                }`}
              >
                <IconComponent size={18} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Main Content */}
        <div className="animate-fade-in">
          <Routes>
            <Route 
              path="/" 
              element={
                <DashboardView 
                  project={project} 
                  updates={updates} 
                  gallery={gallery}
                  getProjectStatus={getProjectStatus}
                  formatDate={formatDate}
                />
              } 
            />
            <Route 
              path="/projects" 
              element={<ProjectsPage />} 
            />
            <Route 
              path="/updates" 
              element={<UpdatesView updates={updates} formatDate={formatDate} />} 
            />
            <Route 
              path="/gallery" 
              element={<GalleryView gallery={gallery} formatDate={formatDate} />} 
            />
            <Route 
              path="/chat" 
              element={
                <ChatView 
                  chatMessages={chatMessages}
                  newMessage={newMessage}
                  setNewMessage={setNewMessage}
                  sendMessage={sendMessage}
                  chatEndRef={chatEndRef}
                  formatDate={formatDate}
                />
              } 
            />
          </Routes>
        </div>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-card border-t border-gray-200 shadow-lg z-50">
        <div className="grid grid-cols-5 h-16">
          {[
            { path: '/', label: 'Dashboard', icon: Home },
            { path: '/projects', label: 'Projects', icon: FolderOpen },
            { path: '/updates', label: 'Updates', icon: ClipboardList },
            { path: '/gallery', label: 'Gallery', icon: Image },
            { path: '/chat', label: 'Messages', icon: MessageCircle }
          ].map((tab) => {
            const IconComponent = tab.icon;
            const isActive = location.pathname === tab.path;
            return (
              <button
                key={tab.path}
                onClick={() => navigate(tab.path)}
                className={`flex flex-col items-center justify-center p-2 text-xs transition-all duration-200 ${
                  isActive
                    ? 'text-primary'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <IconComponent size={20} />
                <span className="mt-1">{tab.label}</span>
                {isActive && (
                  <div className="absolute top-0 w-1/2 h-1 bg-primary rounded-full"></div>
                )}
              </button>
            );
          })}
        </div>
      </nav>

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
          <div className="bg-card rounded-lg w-full max-w-md">
            <div className="flex justify-between items-center p-6 border-b border-gray-200">
              <h2 className="text-xl font-semibold text-foreground">User Profile</h2>
              <button 
                onClick={() => setShowProfile(false)}
                className="text-muted-foreground hover:text-foreground"
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
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Router>
          <AuthProvider>
            <AppContent />
          </AuthProvider>
        </Router>
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
