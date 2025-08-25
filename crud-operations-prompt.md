# Comprehensive CRUD Operations Implementation Prompt

## Objective
Enhance the existing React frontend application with comprehensive CRUD (Create, Read, Update, Delete) operations that communicate with the Spring Boot backend API. The application should provide a seamless user experience for managing project data, updates, gallery items, and chat messages.

## Current State Analysis
- ✅ Spring Boot backend is running successfully on port 8080
- ✅ React frontend is running on port 5173
- ✅ API service layer is already implemented in `src/services/api.js`
- ✅ CORS configuration is properly set up
- ✅ Basic CRUD operations are partially implemented
- ❌ Need enhanced error handling and user feedback
- ❌ Need proper loading states and optimistic updates
- ❌ Need comprehensive form validation
- ❌ Need real-time updates (WebSocket integration)

## Technical Requirements

### 1. Authentication & Authorization
- Implement JWT token management with automatic refresh
- Handle token expiration and re-authentication
- Role-based access control (admin vs client users)
- Protected routes and API endpoints

### 2. Project Management CRUD
**Create:**
- Form for creating new projects with validation
- File upload for project documents/plans
- Real-time project creation with optimistic UI updates

**Read:**
- Fetch all projects for the authenticated user
- Get specific project details with related data
- Pagination and filtering for project lists

**Update:**
- Edit project information (name, address, dates, budget)
- Update project status and progress
- Drag-and-drop timeline updates

**Delete:**
- Soft delete projects with confirmation modal
- Archive functionality for completed projects
- Bulk delete operations

### 3. Project Updates CRUD
**Create:**
- Rich text editor for update descriptions
- Image upload within updates
- @mention functionality for team members

**Read:**
- Timeline view of project updates
- Filter updates by date, type, or importance
- Search functionality across updates

**Update:**
- Edit existing updates with version history
- Mark updates as read/unread
- Pin important updates to top

**Delete:**
- Delete updates with admin confirmation
- Archive old updates automatically

### 4. Gallery Management CRUD
**Create:**
- Multi-file upload with drag-and-drop
- Image compression and optimization
- Batch upload with progress indicators

**Read:**
- Grid and list view options
- Image filtering by room, stage, or date
- Lightbox preview with navigation

**Update:**
- Edit image metadata (caption, room, stage)
- Bulk edit operations
- Reorder gallery items

**Delete:**
- Single and bulk image deletion
- Confirmation modals with preview
- Archive deleted images

### 5. Chat System CRUD
**Create:**
- Real-time message sending
- File attachment support
- @mentions and emoji reactions

**Read:**
- Infinite scroll for message history
- Message search functionality
- Unread message indicators

**Update:**
- Edit sent messages within time limit
- Mark messages as read/unread
- Pin important messages

**Delete:**
- Delete messages with undo functionality
- Clear chat history
- Export chat conversations

## Implementation Guidelines

### Error Handling
- Global error boundary for React components
- API error interception and user-friendly messages
- Retry mechanisms for failed requests
- Network status monitoring

### Loading States
- Skeleton loading for all data fetching
- Progress indicators for file uploads
- Optimistic UI updates for better UX
- Loading spinners for form submissions

### Form Validation
- Client-side validation with real-time feedback
- Server-side validation error handling
- Form persistence across page refreshes
- Multi-step forms for complex operations

### Real-time Updates
- WebSocket integration for live updates
- Polling fallback for older browsers
- Offline capability with sync on reconnect
- Conflict resolution for concurrent edits

### Testing Requirements
- Unit tests for all service functions
- Integration tests for API endpoints
- E2E tests for critical user flows
- Performance testing for large datasets

## File Structure Enhancement
```
src/
  components/
    crud/
      CreateProjectModal.jsx
      EditProjectForm.jsx
      ProjectList.jsx
      UpdateTimeline.jsx
      GalleryManager.jsx
      ChatInterface.jsx
    forms/
      validation.js
      FormField.jsx
      FileUpload.jsx
  hooks/
    useApi.js
    useWebSocket.js
    useOptimisticUpdate.js
  utils/
    errorHandler.js
    fileUtils.js
    dateUtils.js
```

## Success Metrics
- API response time under 200ms
- 99.9% API success rate
- User satisfaction score > 4.5/5
- Zero critical bugs in production
- Mobile responsiveness across all devices

## Priority Order
1. Fix any existing CORS/authentication issues
2. Implement comprehensive error handling
3. Add loading states and optimistic updates
4. Enhance form validation
5. Implement WebSocket real-time updates
6. Add offline capability
7. Comprehensive testing suite

## Dependencies to Add
- `react-query` or `swr` for data fetching
- `react-hook-form` for form management
- `socket.io-client` for WebSocket connections
- `react-dropzone` for file uploads
- `date-fns` for date manipulation
- `react-hot-toast` for notifications
