package database

// DatabaseConfig represents database connection configuration
type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
	SSLMode  string
}

// NewConfig creates a new database configuration with default values
func NewConfig() *DatabaseConfig {
	return &DatabaseConfig{
		Host:     "postgres",
		Port:     "5432",
		User:     "user",
		Password: "password",
		DBName:   "flipapp",
		SSLMode:  "disable",
	}
}

// ConnectionString returns the PostgreSQL connection string
func (c *DatabaseConfig) ConnectionString() string {
	return c.Host + ":" + c.Port + "?user=" + c.User + "&password=" + c.Password + "&dbname=" + c.DBName + "&sslmode=" + c.SSLMode
}
