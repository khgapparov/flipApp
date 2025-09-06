# Database Migration Guide

This guide covers the migration from in-memory storage to PostgreSQL database for the FlipApp backend services.

## Changes Made

### 1. Database Infrastructure
- Added PostgreSQL service to docker-compose.yml
- Created database configuration in `.env` file
- Added database connection utility package (`database/db.go`)
- Created database migration scripts

### 2. Auth Service Updates
- Replaced in-memory refresh token storage with PostgreSQL
- Added proper user registration with password hashing
- Implemented user authentication against database
- Added proper token refresh and logout functionality

### 3. User Service Updates  
- Replaced in-memory user storage with PostgreSQL
- Updated user profile retrieval from database
- Implemented user profile updates in database
- Added user listing from database

### 4. Property Service (TODO)
- Property service still uses in-memory storage
- Needs similar database migration as user service

## Database Schema

### Users Table
- `user_id` - Unique user identifier
- `email` - User email (unique)
- `password_hash` - Hashed password
- `profile_type` - PERSON or LLC
- Personal/company information fields
- Timestamps for creation and updates

### Refresh Tokens Table
- `token` - Refresh token string (unique)
- `user_id` - Reference to users table
- `expires_at` - Token expiration timestamp

### Properties Table (for property-service)
- `property_id` - Unique property identifier
- Address information fields
- Property details (square footage, bedrooms, etc.)
- Timestamps for creation and updates

## Testing the Implementation

### 1. Start the Database
```bash
cd flipApp-go
docker-compose up -d postgres
```

### 2. Run Database Setup
```bash
./test_database.sh
```

### 3. Start the Services
```bash
docker-compose up -d
```

### 4. Test Authentication
```bash
# Register a new user
curl -X POST http://localhost/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# Login
curl -X POST http://localhost/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# Use the refresh token to get new access token
curl -X POST http://localhost/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your_refresh_token_here"
  }'
```

### 5. Test User Service
```bash
# Get user profile (replace with actual user ID)
curl http://localhost/users/user_123456789

# Update user profile
curl -X PUT http://localhost/users/user_123456789 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe"
  }'

# List all users
curl http://localhost/users
```

## Production Considerations

### 1. Password Security
- Currently uses simple base64 encoding for demonstration
- **IMPORTANT**: Replace with proper bcrypt hashing in production
- Use environment variables for salt rounds

### 2. Database Connection Pooling
- Configured with sensible defaults (25 connections)
- Monitor and adjust based on production load

### 3. Indexing
- Basic indexes created for common queries
- Monitor query performance and add indexes as needed

### 4. Backup Strategy
- Implement regular database backups
- Consider using PostgreSQL replication for high availability

### 5. Security
- Use SSL for database connections in production
- Rotate database credentials regularly
- Implement proper secret management

## Next Steps

1. **Migrate Property Service**: Update property-service to use PostgreSQL
2. **Add Proper Password Hashing**: Implement bcrypt with proper salt
3. **Add Database Migrations**: Use proper migration tool (like golang-migrate)
4. **Add Monitoring**: Implement database performance monitoring
5. **Add Tests**: Create comprehensive database tests

## Troubleshooting

### Common Issues

1. **Database Connection Errors**: Check if PostgreSQL is running and credentials are correct
2. **Table Not Found**: Run the migration script to create tables
3. **Connection Timeouts**: Check network connectivity between services
4. **Permission Errors**: Verify database user has proper permissions

### Logs
Check service logs for detailed error information:
```bash
docker-compose logs auth-service
docker-compose logs user-service
docker-compose logs postgres
```

## Rollback Procedure

If issues occur, you can:
1. Stop the services: `docker-compose down`
2. Remove the database volume: `docker volume rm flipapp-go_postgres_data`
3. Revert to previous commit that used in-memory storage
4. Restart services without database dependency
