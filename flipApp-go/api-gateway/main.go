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

	// Discover services at startup
	discoverServices(consulClient)

	// Set up Gin router
	router := gin.Default()

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
		protected.GET("/users/*userPath", proxyHandler)
		protected.POST("/users/*userPath", proxyHandler)
		protected.GET("/properties/*propertyPath", proxyHandler)
		protected.POST("/properties/*propertyPath", proxyHandler)
		protected.GET("/projects/*projectPath", proxyHandler)
		protected.POST("/projects/*projectPath", proxyHandler)
		protected.GET("/gallery/*galleryPath", proxyHandler)
		protected.POST("/gallery/*galleryPath", proxyHandler)
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

	// Create a new reverse proxy and serve the request
	proxy := httputil.NewSingleHostReverseProxy(targetURL)

	// Restore the original path after the request is complete
	defer func() {
		c.Request.URL.Path = originalPath
	}()

	proxy.ServeHTTP(c.Writer, c.Request)
}
