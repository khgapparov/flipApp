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
	SERVICE_NAME = "gallery-service"
	SERVICE_PORT = 8084
)

// Based on the Smithy definition in gallery.smithy

// ImageCategory defines the category of an image.
// @enum
type ImageCategory string

const (
	BEFORE        ImageCategory = "BEFORE"
	AFTER         ImageCategory = "AFTER"
	PROGRESS      ImageCategory = "PROGRESS"
	MARKETING     ImageCategory = "MARKETING"
	RECEIPT       ImageCategory = "RECEIPT"
	UNCATEGORIZED ImageCategory = "UNCATEGORIZED"
)

// Image corresponds to the Image structure in gallery.smithy
type Image struct {
	ImageID     string        `json:"imageId"`
	PropertyID  string        `json:"propertyId"`
	URL         string        `json:"url"`
	Category    ImageCategory `json:"category"`
	Description *string       `json:"description,omitempty"`
	CreatedAt   string        `json:"createdAt"`
}

// Album corresponds to the Album structure in gallery.smithy
type Album struct {
	AlbumID     string  `json:"albumId"`
	PropertyID  string  `json:"propertyId"`
	Title       string  `json:"title"`
	Description *string `json:"description,omitempty"`
	CreatedAt   string  `json:"createdAt"`
}

func main() {
	registerServiceWithConsul()

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on gallery.smithy
	router.POST("/images", UploadImage)
	router.GET("/images/:imageId", GetImage)
	router.GET("/properties/:propertyId/images", ListImagesForProperty)
	router.POST("/albums", CreateAlbum)
	router.GET("/albums/:albumId", GetAlbum)
	router.POST("/albums/:albumId/images", AddImageToAlbum)

	fmt.Printf("Gallery Service started on port %d\n", SERVICE_PORT)
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

func UploadImage(c *gin.Context) {
	c.JSON(http.StatusCreated, gin.H{"message": "Placeholder for UploadImage"})
}

func GetImage(c *gin.Context) {
	imageId := c.Param("imageId")
	c.JSON(http.StatusOK, gin.H{"message": "Placeholder for GetImage", "imageId": imageId})
}

func ListImagesForProperty(c *gin.Context) {
	propertyId := c.Param("propertyId")
	c.JSON(http.StatusOK, gin.H{"message": "Placeholder for ListImagesForProperty", "propertyId": propertyId, "images": []Image{}})
}

func CreateAlbum(c *gin.Context) {
	c.JSON(http.StatusCreated, gin.H{"message": "Placeholder for CreateAlbum"})
}

func GetAlbum(c *gin.Context) {
	albumId := c.Param("albumId")
	c.JSON(http.StatusOK, gin.H{"message": "Placeholder for GetAlbum", "albumId": albumId})
}

func AddImageToAlbum(c *gin.Context) {
	albumId := c.Param("albumId")
	c.JSON(http.StatusOK, gin.H{"message": "Placeholder for AddImageToAlbum", "albumId": albumId})
}
