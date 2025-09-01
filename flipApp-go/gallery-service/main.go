package main

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	consul "github.com/hashicorp/consul/api"
)

const (
	SERVICE_NAME = "gallery-service"
	SERVICE_PORT = 8084
	UPLOAD_DIR   = "./uploads"
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
	AlbumID     string   `json:"albumId"`
	PropertyID  string   `json:"propertyId"`
	Title       string   `json:"title"`
	Description *string  `json:"description,omitempty"`
	CreatedAt   string   `json:"createdAt"`
	ImageIDs    []string `json:"imageIds,omitempty"`
}

// Storage for images and albums
var (
	images         = make(map[string]Image)
	albums         = make(map[string]Album)
	propertyImages = make(map[string][]string) // propertyID -> []imageIDs
	albumImages    = make(map[string][]string) // albumID -> []imageIDs
	mu             sync.RWMutex
)

func main() {
	// Create upload directory if it doesn't exist
	if err := os.MkdirAll(UPLOAD_DIR, 0755); err != nil {
		log.Fatalf("Failed to create upload directory: %v", err)
	}

	registerServiceWithConsul()

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// Serve uploaded files
	router.Static("/uploads", UPLOAD_DIR)

	// API routes based on gallery.smithy
	router.POST("/images", UploadImage)
	router.GET("/images/:imageId", GetImage)
	router.GET("/properties/:propertyId/images", ListImagesForProperty)
	router.POST("/albums", CreateAlbum)
	router.GET("/albums/:albumId", GetAlbum)
	router.POST("/albums/:albumId/images", AddImageToAlbum)

	// Additional endpoints for flipping progress tracking
	router.GET("/properties/:propertyId/progress", GetPropertyProgress)
	router.GET("/properties/:propertyId/images/category/:category", ListImagesByCategory)

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
		log.Printf("Warning: Failed to create consul client: %v (service will continue without Consul)", err)
		return
	}

	// Get service address from environment variable or use service name
	serviceAddr := os.Getenv("SERVICE_ADDRESS")
	if serviceAddr == "" {
		serviceAddr = "gallery-service" // Use service name in Docker network
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
		log.Printf("Warning: Failed to register service with consul: %v (service will continue without Consul)", err)
		return
	}

	fmt.Printf("Successfully registered service '%s' with Consul at %s\n", SERVICE_NAME, consulAddr)
}

func UploadImage(c *gin.Context) {
	// Parse multipart form
	if err := c.Request.ParseMultipartForm(10 << 20); err != nil { // 10 MB max
		c.JSON(http.StatusBadRequest, gin.H{"error": "Failed to parse form"})
		return
	}

	propertyID := c.Request.FormValue("propertyId")
	category := ImageCategory(c.Request.FormValue("category"))
	description := c.Request.FormValue("description")
	filename := c.Request.FormValue("filename")

	if propertyID == "" || category == "" || filename == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing required fields: propertyId, category, filename"})
		return
	}

	// Validate category
	validCategories := map[ImageCategory]bool{
		BEFORE: true, AFTER: true, PROGRESS: true,
		MARKETING: true, RECEIPT: true, UNCATEGORIZED: true,
	}
	if !validCategories[category] {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid category"})
		return
	}

	file, header, err := c.Request.FormFile("image")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "No image file provided"})
		return
	}
	defer file.Close()

	// Generate unique filename
	hash := md5.New()
	hash.Write([]byte(fmt.Sprintf("%s%d", filename, time.Now().UnixNano())))
	fileExt := filepath.Ext(header.Filename)
	newFilename := hex.EncodeToString(hash.Sum(nil)) + fileExt
	filePath := filepath.Join(UPLOAD_DIR, newFilename)

	// Create file
	outFile, err := os.Create(filePath)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create file"})
		return
	}
	defer outFile.Close()

	// Copy file content
	if _, err := io.Copy(outFile, file); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save file"})
		return
	}

	// Create image record
	mu.Lock()
	defer mu.Unlock()

	imageID := generateID("img")
	image := Image{
		ImageID:    imageID,
		PropertyID: propertyID,
		URL:        fmt.Sprintf("/uploads/%s", newFilename),
		Category:   category,
		CreatedAt:  time.Now().Format(time.RFC3339),
	}

	if description != "" {
		image.Description = &description
	}

	images[imageID] = image

	// Add to property's image list
	propertyImages[propertyID] = append(propertyImages[propertyID], imageID)

	c.JSON(http.StatusCreated, gin.H{"image": image})
}

func GetImage(c *gin.Context) {
	imageID := c.Param("imageId")

	mu.RLock()
	image, exists := images[imageID]
	mu.RUnlock()

	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Image not found"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"image": image})
}

func ListImagesForProperty(c *gin.Context) {
	propertyID := c.Param("propertyId")

	mu.RLock()
	defer mu.RUnlock()

	imageIDs, exists := propertyImages[propertyID]
	if !exists {
		c.JSON(http.StatusOK, gin.H{"images": []Image{}})
		return
	}

	var result []Image
	for _, imageID := range imageIDs {
		if image, exists := images[imageID]; exists {
			result = append(result, image)
		}
	}

	c.JSON(http.StatusOK, gin.H{"images": result})
}

func ListImagesByCategory(c *gin.Context) {
	propertyID := c.Param("propertyId")
	category := ImageCategory(strings.ToUpper(c.Param("category")))

	mu.RLock()
	defer mu.RUnlock()

	imageIDs, exists := propertyImages[propertyID]
	if !exists {
		c.JSON(http.StatusOK, gin.H{"images": []Image{}})
		return
	}

	var result []Image
	for _, imageID := range imageIDs {
		if image, exists := images[imageID]; exists && image.Category == category {
			result = append(result, image)
		}
	}

	c.JSON(http.StatusOK, gin.H{"images": result})
}

func CreateAlbum(c *gin.Context) {
	var input struct {
		PropertyID  string  `json:"propertyId" binding:"required"`
		Title       string  `json:"title" binding:"required"`
		Description *string `json:"description"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	mu.Lock()
	defer mu.Unlock()

	albumID := generateID("alb")
	album := Album{
		AlbumID:     albumID,
		PropertyID:  input.PropertyID,
		Title:       input.Title,
		Description: input.Description,
		CreatedAt:   time.Now().Format(time.RFC3339),
		ImageIDs:    []string{},
	}

	albums[albumID] = album
	albumImages[albumID] = []string{}

	c.JSON(http.StatusCreated, gin.H{"album": album})
}

func GetAlbum(c *gin.Context) {
	albumID := c.Param("albumId")

	mu.RLock()
	defer mu.RUnlock()

	album, exists := albums[albumID]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Album not found"})
		return
	}

	// Get album images
	var albumImagesList []Image
	if imageIDs, exists := albumImages[albumID]; exists {
		for _, imageID := range imageIDs {
			if image, exists := images[imageID]; exists {
				albumImagesList = append(albumImagesList, image)
			}
		}
	}

	response := gin.H{
		"album":  album,
		"images": albumImagesList,
	}

	c.JSON(http.StatusOK, response)
}

func AddImageToAlbum(c *gin.Context) {
	albumID := c.Param("albumId")

	var input struct {
		ImageID string `json:"imageId" binding:"required"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	mu.Lock()
	defer mu.Unlock()

	// Check if album exists
	_, albumExists := albums[albumID]
	if !albumExists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Album not found"})
		return
	}

	// Check if image exists
	_, imageExists := images[input.ImageID]
	if !imageExists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Image not found"})
		return
	}

	// Add image to album
	albumImages[albumID] = append(albumImages[albumID], input.ImageID)

	c.JSON(http.StatusOK, gin.H{"message": "Image added to album successfully"})
}

func GetPropertyProgress(c *gin.Context) {
	propertyID := c.Param("propertyId")

	mu.RLock()
	defer mu.RUnlock()

	// Get all progress images for the property
	var progressImages []Image
	if imageIDs, exists := propertyImages[propertyID]; exists {
		for _, imageID := range imageIDs {
			if image, exists := images[imageID]; exists && image.Category == PROGRESS {
				progressImages = append(progressImages, image)
			}
		}
	}

	// Sort by creation date (newest first)
	for i, j := 0, len(progressImages)-1; i < j; i, j = i+1, j-1 {
		progressImages[i], progressImages[j] = progressImages[j], progressImages[i]
	}

	response := gin.H{
		"propertyId":          propertyID,
		"progressImages":      progressImages,
		"totalProgressImages": len(progressImages),
		"latestUpdate":        nil,
	}

	if len(progressImages) > 0 {
		response["latestUpdate"] = progressImages[0].CreatedAt
	}

	c.JSON(http.StatusOK, response)
}

func generateID(prefix string) string {
	return fmt.Sprintf("%s_%d", prefix, time.Now().UnixNano())
}
