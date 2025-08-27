# CRUD Operations & Frontend Pages Quick Reference

## Backend Services CRUD Operations

### Authentication Service
- **POST** `/api/auth/login` - User login
- **POST** `/api/auth/register` - User registration  
- **POST** `/api/auth/logout` - User logout
- **GET** `/api/auth/me` - Get current user
- **PUT** `/api/auth/profile` - Update user profile

### Project Service
- **POST** `/api/projects` - Create project
- **GET** `/api/projects` - Get all projects
- **GET** `/api/projects/{id}` - Get project by ID
- **PUT** `/api/projects/{id}` - Update project
- **DELETE** `/api/projects/{id}` - Delete project
- **GET** `/api/projects/user/{userId}` - Get user's projects

### Project Update Service
- **POST** `/api/project-updates` - Create project update
- **GET** `/api/project-updates` - Get all updates
- **GET** `/api/project-updates/{id}` - Get update by ID
- **PUT** `/api/project-updates/{id}` - Update project update
- **DELETE** `/api/project-updates/{id}` - Delete project update
- **GET** `/api/project-updates/project/{projectId}` - Get updates for project

### Gallery Service
- **POST** `/api/gallery` - Upload gallery item
- **GET** `/api/gallery` - Get all gallery items
- **GET** `/api/gallery/{id}` - Get gallery item by ID
- **PUT** `/api/gallery/{id}` - Update gallery item
- **DELETE** `/api/gallery/{id}` - Delete gallery item
- **GET** `/api/gallery/project/{projectId}` - Get gallery for project

### Chat Service
- **POST** `/api/chat` - Send message
- **GET** `/api/chat` - Get all messages
- **GET** `/api/chat/{id}` - Get message by ID
- **PUT** `/api/chat/{id}` - Update message
- **DELETE** `/api/chat/{id}` - Delete message
- **GET** `/api/chat/project/{projectId}` - Get chat for project

## Frontend Render Pages

### Authentication Pages
- `/login` - User login form
- `/register` - User registration form
- `/profile` - User profile management

### Dashboard Pages
- `/dashboard` - Main dashboard with overview
- `/projects` - Project listing page
- `/projects/{id}` - Project detail page
- `/projects/{id}/edit` - Project edit form

### Project Update Pages
- `/updates` - All project updates timeline
- `/projects/{id}/updates` - Project-specific updates
- `/updates/create` - Create new update form
- `/updates/{id}/edit` - Edit update form

### Gallery Pages
- `/gallery` - Gallery overview
- `/projects/{id}/gallery` - Project gallery
- `/gallery/upload` - Upload gallery items
- `/gallery/{id}` - Gallery item detail

### Chat Pages
- `/chat` - Main chat interface
- `/projects/{id}/chat` - Project-specific chat
- `/chat/{id}` - Individual message view

## Quick CRUD Implementation Template

### Backend Controller Template
```java
@RestController
@RequestMapping("/api/{entity}")
public class EntityController {
    
    @PostMapping
    public ResponseEntity<Entity> create(@RequestBody Entity entity) {
        // Create logic
    }
    
    @GetMapping
    public ResponseEntity<List<Entity>> getAll() {
        // Read all logic
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Entity> getById(@PathVariable Long id) {
        // Read by ID logic
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Entity> update(@PathVariable Long id, @RequestBody Entity entity) {
        // Update logic
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // Delete logic
    }
}
```

### Frontend Service Template
```javascript
// API service functions
export const entityService = {
  create: (data) => api.post('/api/entity', data),
  getAll: () => api.get('/api/entity'),
  getById: (id) => api.get(`/api/entity/${id}`),
  update: (id, data) => api.put(`/api/entity/${id}`, data),
  delete: (id) => api.delete(`/api/entity/${id}`),
};
```

### React Component Template
```jsx
function EntityList() {
  const [entities, setEntities] = useState([]);
  
  useEffect(() => {
    entityService.getAll().then(setEntities);
  }, []);
  
  const handleDelete = (id) => {
    entityService.delete(id).then(() => {
      setEntities(entities.filter(e => e.id !== id));
    });
  };
  
  return (
    <div>
      {entities.map(entity => (
        <EntityItem key={entity.id} entity={entity} onDelete={handleDelete} />
      ))}
    </div>
  );
}
```

## Common Patterns

### Error Handling
- Use try-catch blocks in services
- Implement global error handler
- Show user-friendly error messages

### Loading States
- Use loading spinners during API calls
- Implement skeleton loading for better UX
- Handle optimistic updates

### Form Validation
- Client-side validation with real-time feedback
- Server-side validation error handling
- Form persistence across refreshes

## Quick Checklist for New CRUD Feature
- [ ] Backend controller with CRUD endpoints
- [ ] Service layer business logic
- [ ] Repository database operations
- [ ] Frontend API service functions
- [ ] React components for each operation
- [ ] Form validation and error handling
- [ ] Loading states and user feedback
- [ ] Routing and navigation
- [ ] Testing (unit, integration, e2e)
