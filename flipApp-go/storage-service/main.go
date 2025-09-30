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
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	consul "github.com/hashicorp/consul/api"
)

const (
	SERVICE_NAME  = "storage-service"
	SERVICE_PORT  = 8085
	STORAGE_DIR   = "./storage"
	MAX_FILE_SIZE = 100 << 20 // 100 MB
)

// FileMetadata corresponds to the FileMetadata structure in storage.smithy
type FileMetadata struct {
	FileID      string                 `json:"fileId"`
	Filename    string                 `json:"filename"`
	ContentType string                 `json:"contentType"`
	Size        int64                  `json:"size"`
	CreatedAt   string                 `json:"createdAt"`
	URL         string                 `json:"url"`
	Tags        map[string]string      `json:"tags,omitempty"`
	Metadata    map[string]interface{} `json:"metadata,omitempty"`
}

// Storage interface defines the contract for different storage implementations
type Storage interface {
	Save(fileID string, filename string, contentType string, data []byte, tags map[string]string, metadata map[string]interface{}) (*FileMetadata, error)
	Get(fileID string) ([]byte, *FileMetadata, error)
	Delete(fileID string) error
	GetURL(fileID string) (string, error)
	List(limit, offset int, tags map[string]string) ([]FileMetadata, int, error)
}

// FileStorage implements Storage interface for local file system
type FileStorage struct {
	baseDir string
	mu      sync.RWMutex
	files   map[string]FileMetadata
}

// NewFileStorage creates a new file storage implementation
func NewFileStorage(baseDir string) *FileStorage {
	return &FileStorage{
		baseDir: baseDir,
		files:   make(map[string]FileMetadata),
	}
}

func (fs *FileStorage) Save(fileID string, filename string, contentType string, data []byte, tags map[string]string, metadata map[string]interface{}) (*FileMetadata, error) {
	fs.mu.Lock()
	defer fs.mu.Unlock()

	// Create file path
	filePath := filepath.Join(fs.baseDir, fileID)

	// Ensure directory exists
	if err := os.MkdirAll(fs.baseDir, 0755); err != nil {
		return nil, fmt.Errorf("failed to create storage directory: %v", err)
	}

	// Write file
	if err := os.WriteFile(filePath, data, 0644); err != nil {
		return nil, fmt.Errorf("failed to save file: %v", err)
	}

	// Create metadata
	fileMetadata := FileMetadata{
		FileID:      fileID,
		Filename:    filename,
		ContentType: contentType,
		Size:        int64(len(data)),
		CreatedAt:   time.Now().Format(time.RFC3339),
		URL:         fmt.Sprintf("/static/%s", fileID),
		Tags:        tags,
		Metadata:    metadata,
	}

	// Store metadata
	fs.files[fileID] = fileMetadata

	return &fileMetadata, nil
}

func (fs *FileStorage) Get(fileID string) ([]byte, *FileMetadata, error) {
	fs.mu.RLock()
	defer fs.mu.RUnlock()

	// Check if file exists in metadata
	metadata, exists := fs.files[fileID]
	if !exists {
		return nil, nil, fmt.Errorf("file not found: %s", fileID)
	}

	// Read file content
	filePath := filepath.Join(fs.baseDir, fileID)
	data, err := os.ReadFile(filePath)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to read file: %v", err)
	}

	return data, &metadata, nil
}

func (fs *FileStorage) Delete(fileID string) error {
	fs.mu.Lock()
	defer fs.mu.Unlock()

	// Check if file exists
	if _, exists := fs.files[fileID]; !exists {
		return fmt.Errorf("file not found: %s", fileID)
	}

	// Delete file
	filePath := filepath.Join(fs.baseDir, fileID)
	if err := os.Remove(filePath); err != nil {
		return fmt.Errorf("failed to delete file: %v", err)
	}

	// Remove metadata
	delete(fs.files, fileID)

	return nil
}

func (fs *FileStorage) GetURL(fileID string) (string, error) {
	fs.mu.RLock()
	defer fs.mu.RUnlock()

	// Check if file exists
	if _, exists := fs.files[fileID]; !exists {
		return "", fmt.Errorf("file not found: %s", fileID)
	}

	return fmt.Sprintf("/static/%s", fileID), nil
}

func (fs *FileStorage) List(limit, offset int, tags map[string]string) ([]FileMetadata, int, error) {
	fs.mu.RLock()
	defer fs.mu.RUnlock()

	// Convert map to slice for filtering
	var allFiles []FileMetadata
	for _, file := range fs.files {
		allFiles = append(allFiles, file)
	}

	// Filter by tags if provided
	var filteredFiles []FileMetadata
	if len(tags) > 0 {
		for _, file := range allFiles {
			match := true
			for key, value := range tags {
				if file.Tags[key] != value {
					match = false
					break
				}
			}
			if match {
				filteredFiles = append(filteredFiles, file)
			}
		}
	} else {
		filteredFiles = allFiles
	}

	total := len(filteredFiles)

	// Apply pagination
	start := offset
	if start > total {
		start = total
	}
	end := start + limit
	if end > total {
		end = total
	}

	if start >= total {
		return []FileMetadata{}, total, nil
	}

	return filteredFiles[start:end], total, nil
}

// StorageService handles HTTP requests and delegates to storage implementation
type StorageService struct {
	storage Storage
}

func NewStorageService(storage Storage) *StorageService {
	return &StorageService{
		storage: storage,
	}
}

var (
	storageService *StorageService
)

func main() {
	// Create storage directory if it doesn't exist
	if err := os.MkdirAll(STORAGE_DIR, 0755); err != nil {
		log.Fatalf("Failed to create storage directory: %v", err)
	}

	// Initialize storage service with file storage implementation
	fileStorage := NewFileStorage(STORAGE_DIR)
	storageService = NewStorageService(fileStorage)

	registerServiceWithConsul()

	router := gin.Default()

	// Health check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on storage.smithy
	router.POST("/files", UploadFileHandler)
	router.GET("/files/:fileId", GetFileHandler)
	router.DELETE("/files/:fileId", DeleteFileHandler)
	router.GET("/files/:fileId/url", GetFileURLHandler)
	router.GET("/files", ListFilesHandler)

	// Serve stored files from a different path to avoid routing conflicts
	router.Static("/static", STORAGE_DIR)

	fmt.Printf("Storage Service started on port %d\n", SERVICE_PORT)
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
		serviceAddr = "storage-service" // Use service name in Docker network
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

func UploadFileHandler(c *gin.Context) {
	// Parse multipart form
	if err := c.Request.ParseMultipartForm(MAX_FILE_SIZE); err != nil {
		c.JSON(http.StatusRequestEntityTooLarge, gin.H{"error": "File too large"})
		return
	}

	// Get file from form
	file, header, err := c.Request.FormFile("file")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "No file provided"})
		return
	}
	defer file.Close()

	// Read file content
	fileContent, err := io.ReadAll(file)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read file"})
		return
	}

	// Get form values
	filename := c.Request.FormValue("filename")
	if filename == "" {
		filename = header.Filename
	}

	// Parse tags (comma-separated key=value pairs)
	tags := make(map[string]string)
	tagsStr := c.Request.FormValue("tags")
	if tagsStr != "" {
		pairs := strings.Split(tagsStr, ",")
		for _, pair := range pairs {
			kv := strings.Split(pair, "=")
			if len(kv) == 2 {
				tags[strings.TrimSpace(kv[0])] = strings.TrimSpace(kv[1])
			}
		}
	}

	// Parse metadata (simple key=value pairs for now)
	metadata := make(map[string]interface{})
	metadataStr := c.Request.FormValue("metadata")
	if metadataStr != "" {
		pairs := strings.Split(metadataStr, ",")
		for _, pair := range pairs {
			kv := strings.Split(pair, "=")
			if len(kv) == 2 {
				metadata[strings.TrimSpace(kv[0])] = strings.TrimSpace(kv[1])
			}
		}
	}

	// Generate file ID
	hash := md5.New()
	hash.Write([]byte(fmt.Sprintf("%s%d", filename, time.Now().UnixNano())))
	fileID := hex.EncodeToString(hash.Sum(nil))

	// Save file using storage service
	fileMetadata, err := storageService.storage.Save(
		fileID,
		filename,
		header.Header.Get("Content-Type"),
		fileContent,
		tags,
		metadata,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to save file: %v", err)})
		return
	}

	c.JSON(http.StatusCreated, gin.H{"file": fileMetadata})
}

func GetFileHandler(c *gin.Context) {
	fileID := c.Param("fileId")

	// Get file content and metadata
	data, metadata, err := storageService.storage.Get(fileID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "File not found"})
		return
	}

	// Set headers
	c.Header("Content-Type", metadata.ContentType)
	c.Header("Content-Length", strconv.FormatInt(metadata.Size, 10))
	c.Header("Content-Disposition", fmt.Sprintf("inline; filename=\"%s\"", metadata.Filename))

	// Send file content
	c.Data(http.StatusOK, metadata.ContentType, data)
}

func DeleteFileHandler(c *gin.Context) {
	fileID := c.Param("fileId")

	err := storageService.storage.Delete(fileID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "File not found"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"success": true})
}

func GetFileURLHandler(c *gin.Context) {
	fileID := c.Param("fileId")

	url, err := storageService.storage.GetURL(fileID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "File not found"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"url": url})
}

func ListFilesHandler(c *gin.Context) {
	// Parse query parameters
	limit := 10
	if limitStr := c.Query("limit"); limitStr != "" {
		if l, err := strconv.Atoi(limitStr); err == nil && l > 0 {
			limit = l
		}
	}

	offset := 0
	if offsetStr := c.Query("offset"); offsetStr != "" {
		if o, err := strconv.Atoi(offsetStr); err == nil && o >= 0 {
			offset = o
		}
	}

	// Parse tags
	tags := make(map[string]string)
	tagsStr := c.Query("tags")
	if tagsStr != "" {
		pairs := strings.Split(tagsStr, ",")
		for _, pair := range pairs {
			kv := strings.Split(pair, "=")
			if len(kv) == 2 {
				tags[strings.TrimSpace(kv[0])] = strings.TrimSpace(kv[1])
			}
		}
	}

	// List files using storage service
	files, total, err := storageService.storage.List(limit, offset, tags)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to list files"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"files": files,
		"total": total,
	})
}

func generateID(prefix string) string {
	return fmt.Sprintf("%s_%d", prefix, time.Now().UnixNano())
}
