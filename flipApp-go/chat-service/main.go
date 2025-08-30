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
	SERVICE_NAME = "chat-service"
	SERVICE_PORT = 8085
)

// Based on the Smithy definition in chat.smithy

// Conversation corresponds to the Conversation structure in chat.smithy
type Conversation struct {
	ConversationID string   `json:"conversationId"`
	ProjectID      string   `json:"projectId"`
	Participants   []string `json:"participants"`
	CreatedAt      string   `json:"createdAt"`
}

// Message corresponds to the Message structure in chat.smithy
type Message struct {
	MessageID      string `json:"messageId"`
	ConversationID string `json:"conversationId"`
	SenderID       string `json:"senderId"`
	Content        string `json:"content"`
	Timestamp      string `json:"timestamp"`
}

func main() {
	registerServiceWithConsul()

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on chat.smithy
	api := router.Group("/conversations")
	{
		api.POST("", StartConversation)
		api.POST("/:conversationId/messages", PostMessage)
		api.GET("/:conversationId/messages", GetConversationMessages)
	}

	fmt.Printf("Chat Service started on port %d\n", SERVICE_PORT)
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

func StartConversation(c *gin.Context) {
	c.JSON(http.StatusCreated, gin.H{"message": "Placeholder for StartConversation"})
}

func PostMessage(c *gin.Context) {
	conversationId := c.Param("conversationId")
	c.JSON(http.StatusCreated, gin.H{"message": "Placeholder for PostMessage", "conversationId": conversationId})
}

func GetConversationMessages(c *gin.Context) {
	conversationId := c.Param("conversationId")
	c.JSON(http.StatusOK, gin.H{"message": "Placeholder for GetConversationMessages", "conversationId": conversationId, "messages": []Message{}})
}
