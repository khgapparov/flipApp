FROM golang:1.25-alpine AS builder

WORKDIR /app

# Copy go mod files
COPY storage-service/go.mod storage-service/go.sum ./
RUN go mod download

# Copy source code
COPY storage-service/ .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o main .

FROM alpine:latest

RUN apk --no-cache add ca-certificates

WORKDIR /root/

# Copy the binary from builder
COPY --from=builder /app/main .

# Create storage directory
RUN mkdir -p /storage

# Expose port
EXPOSE 8085

# Run the binary
CMD ["./main"]
