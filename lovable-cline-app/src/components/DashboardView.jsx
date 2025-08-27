import React from 'react';
import { 
  Calendar, 
  TrendingUp, 
  Image, 
  Clock,
  MapPin,
  AlertCircle,
  CheckCircle2,
  Clock as ClockIcon,
  User,
  LogIn,
  DollarSign,
  Users
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Badge } from "./ui/badge";
import { Progress } from "./ui/progress";

const DashboardView = ({ project, updates, gallery, getProjectStatus, formatDate }) => {
  const { user: authUser, loading: authLoading } = useAuth();

  if (!project) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">No project assigned yet.</p>
      </div>
    );
  }

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <div className="text-center py-12">
        <div className="loading-spinner mx-auto mb-4"></div>
        <p className="text-muted-foreground">Loading user information...</p>
      </div>
    );
  }

  const latestUpdate = updates[0];
  const recentImages = gallery.slice(0, 3);
  const projectStatus = getProjectStatus();

  const getStatusIcon = (status) => {
    switch (status) {
      case 'On Track':
        return <CheckCircle2 size={16} className="text-success" />;
      case 'Potential Delay':
        return <AlertCircle size={16} className="text-warning" />;
      case 'Delayed':
        return <AlertCircle size={16} className="text-destructive" />;
      default:
        return <ClockIcon size={16} className="text-muted-foreground" />;
    }
  };

  return (
    <div className="space-y-8">
      {/* User Status Banner */}
      {authUser && authUser.isAnonymous && (
        <Card className="bg-warning/10 border-warning/20">
          <CardContent className="p-4">
            <div className="flex items-center">
              <User size={20} className="text-warning mr-3" />
              <div>
                <h3 className="text-sm font-medium text-warning-foreground">Guest Account</h3>
                <p className="text-sm text-warning-foreground/80">
                  You're using a temporary guest account. Create a permanent account to save your preferences and access all features.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {!authUser && (
        <Card className="bg-info/10 border-info/20">
          <CardContent className="p-4">
            <div className="flex items-center">
              <LogIn size={20} className="text-info mr-3" />
              <div>
                <h3 className="text-sm font-medium text-info-foreground">Welcome!</h3>
                <p className="text-sm text-info-foreground/80">
                  Sign in or create an account to personalize your experience and access additional features.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Project Header */}
      <Card className="animate-fade-in">
        <CardContent className="p-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex-1">
              <h2 className="text-3xl font-bold text-foreground mb-2">{project.name}</h2>
              <div className="flex items-center text-muted-foreground mb-4">
                <MapPin size={16} className="mr-2" />
                <span>{project.address}</span>
              </div>
              <p className="text-muted-foreground leading-relaxed">{project.description}</p>
            </div>
            <div className="flex flex-col items-end gap-2">
              <Badge variant="outline" className="gap-2">
                {getStatusIcon(projectStatus)}
                <span>{projectStatus}</span>
              </Badge>
              <Badge variant="secondary" className="bg-primary/10 text-primary-foreground">
                {project.status}
              </Badge>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Status Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Timeline Card */}
        <Card className="animate-fade-in">
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="bg-primary/10 p-3 rounded-lg">
                <Calendar size={20} className="text-primary" />
              </div>
              <div className="ml-4">
                <h3 className="text-sm font-medium text-muted-foreground">Timeline Status</h3>
                <p className="text-lg font-semibold text-foreground">
                  {projectStatus}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Progress Card */}
        <Card className="animate-fade-in">
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="bg-success/10 p-3 rounded-lg">
                <TrendingUp size={20} className="text-success" />
              </div>
              <div className="ml-4">
                <h3 className="text-sm font-medium text-muted-foreground">Progress Updates</h3>
                <p className="text-lg font-semibold text-foreground">
                  {updates.length} Updates
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Gallery Card */}
        <Card className="animate-fade-in">
          <CardContent className="p-6">
            <div className="flex items-center">
              <div className="bg-info/10 p-3 rounded-lg">
                <Image size={20} className="text-info" />
              </div>
              <div className="ml-4">
                <h3 className="text-sm font-medium text-muted-foreground">Photo Gallery</h3>
                <p className="text-lg font-semibold text-foreground">
                  {gallery.length} Photos
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left Column */}
        <div className="space-y-6">
          {/* Latest Update */}
          {latestUpdate && (
            <Card className="animate-fade-in">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="h-5 w-5 text-info" />
                  Latest Update
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="border-l-4 border-primary pl-4">
                  <p className="text-foreground leading-relaxed">{latestUpdate.description}</p>
                  <div className="flex items-center mt-3 text-sm text-muted-foreground">
                    <Clock size={14} className="mr-2" />
                    <span>{formatDate(latestUpdate.timestamp)}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Project Timeline */}
          <Card className="animate-fade-in">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calendar className="h-5 w-5 text-primary" />
                Project Timeline
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Start Date</span>
                <span className="text-sm font-medium text-foreground">
                  {project.startDate ? formatDate(project.startDate.toDate()) : 'N/A'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Estimated Completion</span>
                <span className="text-sm font-medium text-foreground">
                  {project.estimatedEndDate ? formatDate(project.estimatedEndDate.toDate()) : 'N/A'}
                </span>
              </div>
              <div className="w-full bg-secondary rounded-full h-2">
                <Progress value={project.progress || 65} className="h-2" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Right Column - Recent Images */}
        {recentImages.length > 0 && (
          <Card className="animate-fade-in">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Image className="h-5 w-5 text-info" />
                Recent Photos
              </CardTitle>
            </CardHeader>
            <CardContent>
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
                        <div className="bg-card p-2 rounded-full">
                          <Image size={16} className="text-foreground" />
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default DashboardView;
