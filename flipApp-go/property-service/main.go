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
	SERVICE_NAME = "property-service"
	SERVICE_PORT = 8082
)

// Based on the Smithy definition in property.smithy

// Address corresponds to the Address structure in property.smithy
type Address struct {
	Street  string `json:"street"`
	City    string `json:"city"`
	State   string `json:"state"`
	ZipCode string `json:"zipCode"`
}

// Property corresponds to the Property structure in property.smithy
type Property struct {
	PropertyID    string   `json:"propertyId"`
	Address       Address  `json:"address"`
	SquareFootage *int     `json:"squareFootage,omitempty"`
	Bedrooms      *float32 `json:"bedrooms,omitempty"`
	Bathrooms     *float32 `json:"bathrooms,omitempty"`
	LotSize       *float32 `json:"lotSize,omitempty"`
	YearBuilt     *int     `json:"yearBuilt,omitempty"`
	PropertyType  *string  `json:"propertyType,omitempty"`
	CreatedAt     string   `json:"createdAt"`
	UpdatedAt     string   `json:"updatedAt"`
}

func main() {
	registerServiceWithConsul()

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on property.smithy
	api := router.Group("/properties")
	{
		api.POST("", CreateProperty)
		api.GET("/:propertyId", GetProperty)
		api.PUT("/:propertyId", UpdateProperty)
		api.GET("", ListProperties)
	}

	fmt.Printf("Property Service started on port %d\n", SERVICE_PORT)
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

// In-memory storage for demonstration (replace with database in production)
var properties = make(map[string]Property)

func CreateProperty(c *gin.Context) {
	var input struct {
		Address       Address  `json:"address" binding:"required"`
		SquareFootage *int     `json:"squareFootage"`
		Bedrooms      *float32 `json:"bedrooms"`
		Bathrooms     *float32 `json:"bathrooms"`
		LotSize       *float32 `json:"lotSize"`
		YearBuilt     *int     `json:"yearBuilt"`
		PropertyType  *string  `json:"propertyType"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	propertyID := fmt.Sprintf("property_%d", time.Now().UnixNano())
	property := Property{
		PropertyID:    propertyID,
		Address:       input.Address,
		SquareFootage: input.SquareFootage,
		Bedrooms:      input.Bedrooms,
		Bathrooms:     input.Bathrooms,
		LotSize:       input.LotSize,
		YearBuilt:     input.YearBuilt,
		PropertyType:  input.PropertyType,
		CreatedAt:     time.Now().Format(time.RFC3339),
		UpdatedAt:     time.Now().Format(time.RFC3339),
	}

	properties[propertyID] = property
	c.JSON(http.StatusCreated, gin.H{"property": property})
}

func GetProperty(c *gin.Context) {
	propertyId := c.Param("propertyId")
	property, exists := properties[propertyId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Property not found"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"property": property})
}

func UpdateProperty(c *gin.Context) {
	propertyId := c.Param("propertyId")
	property, exists := properties[propertyId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Property not found"})
		return
	}

	var input struct {
		Address       Address  `json:"address" binding:"required"`
		SquareFootage *int     `json:"squareFootage"`
		Bedrooms      *float32 `json:"bedrooms"`
		Bathrooms     *float32 `json:"bathrooms"`
		LotSize       *float32 `json:"lotSize"`
		YearBuilt     *int     `json:"yearBuilt"`
		PropertyType  *string  `json:"propertyType"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	property.Address = input.Address
	property.SquareFootage = input.SquareFootage
	property.Bedrooms = input.Bedrooms
	property.Bathrooms = input.Bathrooms
	property.LotSize = input.LotSize
	property.YearBuilt = input.YearBuilt
	property.PropertyType = input.PropertyType
	property.UpdatedAt = time.Now().Format(time.RFC3339)

	properties[propertyId] = property
	c.JSON(http.StatusOK, gin.H{"property": property})
}

func ListProperties(c *gin.Context) {
	propertyList := make([]Property, 0, len(properties))
	for _, property := range properties {
		propertyList = append(propertyList, property)
	}
	c.JSON(http.StatusOK, gin.H{"properties": propertyList})
}
