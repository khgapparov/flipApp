package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	consul "github.com/hashicorp/consul/api"
	_ "github.com/lib/pq"
)

const (
	SERVICE_NAME = "user-service"
	SERVICE_PORT = 8081
)

// Based on the Smithy definition in user.smithy

// ProfileType defines the type of user profile.
// @enum
type ProfileType string

const (
	Person ProfileType = "PERSON"
	LLC    ProfileType = "LLC"
)

// UserProfile corresponds to the UserProfile structure in user.smithy
type UserProfile struct {
	UserID      string      `json:"userId"`
	ProfileType ProfileType `json:"profileType"`
	Email       string      `json:"email"`
	FirstName   *string     `json:"firstName,omitempty"`
	LastName    *string     `json:"lastName,omitempty"`
	CompanyName *string     `json:"companyName,omitempty"`
	AvatarURL   *string     `json:"avatarUrl,omitempty"`
	PhoneNumber *string     `json:"phoneNumber,omitempty"`
	CreatedAt   string      `json:"createdAt"`
	UpdatedAt   string      `json:"updatedAt"`
}

var db *sql.DB

func main() {
	// Initialize database connection
	var err error
	db, err = connectToDatabase()
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	registerServiceWithConsul()

	router := gin.Default()

	// Health check endpoint for Consul
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on user.smithy
	api := router.Group("/users")
	{
		api.POST("", CreateUserProfile)
		api.GET("/:userId", GetUserProfile)
		api.PUT("/:userId", UpdateUserProfile)
		api.GET("", ListUsers)
	}

	fmt.Printf("User Service started on port %d\n", SERVICE_PORT)
	router.Run(fmt.Sprintf(":%d", SERVICE_PORT))
}

func registerServiceWithConsul() {
	// Get Consul address from environment variable or use default
	consulAddr := os.Getenv("CONSUL_ADDRESS")
	if consulAddr == "" {
		consulAddr = "consul:8500" // Use service name in Docker network
	}

	config := consul.DefaultConfig()
	config.Address = consulAddr
	consulClient, err := consul.NewClient(config)
	if err != nil {
		log.Fatalf("Failed to create consul client: %v", err)
	}

	// Get service address from environment variable or use service name
	serviceAddr := os.Getenv("SERVICE_ADDRESS")
	if serviceAddr == "" {
		serviceAddr = SERVICE_NAME // Use service name in Docker network
	}

	registration := &consul.AgentServiceRegistration{
		ID:      SERVICE_NAME,
		Name:    SERVICE_NAME,
		Port:    SERVICE_PORT,
		Address: serviceAddr,
		Check: &consul.AgentServiceCheck{
			HTTP:                           fmt.Sprintf("http://%s:%d/health", serviceAddr, SERVICE_PORT),
			Interval:                       "10s",
			Timeout:                        "1s",
			DeregisterCriticalServiceAfter: "1m",
		},
	}

	if err := consulClient.Agent().ServiceRegister(registration); err != nil {
		log.Fatalf("Failed to register service with consul: %v", err)
	}

	fmt.Printf("Successfully registered service '%s' with Consul at %s\n", SERVICE_NAME, consulAddr)
}

// Database helper functions
func getUserFromDB(userID string) (*UserProfile, error) {
	var user UserProfile
	err := db.QueryRow(`
		SELECT user_id, profile_type, email, first_name, last_name, 
		       company_name, avatar_url, phone_number, created_at, updated_at
		FROM users WHERE user_id = $1
	`, userID).Scan(
		&user.UserID, &user.ProfileType, &user.Email, &user.FirstName, &user.LastName,
		&user.CompanyName, &user.AvatarURL, &user.PhoneNumber, &user.CreatedAt, &user.UpdatedAt,
	)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("user not found")
		}
		return nil, err
	}
	return &user, nil
}

func updateUserInDB(userID string, firstName, lastName, companyName, avatarURL, phoneNumber *string) error {
	_, err := db.Exec(`
		UPDATE users 
		SET first_name = COALESCE($1, first_name),
		    last_name = COALESCE($2, last_name),
		    company_name = COALESCE($3, company_name),
		    avatar_url = COALESCE($4, avatar_url),
		    phone_number = COALESCE($5, phone_number),
		    updated_at = CURRENT_TIMESTAMP
		WHERE user_id = $6
	`, firstName, lastName, companyName, avatarURL, phoneNumber, userID)
	return err
}

func getAllUsersFromDB() ([]UserProfile, error) {
	rows, err := db.Query(`
		SELECT user_id, profile_type, email, first_name, last_name, 
		       company_name, avatar_url, phone_number, created_at, updated_at
		FROM users
	`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var users []UserProfile
	for rows.Next() {
		var user UserProfile
		err := rows.Scan(
			&user.UserID, &user.ProfileType, &user.Email, &user.FirstName, &user.LastName,
			&user.CompanyName, &user.AvatarURL, &user.PhoneNumber, &user.CreatedAt, &user.UpdatedAt,
		)
		if err != nil {
			return nil, err
		}
		users = append(users, user)
	}
	return users, nil
}

func CreateUserProfile(c *gin.Context) {
	var input struct {
		ProfileType ProfileType `json:"profileType" binding:"required"`
		Email       string      `json:"email" binding:"required"`
		FirstName   *string     `json:"firstName"`
		LastName    *string     `json:"lastName"`
		CompanyName *string     `json:"companyName"`
		AvatarURL   *string     `json:"avatarUrl"`
		PhoneNumber *string     `json:"phoneNumber"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Note: User creation is handled by auth-service during registration
	// This endpoint should only update existing user profiles
	c.JSON(http.StatusMethodNotAllowed, gin.H{"error": "Use auth/register endpoint to create users"})
}

func GetUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	user, err := getUserFromDB(userId)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"profile": user})
}

func UpdateUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	
	// Check if user exists
	_, err := getUserFromDB(userId)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}

	var input struct {
		FirstName   *string `json:"firstName"`
		LastName    *string `json:"lastName"`
		CompanyName *string `json:"companyName"`
		AvatarURL   *string `json:"avatarUrl"`
		PhoneNumber *string `json:"phoneNumber"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Update user in database
	if err := updateUserInDB(userId, input.FirstName, input.LastName, input.CompanyName, input.AvatarURL, input.PhoneNumber); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user profile"})
		return
	}

	// Get updated user profile
	updatedUser, err := getUserFromDB(userId)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get updated user profile"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"profile": updatedUser})
}

func ListUsers(c *gin.Context) {
	users, err := getAllUsersFromDB()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to retrieve users"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"users": users})
}

// connectToDatabase establishes a connection to PostgreSQL database
func connectToDatabase() (*sql.DB, error) {
	// Get database configuration from environment variables or use defaults
	host := getEnv("DB_HOST", "postgres")
	port := getEnv("DB_PORT", "5432")
	user := getEnv("DB_USER", "user")
	password := getEnv("DB_PASSWORD", "password")
	dbname := getEnv("DB_NAME", "flipapp")
	sslmode := getEnv("DB_SSLMODE", "disable")

	connStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		host, port, user, password, dbname, sslmode)

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		return nil, fmt.Errorf("failed to open database connection: %v", err)
	}

	// Set connection pool settings
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(25)
	db.SetConnMaxLifetime(5 * time.Minute)

	// Test the connection
	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %v", err)
	}

	log.Println("Successfully connected to PostgreSQL database")
	return db, nil
}

// getEnv gets environment variable or returns default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
