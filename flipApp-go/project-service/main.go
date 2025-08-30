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
	SERVICE_NAME = "project-service"
	SERVICE_PORT = 8083
)

// Based on the Smithy definition in project.smithy

// ProjectStatus defines the lifecycle of a flip.
// @enum
type ProjectStatus string

const (
	LEAD           ProjectStatus = "LEAD"
	OFFER_SENT     ProjectStatus = "OFFER_SENT"
	OFFER_ACCEPTED ProjectStatus = "OFFER_ACCEPTED"
	ACTIVE         ProjectStatus = "ACTIVE"
	ON_MARKET      ProjectStatus = "ON_MARKET"
	SOLD           ProjectStatus = "SOLD"
	ARCHIVED       ProjectStatus = "ARCHIVED"
)

// MemberRole defines the role of a user in a project.
// @enum
type MemberRole string

const (
	OWNER           MemberRole = "OWNER"
	CONSTRUCTOR     MemberRole = "CONSTRUCTOR"
	AGENT           MemberRole = "AGENT"
	PROJECT_MANAGER MemberRole = "PROJECT_MANAGER"
)

// Project corresponds to the Project structure in project.smithy
type Project struct {
	ProjectID   string        `json:"projectId"`
	PropertyID  string        `json:"propertyId"`
	Status      ProjectStatus `json:"status"`
	ProjectName *string       `json:"projectName,omitempty"`
	Budget      *float64      `json:"budget,omitempty"`
	StartDate   *string       `json:"startDate,omitempty"`
	EndDate     *string       `json:"endDate,omitempty"`
	CreatedAt   string        `json:"createdAt"`
	UpdatedAt   string        `json:"updatedAt"`
}

// ProjectMember corresponds to the ProjectMember structure in project.smithy
type ProjectMember struct {
	ProjectID string     `json:"projectId"`
	UserID    string     `json:"userId"`
	Role      MemberRole `json:"role"`
}

func main() {
	registerServiceWithConsul()

	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// API routes based on project.smithy
	api := router.Group("/projects")
	{
		api.POST("", CreateProject)
		api.GET("/:projectId", GetProject)
		api.PUT("/:projectId/status", UpdateProjectStatus)
		api.GET("", ListProjects)
		api.POST("/:projectId/members", AddProjectMember)
		api.GET("/:projectId/members", ListProjectMembers)
	}

	fmt.Printf("Project Service started on port %d\n", SERVICE_PORT)
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
var projects = make(map[string]Project)
var projectMembers = make(map[string][]ProjectMember)

func CreateProject(c *gin.Context) {
	var input struct {
		PropertyID  string   `json:"propertyId" binding:"required"`
		ProjectName *string  `json:"projectName"`
		Budget      *float64 `json:"budget"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	projectID := fmt.Sprintf("project_%d", time.Now().UnixNano())
	project := Project{
		ProjectID:   projectID,
		PropertyID:  input.PropertyID,
		Status:      LEAD,
		ProjectName: input.ProjectName,
		Budget:      input.Budget,
		CreatedAt:   time.Now().Format(time.RFC3339),
		UpdatedAt:   time.Now().Format(time.RFC3339),
	}

	projects[projectID] = project
	c.JSON(http.StatusCreated, gin.H{"project": project})
}

func GetProject(c *gin.Context) {
	projectId := c.Param("projectId")
	project, exists := projects[projectId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Project not found"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"project": project})
}

func UpdateProjectStatus(c *gin.Context) {
	projectId := c.Param("projectId")
	project, exists := projects[projectId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Project not found"})
		return
	}

	var input struct {
		Status ProjectStatus `json:"status" binding:"required"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	project.Status = input.Status
	project.UpdatedAt = time.Now().Format(time.RFC3339)
	projects[projectId] = project

	c.JSON(http.StatusOK, gin.H{"project": project})
}

func ListProjects(c *gin.Context) {
	projectList := make([]Project, 0, len(projects))
	for _, project := range projects {
		projectList = append(projectList, project)
	}
	c.JSON(http.StatusOK, gin.H{"projects": projectList})
}

func AddProjectMember(c *gin.Context) {
	projectId := c.Param("projectId")
	_, exists := projects[projectId]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Project not found"})
		return
	}

	var input struct {
		UserID string     `json:"userId" binding:"required"`
		Role   MemberRole `json:"role" binding:"required"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	member := ProjectMember{
		ProjectID: projectId,
		UserID:    input.UserID,
		Role:      input.Role,
	}

	projectMembers[projectId] = append(projectMembers[projectId], member)
	c.JSON(http.StatusCreated, gin.H{"member": member})
}

func ListProjectMembers(c *gin.Context) {
	projectId := c.Param("projectId")
	members, exists := projectMembers[projectId]
	if !exists {
		c.JSON(http.StatusOK, gin.H{"members": []ProjectMember{}})
		return
	}
	c.JSON(http.StatusOK, gin.H{"members": members})
}
