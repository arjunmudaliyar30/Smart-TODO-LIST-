# Forge — AI-Powered Productivity & Accountability Platform

A full-stack, PWA-enabled productivity platform that combines task management, fitness tracking, goal setting, and an AI assistant into a single unified experience.

---

## Features

### Core Productivity
- **Task Management** — Create, edit, filter by status/date, smart auto-complete scheduler
- **Goal Tracking** — Categories, progress tracking, AI-powered goal analysis
- **Daily Notes** — Personal journal with sharing, comments, and collaboration
- **Focus Sessions** — Pomodoro-style timed sessions linked to tasks
- **Streak Engine** — Consecutive-day streaks for tasks, workouts, calories, and notes

### AI Assistant
- **Chat Interface** — Conversational AI backed by Groq LLM
- **Smart Scheduling** — AI detects task/event intent in chat and creates tasks automatically
- **End-of-Day Summary** — AI-generated daily recap based on actual completed tasks and goals
- **Goal Analysis** — AI insights on goal progress and next steps
- **Per-minute Rate Limiting** — Prevents API abuse

### Fitness & Health
- **Workout Logging** — Track sets, reps, volume with custom exercises
- **Workout Schedules** — Day-by-day weekly plans
- **Fitness Categories** — System defaults + user-defined custom categories
- **Fitness Progression** — Track progress over time per exercise
- **Calories Tracking** — Daily intake/burn logging with net calorie view
- **User Fitness Profile** — Goals, weight, height, activity level

### Analytics
- **Streak Dashboard** — Current and longest streaks across all modules
- **Weekly Reports** — Aggregated performance across tasks, fitness, and mood
- **Mood Analytics** — Mood trends over custom date ranges
- **Achievements** — Milestone badges unlocked by activity

### Notifications & Messaging
- **Web Push Notifications** — VAPID-based browser push for reminders
- **Email Notifications** — OTP, password reset, and reminders via Gmail SMTP
- **WhatsApp Integration** — Twilio-powered WhatsApp message delivery
- **In-App Messaging** — User-to-user direct messages

### Authentication & Security
- **JWT Auth** — Register, login, refresh tokens (30-day / 90-day expiry)
- **OTP Verification** — Email-based OTP for registration and password reset
- **Forgot / Reset Password** — Secure token-based flow
- **Accountability Mode** — Toggleable strict accountability profile per user

### File Management
- Upload, download, and inline preview (images, PDFs, text)
- Files linked to tasks; 10 MB per file limit

### PWA
- Installable on mobile and desktop
- Service Worker with offline support
- Cinematic splash screen → Landing page → Dashboard flow

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.3, Java 17, Maven |
| Database | MongoDB Atlas |
| AI / LLM | Groq API (configurable model) |
| Auth | JWT (jjwt), Spring Security |
| Email | Gmail SMTP via Spring Mail |
| Push | VAPID Web Push |
| Messaging | Twilio (WhatsApp) |
| Frontend | Vanilla JS, CSS3, PWA (Service Worker) |
| Containerization | Docker (2-stage Maven build) |
| Hosting | Fly.io |

---

## Project Structure

```
src/main/java/com/yourapp/
├── config/          # Security, CORS, OpenAI config
├── controller/      # REST API endpoints
├── dto/             # Request / Response objects
├── event/           # Application events (task completed, workout done)
├── filter/          # JWT request filter
├── listener/        # Event listeners for achievements & streaks
├── model/           # MongoDB document models
├── repository/      # Spring Data MongoDB repositories
├── scheduler/       # Cron jobs (reminders, auto-complete, daily reset)
├── security/        # JWT utilities, UserDetailsService
└── service/         # Business logic

src/main/resources/static/
├── index.html       # Landing / splash page
├── dashboard.html   # Main app dashboard
├── login.html
├── register.html
├── js/              # api.js, auth.js, dashboard.js
├── css/
└── assets/          # Icons, SVG
```

---

## API Endpoints

| Module | Base Path |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `/forgot-password`, `/reset-password`, `/verify-otp` |
| Tasks | `GET/POST /api/tasks`, `/by-date`, `/calories/net` |
| Goals | `GET/POST /api/goals`, `/{id}/analyze` |
| AI | `POST /api/ai/chat`, `GET /api/ai/chat/history`, `POST /api/ai/summary` |
| Analytics | `GET /api/analytics/streaks`, `/weekly`, `/mood`, `/achievements` |
| Focus | `POST /api/focus`, `POST /api/focus/{id}/complete` |
| Fitness | `/api/fitness/categories`, `/api/fitness/profile`, `/api/workouts`, `/api/workout-schedules` |
| Calories | `GET/POST /api/calories` |
| Files | `POST /api/files`, `GET /api/files/{id}/download`, `/preview` |
| Notes | `/api/notes` |
| Messages | `/api/messages` |
| Push | `POST /api/push/subscribe` |
| Users | `GET /api/users/me`, `PATCH /api/users/me/accountability` |

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB Atlas account (or local MongoDB)

### 1. Configure secrets

Create `src/main/resources/application-local.properties`:

```properties
spring.data.mongodb.uri=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/<db>
jwt.secret=<base64-secret>
openai.api.key=<groq-api-key>
openai.base-url=https://api.groq.com/openai/v1
openai.model=llama3-8b-8192
spring.mail.username=<gmail>
spring.mail.password=<app-password>
vapid.public.key=<vapid-public>
vapid.private.key=<vapid-private>
vapid.subject=mailto:<email>
```

### 2. Run

```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080`

---

## Docker

```bash
docker build -t forge-app .
docker run -p 8080:8080 \
  -e MONGODB_URI="..." \
  -e JWT_SECRET="..." \
  -e GROQ_API_KEY="..." \
  forge-app
```

---

## Deploying to Fly.io

```bash
# Login
flyctl auth login

# Set secrets
flyctl secrets set \
  MONGODB_URI="..." \
  JWT_SECRET="..." \
  GROQ_API_KEY="..." \
  MAIL_USERNAME="..." \
  MAIL_PASSWORD="..." \
  VAPID_PUBLIC_KEY="..." \
  VAPID_PRIVATE_KEY="..." \
  VAPID_SUBJECT="mailto:you@example.com"

# Deploy
flyctl deploy
```

Live at: `https://smart-todo-forge.fly.dev`

---

## Environment Variables Reference

| Variable | Description |
|---|---|
| `MONGODB_URI` | Full MongoDB Atlas connection string |
| `JWT_SECRET` | Base64-encoded JWT signing secret |
| `GROQ_API_KEY` | Groq LLM API key |
| `MAIL_USERNAME` | Gmail address for sending emails |
| `MAIL_PASSWORD` | Gmail app password |
| `VAPID_PUBLIC_KEY` | VAPID public key for web push |
| `VAPID_PRIVATE_KEY` | VAPID private key for web push |
| `VAPID_SUBJECT` | `mailto:` URI for VAPID identity |
| `ALLOWED_ORIGINS` | CORS allowed origins (production domain) |
| `PORT` / `APP_PORT` | Server port (default: 8080) |

---

## License

MIT
