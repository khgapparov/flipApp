# Stage 1: Build the Go binary
FROM golang:1.25-alpine AS builder

WORKDIR /app

# Copy the entire project structure to access the database module
COPY . .

# Change to user-service directory and build
WORKDIR /app/user-service

# Download dependencies
RUN go mod download

# Build the application
# -o /app/main creates the binary at the specified path
# CGO_ENABLED=0 is important for creating a static binary that can run in a minimal image
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/main .

# Stage 2: Create the final, minimal image
FROM alpine:latest

WORKDIR /root/

# Copy the built binary from the builder stage
COPY --from=builder /app/main .

# Expose the port the service runs on
EXPOSE 8081

# Command to run the application
CMD ["./main"]
