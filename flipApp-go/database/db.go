package database

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"time"

	_ "github.com/lib/pq"
)

type Config struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
	SSLMode  string
}

func NewConfig() *Config {
	return &Config{
		Host:     getEnv("DB_HOST", "postgres"),
		Port:     getEnv("DB_PORT", "5432"),
		User:     getEnv("DB_USER", "user"),
		Password: getEnv("DB_PASSWORD", "password"),
		DBName:   getEnv("DB_NAME", "flipapp"),
		SSLMode:  getEnv("DB_SSLMODE", "disable"),
	}
}

func (c *Config) ConnectionString() string {
	return fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		c.Host, c.Port, c.User, c.Password, c.DBName, c.SSLMode)
}

func Connect() (*sql.DB, error) {
	config := NewConfig()
	
	db, err := sql.Open("postgres", config.ConnectionString())
	if err != nil {
		return nil, fmt.Errorf("failed to open database connection: %v", err)
	}

	// Set connection pool settings
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(25)
	db.SetConnMaxLifetime(5 * time.Minute)

	// Test the connection
	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %v", err)
	}

	log.Println("Successfully connected to PostgreSQL database")
	return db, nil
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
