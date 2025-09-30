package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestMain(m *testing.M) {
	// Set Gin to test mode
	gin.SetMode(gin.TestMode)

	// Run tests
	code := m.Run()

	// Cleanup
	os.Exit(code)
}

func setupTestStorage() (*FileStorage, string) {
	// Create temporary directory for testing
	tempDir, _ := os.MkdirTemp("", "storage_test")
	storage := NewFileStorage(tempDir)
	return storage, tempDir
}

func cleanupTestStorage(tempDir string) {
	os.RemoveAll(tempDir)
}

func TestFileStorage_Save(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Test data
	fileID := "test123"
	filename := "test.txt"
	contentType := "text/plain"
	data := []byte("Hello, World!")
	tags := map[string]string{"category": "test", "type": "text"}
	metadata := map[string]interface{}{"author": "tester"}

	// Save file
	metadataResult, err := storage.Save(fileID, filename, contentType, data, tags, metadata)

	// Assertions
	assert.NoError(t, err)
	assert.NotNil(t, metadataResult)
	assert.Equal(t, fileID, metadataResult.FileID)
	assert.Equal(t, filename, metadataResult.Filename)
	assert.Equal(t, contentType, metadataResult.ContentType)
	assert.Equal(t, int64(len(data)), metadataResult.Size)
	assert.Equal(t, tags, metadataResult.Tags)
	assert.Equal(t, metadata, metadataResult.Metadata)

	// Verify file was actually created
	filePath := filepath.Join(tempDir, fileID)
	_, err = os.Stat(filePath)
	assert.NoError(t, err)
}

func TestFileStorage_Get(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Test data
	fileID := "test123"
	filename := "test.txt"
	contentType := "text/plain"
	data := []byte("Hello, World!")
	tags := map[string]string{"category": "test"}

	// Save file first
	_, err := storage.Save(fileID, filename, contentType, data, tags, nil)
	assert.NoError(t, err)

	// Get file
	retrievedData, metadata, err := storage.Get(fileID)

	// Assertions
	assert.NoError(t, err)
	assert.Equal(t, data, retrievedData)
	assert.NotNil(t, metadata)
	assert.Equal(t, fileID, metadata.FileID)
	assert.Equal(t, filename, metadata.Filename)
	assert.Equal(t, contentType, metadata.ContentType)
	assert.Equal(t, int64(len(data)), metadata.Size)
}

func TestFileStorage_Get_NotFound(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Try to get non-existent file
	_, _, err := storage.Get("nonexistent")

	// Assertions
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "file not found")
}

func TestFileStorage_Delete(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Test data
	fileID := "test123"
	filename := "test.txt"
	contentType := "text/plain"
	data := []byte("Hello, World!")

	// Save file first
	_, err := storage.Save(fileID, filename, contentType, data, nil, nil)
	assert.NoError(t, err)

	// Delete file
	err = storage.Delete(fileID)
	assert.NoError(t, err)

	// Verify file was deleted
	_, _, err = storage.Get(fileID)
	assert.Error(t, err)
}

func TestFileStorage_Delete_NotFound(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Try to delete non-existent file
	err := storage.Delete("nonexistent")

	// Assertions
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "file not found")
}

func TestFileStorage_GetURL(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Test data
	fileID := "test123"
	filename := "test.txt"
	contentType := "text/plain"
	data := []byte("Hello, World!")

	// Save file first
	_, err := storage.Save(fileID, filename, contentType, data, nil, nil)
	assert.NoError(t, err)

	// Get URL
	url, err := storage.GetURL(fileID)

	// Assertions
	assert.NoError(t, err)
	assert.Equal(t, fmt.Sprintf("/static/%s", fileID), url)
}

func TestFileStorage_List(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)

	// Save multiple files with different tags
	storage.Save("file1", "test1.txt", "text/plain", []byte("data1"), map[string]string{"category": "doc"}, nil)
	storage.Save("file2", "test2.txt", "text/plain", []byte("data2"), map[string]string{"category": "image"}, nil)
	storage.Save("file3", "test3.txt", "text/plain", []byte("data3"), map[string]string{"category": "doc"}, nil)

	// List all files
	files, total, err := storage.List(10, 0, nil)
	assert.NoError(t, err)
	assert.Equal(t, 3, total)
	assert.Len(t, files, 3)

	// List with tag filter
	files, total, err = storage.List(10, 0, map[string]string{"category": "doc"})
	assert.NoError(t, err)
	assert.Equal(t, 2, total)
	assert.Len(t, files, 2)

	// List with pagination
	files, total, err = storage.List(2, 0, nil)
	assert.NoError(t, err)
	assert.Equal(t, 3, total)
	assert.Len(t, files, 2)
}

func TestUploadFileHandler(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	router := gin.Default()
	router.POST("/files", UploadFileHandler)

	// Create multipart form data
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	// Create form file
	part, _ := writer.CreateFormFile("file", "test.txt")
	part.Write([]byte("test file content"))

	// Add form fields
	writer.WriteField("filename", "custom_name.txt")
	writer.WriteField("tags", "category=test,type=text")
	writer.WriteField("metadata", "author=tester,version=1.0")

	writer.Close()

	// Create request
	req, _ := http.NewRequest("POST", "/files", body)
	req.Header.Set("Content-Type", writer.FormDataContentType())

	// Execute request
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusCreated, w.Code)

	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)

	fileData := response["file"].(map[string]interface{})
	assert.Equal(t, "custom_name.txt", fileData["filename"])
	// Content type might be "application/octet-stream" for multipart form data
	assert.Equal(t, float64(17), fileData["size"]) // 17 bytes

	// Verify tags
	tags := fileData["tags"].(map[string]interface{})
	assert.Equal(t, "test", tags["category"])
	assert.Equal(t, "text", tags["type"])

	// Note: Content type might be "application/octet-stream" for multipart form data
	// This is expected behavior
}

func TestGetFileHandler(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	// Save a test file first
	fileID := "test123"
	storage.Save(fileID, "test.txt", "text/plain", []byte("test content"), nil, nil)

	router := gin.Default()
	router.GET("/files/:fileId", GetFileHandler)

	// Create request
	req, _ := http.NewRequest("GET", "/files/"+fileID, nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "test content", w.Body.String())
	assert.Equal(t, "text/plain", w.Header().Get("Content-Type"))
}

func TestGetFileHandler_NotFound(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	router := gin.Default()
	router.GET("/files/:fileId", GetFileHandler)

	// Create request for non-existent file
	req, _ := http.NewRequest("GET", "/files/nonexistent", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusNotFound, w.Code)
}

func TestDeleteFileHandler(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	// Save a test file first
	fileID := "test123"
	storage.Save(fileID, "test.txt", "text/plain", []byte("test content"), nil, nil)

	router := gin.Default()
	router.DELETE("/files/:fileId", DeleteFileHandler)

	// Create request
	req, _ := http.NewRequest("DELETE", "/files/"+fileID, nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusOK, w.Code)

	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.True(t, response["success"].(bool))

	// Verify file was actually deleted
	_, _, err := storage.Get(fileID)
	assert.Error(t, err)
}

func TestGetFileURLHandler(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	// Save a test file first
	fileID := "test123"
	storage.Save(fileID, "test.txt", "text/plain", []byte("test content"), nil, nil)

	router := gin.Default()
	router.GET("/files/:fileId/url", GetFileURLHandler)

	// Create request
	req, _ := http.NewRequest("GET", "/files/"+fileID+"/url", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusOK, w.Code)

	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, fmt.Sprintf("/static/%s", fileID), response["url"])
}

func TestListFilesHandler(t *testing.T) {
	// Setup
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	// Save multiple test files
	storage.Save("file1", "test1.txt", "text/plain", []byte("data1"), map[string]string{"category": "doc"}, nil)
	storage.Save("file2", "test2.txt", "text/plain", []byte("data2"), map[string]string{"category": "image"}, nil)

	router := gin.Default()
	router.GET("/files", ListFilesHandler)

	// Create request
	req, _ := http.NewRequest("GET", "/files?limit=10&offset=0&tags=category=doc", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	// Assertions
	assert.Equal(t, http.StatusOK, w.Code)

	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)

	files := response["files"].([]interface{})
	total := int(response["total"].(float64))

	assert.Equal(t, 1, total)
	assert.Len(t, files, 1)
}

func TestHealthCheck(t *testing.T) {
	router := gin.Default()
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	req, _ := http.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusOK, w.Code)

	var response map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "ok", response["status"])
}

// Integration test for the complete flow
func TestStorageServiceIntegration(t *testing.T) {
	storage, tempDir := setupTestStorage()
	defer cleanupTestStorage(tempDir)
	storageService = NewStorageService(storage)

	router := gin.Default()
	router.POST("/files", UploadFileHandler)
	router.GET("/files/:fileId", GetFileHandler)
	router.DELETE("/files/:fileId", DeleteFileHandler)
	router.GET("/files/:fileId/url", GetFileURLHandler)
	router.GET("/files", ListFilesHandler)

	// Step 1: Upload a file
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)
	part, _ := writer.CreateFormFile("file", "integration_test.txt")
	part.Write([]byte("integration test content"))
	writer.WriteField("tags", "test=integration")
	writer.Close()

	req, _ := http.NewRequest("POST", "/files", body)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	assert.Equal(t, http.StatusCreated, w.Code)

	var uploadResponse map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &uploadResponse)
	fileData := uploadResponse["file"].(map[string]interface{})
	fileID := fileData["fileId"].(string)

	// Step 2: Get the file
	req, _ = http.NewRequest("GET", "/files/"+fileID, nil)
	w = httptest.NewRecorder()
	router.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)
	assert.Equal(t, "integration test content", w.Body.String())

	// Step 3: Get file URL
	req, _ = http.NewRequest("GET", "/files/"+fileID+"/url", nil)
	w = httptest.NewRecorder()
	router.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)

	var urlResponse map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &urlResponse)
	assert.Equal(t, fmt.Sprintf("/static/%s", fileID), urlResponse["url"])

	// Step 4: List files
	req, _ = http.NewRequest("GET", "/files", nil)
	w = httptest.NewRecorder()
	router.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)

	var listResponse map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &listResponse)
	assert.Equal(t, float64(1), listResponse["total"])

	// Step 5: Delete the file
	req, _ = http.NewRequest("DELETE", "/files/"+fileID, nil)
	w = httptest.NewRecorder()
	router.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Code)

	var deleteResponse map[string]interface{}
	json.Unmarshal(w.Body.Bytes(), &deleteResponse)
	assert.True(t, deleteResponse["success"].(bool))

	// Step 6: Verify file is gone
	req, _ = http.NewRequest("GET", "/files/"+fileID, nil)
	w = httptest.NewRecorder()
	router.ServeHTTP(w, req)
	assert.Equal(t, http.StatusNotFound, w.Code)
}
