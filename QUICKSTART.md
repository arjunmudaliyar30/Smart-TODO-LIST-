# ============================================================
# Quick Start Guide - Smart TODO List Application
# ============================================================

## Prerequisites
Your application requires MongoDB. Choose one of these options:

### Option 1: MongoDB Atlas (Recommended - Free Cloud Database)
1. Go to https://www.mongodb.com/cloud/atlas/register
2. Create a free account
3. Create a free cluster (M0 Sandbox)
4. Click "Connect" → "Connect your application"
5. Copy the connection string (looks like: mongodb+srv://username:password@cluster.mongodb.net/dbname)
6. Update MONGODB_URI in run-local.ps1 with your connection string

### Option 2: Install MongoDB Locally
1. Download from: https://www.mongodb.com/try/download/community
2. Install and start MongoDB service
3. Use connection string: mongodb://localhost:27017/ai-execution

### Option 3: Use Docker (if you have Docker Desktop)
```powershell
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

## Running the Application

1. **Update MongoDB connection in run-local.ps1**
   Edit line with $env:MONGODB_URI with your actual connection string

2. **Run the application**
   ```powershell
   .\run-local.ps1
   ```

3. **Test the health endpoint**
   Open browser: http://localhost:8080/health
   Should return: OK

## Environment Variables Configured
- JWT_SECRET: Local development secret
- MONGODB_URI: MongoDB connection (YOU NEED TO UPDATE THIS!)
- GROQ_API_KEY: AI service key (optional for basic features)
- PORT: 8080 (default)

## Health Check Endpoint
- URL: http://localhost:8080/health
- Method: GET
- Response: Plain text "OK"
- No dependencies (works even if database has issues)

## Notes
- The health endpoint is independent and will work once app starts
- Other features require MongoDB connection
- Update MONGODB_URI before running
