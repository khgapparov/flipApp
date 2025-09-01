package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

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

var users = make(map[string]UserProfile)

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

	userID := fmt.Sprintf("user_%d", time.Now().UnixNano())
	user := UserProfile{
		UserID:      userID,
		ProfileType: input.ProfileType,
		Email:       input.Email,
		FirstName:   input.FirstName,
		LastName:    input.LastName,
		CompanyName: input.CompanyName,
		AvatarURL:   input.AvatarURL,
		PhoneNumber: input.PhoneNumber,
		CreatedAt:   time.Now().Format(time.RFC3339),
		UpdatedAt:   time.Now().Format(time.RFC3339),
	}

	users[userID] = user
	c.JSON(http.StatusCreated, gin.H{"profile": user})
}

func GetUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	user, exists := users[userId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"profile": user})
}

func UpdateUserProfile(c *gin.Context) {
	userId := c.Param("userId")
	user, exists := users[userId]
	if !exists {
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

	user.FirstName = input.FirstName
	user.LastName = input.LastName
	user.CompanyName = input.CompanyName
	user.AvatarURL = input.AvatarURL
	user.PhoneNumber = input.PhoneNumber
	user.UpdatedAt = time.Now().Format(time.RFC3339)

	users[userId] = user
	c.JSON(http.StatusOK, gin.H{"profile": user})
}

func ListUsers(c *gin.Context) {
	userList := make([]UserProfile, 0, len(users))
	for _, user := range users {
		userList = append(userList, user)
	}
	c.JSON(http.StatusOK, gin.H{"users": userList})
}