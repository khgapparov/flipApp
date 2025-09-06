package main

import (
	"crypto/rand"
	"database/sql"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	consul "github.com/hashicorp/consul/api"
	"flipapp/database"
)

const (
	SERVICE_NAME         = "auth-service"
	SERVICE_PORT         = 8080
	ACCESS_TOKEN_EXPIRY  = 15 * time.Minute
	REFRESH_TOKEN_EXPIRY = 7 * 24 * time.Hour
)

var (
	jwtSecret []byte
	db        *sql.DB
)

// Based on the Smithy definition in auth.smithy

func main() {
	// Generate or load JWT secret
	initJWTSecret()

	// Initialize database connection
	var err error
	db, err = database.Connect()
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Try to register with Consul, but continue if it fails (for development)
	if err := registerServiceWithConsul(); err != nil {
		fmt.Printf("Warning: Consul registration failed: %v\n", err)
		fmt.Println("Service will continue running without Consul registration")
	}

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on auth.smithy
	api := router.Group("/auth")
	{
		api.POST("/register", Register)
		api.POST("/login", Login)
		api.POST("/refresh", RefreshToken)
		api.POST("/logout", Logout)
		api.POST("/validate", ValidateToken)
	}

	fmt.Printf("Auth Service started on port %d\n", SERVICE_PORT)
	router.Run(fmt.Sprintf(":%d", SERVICE_PORT))
}

func registerServiceWithConsul() error {
	// Get Consul address from environment variable or use default
	consulAddr := os.Getenv("CONSUL_ADDRESS")
	if consulAddr == "" {
		consulAddr = "consul:8500" // Use service name in Docker network
	}

	config := consul.DefaultConfig()
	config.Address = consulAddr
	consulClient, err := consul.NewClient(config)
	if err != nil {
		return fmt.Errorf("failed to create consul client: %v", err)
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
		return fmt.Errorf("failed to register service with consul: %v", err)
	}

	fmt.Printf("Successfully registered service '%s' with Consul at %s\n", SERVICE_NAME, consulAddr)
	return nil
}

// Custom claims structure
type CustomClaims struct {
	UserID string `json:"userId"`
	Email  string `json:"email"`
	jwt.RegisteredClaims
}

func initJWTSecret() {
	// In production, load from environment variable or secret manager
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		// Generate a random secret for development
		secretBytes := make([]byte, 32)
		if _, err := rand.Read(secretBytes); err != nil {
			log.Fatalf("Failed to generate JWT secret: %v", err)
		}
		secret = base64.StdEncoding.EncodeToString(secretBytes)
		fmt.Printf("Generated JWT secret: %s\n", secret)
	}
	jwtSecret = []byte(secret)
}

func generateAccessToken(userID, email string) (string, error) {
	claims := CustomClaims{
		UserID: userID,
		Email:  email,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(ACCESS_TOKEN_EXPIRY)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			NotBefore: jwt.NewNumericDate(time.Now()),
			Issuer:    "flipapp-auth-service",
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(jwtSecret)
}

func generateRefreshToken() (string, error) {
	tokenBytes := make([]byte, 32)
	if _, err := rand.Read(tokenBytes); err != nil {
		return "", err
	}
	return base64.URLEncoding.EncodeToString(tokenBytes), nil
}

func validateToken(tokenString string) (*CustomClaims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &CustomClaims{}, func(token *jwt.Token) (interface{}, error) {
		return jwtSecret, nil
	})

	if err != nil {
		return nil, err
	}

	if claims, ok := token.Claims.(*CustomClaims); ok && token.Valid {
		return claims, nil
	}

	return nil, fmt.Errorf("invalid token")
}

// Database helper functions
func storeRefreshToken(token, userID string) error {
	expiresAt := time.Now().Add(REFRESH_TOKEN_EXPIRY)
	_, err := db.Exec(`
		INSERT INTO refresh_tokens (token, user_id, expires_at) 
		VALUES ($1, $2, $3)
		ON CONFLICT (token) DO UPDATE SET expires_at = $3
	`, token, userID, expiresAt)
	return err
}

func getRefreshTokenUserID(token string) (string, error) {
	var userID string
	err := db.QueryRow(`
		SELECT user_id FROM refresh_tokens 
		WHERE token = $1 AND expires_at > NOW()
	`, token).Scan(&userID)
	if err != nil {
		if err == sql.ErrNoRows {
			return "", fmt.Errorf("invalid or expired refresh token")
		}
		return "", err
	}
	return userID, nil
}

func deleteRefreshToken(token string) error {
	_, err := db.Exec("DELETE FROM refresh_tokens WHERE token = $1", token)
	return err
}

func createUser(email, passwordHash string) (string, error) {
	userID := fmt.Sprintf("user_%d", time.Now().UnixNano())
	_, err := db.Exec(`
		INSERT INTO users (user_id, email, password_hash, profile_type)
		VALUES ($1, $2, $3, 'PERSON')
	`, userID, email, passwordHash)
	if err != nil {
		return "", err
	}
	return userID, nil
}

func getUserByEmail(email string) (string, string, error) {
	var userID, passwordHash string
	err := db.QueryRow(`
		SELECT user_id, password_hash FROM users WHERE email = $1
	`, email).Scan(&userID, &passwordHash)
	if err != nil {
		if err == sql.ErrNoRows {
			return "", "", fmt.Errorf("user not found")
		}
		return "", "", err
	}
	return userID, passwordHash, nil
}

func getUserEmail(userID string) (string, error) {
	var email string
	err := db.QueryRow("SELECT email FROM users WHERE user_id = $1", userID).Scan(&email)
	if err != nil {
		return "", err
	}
	return email, nil
}

// Simple password hashing (in production, use bcrypt with proper salt)
func hashPassword(password string) string {
	// This is a simple hash for demonstration - use bcrypt in production
	return base64.StdEncoding.EncodeToString([]byte(password))
}

// Request structures
type RegisterRequest struct {
	Username string `json:"username" binding:"required"`
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=6"`
}

type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refreshToken" binding:"required"`
}

type ValidateRequest struct {
	Token string `json:"token" binding:"required"`
}

func Register(c *gin.Context) {
	var req RegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Check if user already exists
	_, _, err := getUserByEmail(req.Email)
	if err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "User already exists"})
		return
	}

	// Create user in database
	passwordHash := hashPassword(req.Password)
	userID, err := createUser(req.Email, passwordHash)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create user"})
		return
	}

	accessToken, err := generateAccessToken(userID, req.Email)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate access token"})
		return
	}

	refreshToken, err := generateRefreshToken()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate refresh token"})
		return
	}

	// Store refresh token in database
	if err := storeRefreshToken(refreshToken, userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to store refresh token"})
		return
	}

	c.JSON(http.StatusCreated, gin.H{
		"userId":       userID,
		"accessToken":  accessToken,
		"refreshToken": refreshToken,
	})
}

func Login(c *gin.Context) {
	var req LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Get user from database
	userID, storedHash, err := getUserByEmail(req.Email)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid credentials"})
		return
	}

	// Verify password (in production, use proper password comparison with bcrypt)
	inputHash := hashPassword(req.Password)
	if inputHash != storedHash {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid credentials"})
		return
	}

	accessToken, err := generateAccessToken(userID, req.Email)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate access token"})
		return
	}

	refreshToken, err := generateRefreshToken()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate refresh token"})
		return
	}

	// Store refresh token in database
	if err := storeRefreshToken(refreshToken, userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to store refresh token"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"accessToken":  accessToken,
		"refreshToken": refreshToken,
	})
}

func RefreshToken(c *gin.Context) {
	var req RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Validate refresh token from database
	userID, err := getRefreshTokenUserID(req.RefreshToken)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid refresh token"})
		return
	}

	// Get user email from database
	email, err := getUserEmail(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get user information"})
		return
	}

	accessToken, err := generateAccessToken(userID, email)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate access token"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"accessToken": accessToken,
	})
}

func Logout(c *gin.Context) {
	var req RefreshRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Remove refresh token from database
	if err := deleteRefreshToken(req.RefreshToken); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to logout"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Logged out successfully",
	})
}

func ValidateToken(c *gin.Context) {
	var req ValidateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	claims, err := validateToken(req.Token)
	if err != nil {
		c.JSON(http.StatusOK, gin.H{
			"isValid": false,
			"userId":  "",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"isValid": true,
		"userId":  claims.UserID,
	})
}
