# Local Development Environment Variables
# Run this script to start the application locally

# Set environment variables
$env:JWT_SECRET = "your-jwt-secret-here"
$env:JWT_EXPIRATION_MS = "2592000000"
$env:JWT_REFRESH_EXPIRATION_MS = "7776000000"

# MongoDB - Atlas connection
$env:MONGODB_URI = "mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<dbname>?retryWrites=true&w=majority"

# Groq API (optional for local testing)
$env:GROQ_API_KEY = "your-groq-api-key-here"
$env:GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
$env:GROQ_MODEL = "llama-3.3-70b-versatile"

# Web Push VAPID (auto-generated if blank)
$env:VAPID_PUBLIC_KEY = "your-vapid-public-key-here"
$env:VAPID_PRIVATE_KEY = "your-vapid-private-key-here"
$env:VAPID_SUBJECT = "mailto:your-email@example.com"

# Mail (optional for local testing)
$env:MAIL_USERNAME = "noreply@aiexecution.app"
$env:MAIL_PASSWORD = "dummy-password"

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
