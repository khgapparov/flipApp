package main

import (
    "os"
	"fmt"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	consul "github.com/hashicorp/consul/api"
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

func main() {
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

// CreateUserProfile is a placeholder handler for the CreateUserProfile operation
func CreateUserProfile(c *gin.Context) {
	c.JSON(http.StatusCreated, gin.H{
		"message": "Placeholder for CreateUserProfile",
	})
}

// GetUserProfile is a placeholder handler for the GetUserProfile operation
func GetUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	c.JSON(http.StatusOK, gin.H{
		"message": "Placeholder for GetUserProfile",
		"userId":  userId,
	})
}

// UpdateUserProfile is a placeholder handler for the UpdateUserProfile operation
func UpdateUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	c.JSON(http.StatusOK, gin.H{
		"message": "Placeholder for UpdateUserProfile",
		"userId":  userId,
	})
}

// ListUsers is a placeholder handler for the ListUsers operation
func ListUsers(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"message": "Placeholder for ListUsers",
		"users":   []UserProfile{},
	})
}
