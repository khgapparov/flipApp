package main

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	consul "github.com/hashicorp/consul/api"
)

// serviceRegistry holds the discovered locations of our services
var serviceRegistry = make(map[string]*url.URL)
var jwtSecret []byte

func main() {
	// Initialize JWT secret
	initJWTSecret()

	// Get Consul address from environment variable or use default
	consulAddr := os.Getenv("CONSUL_ADDRESS")
	if consulAddr == "" {
		consulAddr = "consul:8500" // Use service name in Docker network
	}

	// Initialize Consul client with custom address
	config := consul.DefaultConfig()
	config.Address = consulAddr
	consulClient, err := consul.NewClient(config)
	if err != nil {
		log.Fatalf("Failed to create consul client: %v", err)
	}

	// Discover services at startup and start periodic discovery
	discoverServices(consulClient)
	go startPeriodicServiceDiscovery(consulClient)

	// Set up Gin router with CORS
	router := gin.Default()

	// Configure CORS middleware
	// Get allowed origins from environment variable or use default for development
	allowedOrigins := os.Getenv("ALLOWED_ORIGINS")
	var origins []string
	if allowedOrigins != "" {
		origins = strings.Split(allowedOrigins, ",")
	} else {
		// Default to allowing all origins in development
		origins = []string{"*"}
	}

	router.Use(cors.New(cors.Config{
		AllowOrigins:     origins,
		AllowMethods:     []string{"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Length", "Content-Type", "Authorization"},
		ExposeHeaders:    []string{"Content-Length"},
		AllowCredentials: true,
		MaxAge:           12 * time.Hour,
	}))

	// Public routes (no authentication required)
	public := router.Group("/api")
	{
		public.POST("/auth/register", proxyHandler)
		public.POST("/auth/login", proxyHandler)
		public.POST("/auth/validate", proxyHandler)
		public.POST("/auth/refresh", proxyHandler)
		public.POST("/auth/logout", proxyHandler)
	}

	// Protected routes (require authentication)
	protected := router.Group("/api")
	protected.Use(authMiddleware())
	{
		// Use specific routes instead of wildcard to avoid conflicts
		protected.GET("/users", proxyHandler)
		protected.POST("/users", proxyHandler)
		protected.GET("/users/*userPath", proxyHandler)
		protected.POST("/users/*userPath", proxyHandler)
		protected.GET("/properties", proxyHandler)
		protected.POST("/properties", proxyHandler)
		protected.GET("/properties/*propertyPath", proxyHandler)
		protected.POST("/properties/*propertyPath", proxyHandler)
		protected.GET("/projects", proxyHandler)
		protected.POST("/projects", proxyHandler)
		protected.GET("/projects/*projectPath", proxyHandler)
		protected.POST("/projects/*projectPath", proxyHandler)

		// Gallery service endpoints - all protected
		protected.GET("/gallery/health", proxyHandler)
		protected.GET("/gallery/images", proxyHandler)
		protected.POST("/gallery/images", proxyHandler)
		protected.GET("/gallery/images/:imageId", proxyHandler)
		protected.GET("/gallery/properties/:propertyId/images", proxyHandler)
		protected.POST("/gallery/albums", proxyHandler)
		protected.GET("/gallery/albums/:albumId", proxyHandler)
		protected.POST("/gallery/albums/:albumId/images", proxyHandler)
		protected.GET("/gallery/properties/:propertyId/progress", proxyHandler)
		protected.GET("/gallery/properties/:propertyId/images/category/:category", proxyHandler)

		// Storage service endpoints - all protected
		protected.GET("/storage/health", proxyHandler)
		protected.POST("/storage/files", proxyHandler)
		protected.GET("/storage/files/:fileId", proxyHandler)
		protected.DELETE("/storage/files/:fileId", proxyHandler)
		protected.GET("/storage/files/:fileId/url", proxyHandler)
		protected.GET("/storage/files", proxyHandler)

		protected.GET("/chat", proxyHandler)
		protected.POST("/chat", proxyHandler)
		protected.GET("/chat/*chatPath", proxyHandler)
		protected.POST("/chat/*chatPath", proxyHandler)
	}

	fmt.Println("API Gateway started on :9000")
	router.Run(":9000")
}

// discoverServices queries Consul and populates the serviceRegistry
func discoverServices(client *consul.Client) {
	// In a real app, this should run periodically or use watches.
	services, _, err := client.Catalog().Services(nil)
	if err != nil {
		log.Printf("Error discovering services: %v", err)
		return
	}

	for name, _ := range services {
		// We only care about our flipApp services, not consul itself
		if strings.HasSuffix(name, "-service") {
			serviceEntries, _, err := client.Health().Service(name, "", true, nil)
			if err != nil || len(serviceEntries) == 0 {
				log.Printf("Failed to get healthy instance for %s", name)
				continue
			}

			// For simplicity, we use the first healthy instance
			instance := serviceEntries[0].Service
			serviceURL, err := url.Parse(fmt.Sprintf("http://%s:%d", instance.Address, instance.Port))
			if err != nil {
				log.Printf("Error parsing service URL for %s: %v", name, err)
				continue
			}
			serviceRegistry[name] = serviceURL
			fmt.Printf("Discovered service: %s at %s\n", name, serviceURL)
		}
	}
}

func initJWTSecret() {
	// In production, load from environment variable or secret manager
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		// Generate a random secret for development (must match auth service secret)
		secretBytes := make([]byte, 32)
		if _, err := rand.Read(secretBytes); err != nil {
			log.Fatalf("Failed to generate JWT secret: %v", err)
		}
		secret = base64.StdEncoding.EncodeToString(secretBytes)
		fmt.Printf("Generated JWT secret: %s\n", secret)
	}
	jwtSecret = []byte(secret)
}

// startPeriodicServiceDiscovery periodically rediscovers services to handle
// services that may start after the API gateway
func startPeriodicServiceDiscovery(client *consul.Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			fmt.Println("Performing periodic service discovery...")
			discoverServices(client)
		}
	}
}

// Custom claims structure (must match auth service)
type CustomClaims struct {
	UserID string `json:"userId"`
	Email  string `json:"email"`
	jwt.RegisteredClaims
}

// authMiddleware validates JWT tokens for protected routes
func authMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from Authorization header
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization header required"})
			c.Abort()
			return
		}

		// Check if it's a Bearer token
		if !strings.HasPrefix(authHeader, "Bearer ") {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Bearer token required"})
			c.Abort()
			return
		}

		// Extract the token
		tokenString := strings.TrimPrefix(authHeader, "Bearer ")

		// Validate the token
		token, err := jwt.ParseWithClaims(tokenString, &CustomClaims{}, func(token *jwt.Token) (interface{}, error) {
			return jwtSecret, nil
		})

		if err != nil || !token.Valid {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid or expired token"})
			c.Abort()
			return
		}

		// Add user information to context for downstream services
		if claims, ok := token.Claims.(*CustomClaims); ok {
			c.Set("userId", claims.UserID)
			c.Set("userEmail", claims.Email)
		}

		c.Next()
	}
}

// proxyHandler determines the target service and forwards the request
func proxyHandler(c *gin.Context) {
	// Example: /api/auth/register -> target service is "auth-service"
	// Example: /api/users/123 -> target service is "user-service"
	fmt.Printf("Handling request: %s %s\n", c.Request.Method, c.Request.URL.Path)
	pathSegments := strings.Split(c.Request.URL.Path, "/")
	if len(pathSegments) < 3 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request path"})
		return
	}

	// Convention: /api/auth/* -> auth-service, /api/users/* -> user-service, etc.
	serviceName := pathSegments[2] + "-service"

	// Handle special cases where the path doesn't match the service name exactly
	if serviceName == "properties-service" {
		serviceName = "property-service"
	} else if serviceName == "users-service" {
		serviceName = "user-service"
	} else if serviceName == "projects-service" {
		serviceName = "project-service"
	}

	targetURL, ok := serviceRegistry[serviceName]
	if !ok {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": fmt.Sprintf("Service '%s' not available", serviceName)})
		return
	}

	// Strip the "/api" prefix from the path before forwarding
	originalPath := c.Request.URL.Path
	c.Request.URL.Path = strings.TrimPrefix(originalPath, "/api")

	// For gallery service, we need to handle the path mapping
	if serviceName == "gallery-service" {
		originalGalleryPath := c.Request.URL.Path

		// Handle health endpoint mapping
		if strings.HasPrefix(c.Request.URL.Path, "/gallery/health") {
			c.Request.URL.Path = "/health"
			fmt.Printf("Gallery service health path mapping: %s -> %s (service: %s, target: %s)\n",
				originalGalleryPath, c.Request.URL.Path, serviceName, targetURL.String())
		} else {
			// For other gallery routes, strip the "/gallery" prefix
			beforeStrip := c.Request.URL.Path
			c.Request.URL.Path = strings.TrimPrefix(c.Request.URL.Path, "/gallery")
			fmt.Printf("Gallery service path mapping: %s -> %s -> %s (service: %s, target: %s)\n",
				originalGalleryPath, beforeStrip, c.Request.URL.Path, serviceName, targetURL.String())
		}
	}

	// For storage service, we need to handle the path mapping
	if serviceName == "storage-service" {
		originalStoragePath := c.Request.URL.Path

		// Handle health endpoint mapping
		if strings.HasPrefix(c.Request.URL.Path, "/storage/health") {
			c.Request.URL.Path = "/health"
			fmt.Printf("Storage service health path mapping: %s -> %s (service: %s, target: %s)\n",
				originalStoragePath, c.Request.URL.Path, serviceName, targetURL.String())
		} else {
			// For other storage routes, strip the "/storage" prefix
			beforeStrip := c.Request.URL.Path
			c.Request.URL.Path = strings.TrimPrefix(c.Request.URL.Path, "/storage")
			fmt.Printf("Storage service path mapping: %s -> %s -> %s (service: %s, target: %s)\n",
				originalStoragePath, beforeStrip, c.Request.URL.Path, serviceName, targetURL.String())
		}
	}

	// Create a new reverse proxy and serve the request
	proxy := httputil.NewSingleHostReverseProxy(targetURL)

	// Restore the original path after the request is complete
	defer func() {
		c.Request.URL.Path = originalPath
	}()

	proxy.ServeHTTP(c.Writer, c.Request)
}
