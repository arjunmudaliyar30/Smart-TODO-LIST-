# Local Development Environment Variables
# Run this script to start the application locally

# Set environment variables
$env:JWT_SECRET = "your-local-secret-key-must-be-at-least-256-bits-long-abcdefghijklmnopqrstuvwxyz0123456789"
$env:JWT_EXPIRATION_MS = "2592000000"
$env:JWT_REFRESH_EXPIRATION_MS = "7776000000"

# MongoDB - Update with your local MongoDB connection
$env:MONGODB_URI = "mongodb://localhost:27017/ai-execution"

# Groq API (optional for local testing)
$env:GROQ_API_KEY = "gsk-dummy-key-for-local-testing"
$env:GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
$env:GROQ_MODEL = "llama-3.3-70b-versatile"

# Twilio (optional for local testing)
$env:TWILIO_ACCOUNT_SID = ""
$env:TWILIO_AUTH_TOKEN = ""
$env:TWILIO_WHATSAPP_FROM = "whatsapp:+14155238886"

# CORS
$env:ALLOWED_ORIGINS = "http://localhost:8080,http://localhost:3000"

# File Upload
$env:FILE_UPLOAD_DIR = "./uploads"

# AI Rate Limit
$env:AI_RATE_LIMIT_PER_MINUTE = "10"

Write-Host "Starting Spring Boot application..." -ForegroundColor Green
Write-Host "Health endpoint will be available at: http://localhost:8080/health" -ForegroundColor Cyan

# Run the application
mvn spring-boot:run
