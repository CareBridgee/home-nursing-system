# Home Nursing Care Platform 🏡

A comprehensive platform connecting patients with home nursing care professionals.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-7B5CE0)
![Tests](https://img.shields.io/badge/Tests-993%20passing-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-4169E1)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

---

## Overview

The platform connects patients with home nursing care professionals: patients create service requests, nearby approved nurses receive real-time offers, and visits are tracked from request to completion. It includes an AI assistant ("Nursy") that helps patients prepare booking drafts through natural conversation, a credit-based wallet, clinical report generation, and full admin oversight.

---

## Key Features

### Authentication & Authorization 🔐
- **OTP-based login** (phone number + SMS via Twilio) for patients and nurses
- **Google Sign-In** integration for patients and nurses with account linking
- **JWT access + refresh tokens** with Redis-backed token storage and logout
- **Role-based access**: PATIENT, NURSE, ADMIN

### Patient & Profile Management 👤
- **Multi-profile support**: one user can manage multiple patient profiles
- **Medical records**: conditions, allergies, medications, medical history, surgeries, hospitalizations
- **Emergency contacts** with phone numbers
- **Profile images** with Cloudinary upload
- **Mobility status**, blood type, height, weight, gender, date of birth, notes

### Nurse Verification & Management 🧑‍⚕️
- **Nurse registration** with document upload (multipart/form-data)
- **Admin approval workflow**: UNDER_REVIEW → APPROVED / REJECTED with rejection reasons
- **Service offerings**: nurses declare which service types they provide
- **Availability toggle** + real-time location via WebSocket

### Service Catalog 🗂️
- **Service types** with name, description, category, base price, estimated duration
- **Admin CRUD** for service types (multipart image upload)

### Booking & Service Requests (Core Flow) 📅
1. **Patient creates request** → GPS coordinates + preferred date/time + service type + description
2. **System finds nearby nurses** (Haversine distance, active + approved + offering the service)
3. **Real-time push** via WebSocket to nearby nurses with estimated price & duration
4. **Nurses make offers** with proposed price, date, time, optional message
5. **Patient accepts / counters / rejects** offers
6. **Visit code generation** for visit verification
7. **Completion flow** with visit code verification
8. **Cancellation** at any stage before completion
9. **History**: patient confirmed history, nurse history, current visit

### AI Assistant "Nursy" 🤖
- **Conversational booking draft preparation** — chat NEVER creates a reservation
- **4 tool groups (6 callable functions)**:
  - `listServiceTypes` — live catalog with prices
  - `searchFaqs` — RAG-powered FAQ retrieval (pgvector, topK=4, threshold=0.35)
  - `updateReservationDraft` — record service choice + care description (user's own words)
  - `setUrgency` / `clearUrgency` / `resetDraft` — medical safety & draft control
- **Medical safety**: emergency keywords (chest pain, bleeding, breathing difficulty, unconsciousness) → immediate emergency advice + urgency banner (HOSPITALIZATION)
- **Booking honesty**: never claims a nurse is dispatched/confirmed; directs to app for confirmation
- **Scope guard**: refuses off-topic, roleplay, style hijacking; never confirms/denies booking-state claims
- **Per-turn token logging** (`chat-usage` log line with prompt/completion/total)
- **Streaming endpoint** (`/api/v1/chat/stream`, SSE)
- **Rate limit**: 20 requests / 5 min per user (Redis)
- **20-message conversation memory** (in-memory window)

### FAQ RAG Pipeline 📚
- **Ingestion** (`FaqIngestionService`): `faqs.pdf` → section/question parsing → Q&A chunks → `gemini-embedding-001` (768-dim) → pgvector HNSW (cosine)
- **Idempotent**: SHA-256 marker in Redis (30-day TTL), no re-embed on restart
- **Retrieval** (`FaqSearchTool`): topK=4, similarity ≥0.35, returns `[Section] Q/A` blocks

### Nurse Offers 💼
- Create, list, get, accept, counter-offer, reject, withdraw, update, delete
- Real-time WebSocket events for offer lifecycle

### Real-time WebSocket ⚡
- **Nurse presence**: heartbeat, availability toggle, location updates
- **Offer lifecycle**: create, update, counter, accept, withdraw, reject
- **Reservation actions**: cancel, list offers
- **In-reservation chat**: send messages
- **Authorization**: only APPROVED nurses can use presence/offer endpoints
- See `WEBSOCKET.md` for the full protocol documentation

### Credit & Payment System 💰
- **User wallet**: `BigDecimal credit` (precision 12, scale 2, default 0.00)
- **Operations**: ADD / DEDUCT with insufficient-credit protection
- **Balance query**
- **Service request payment type**: CASH or CREDIT (enum on ServiceRequest)

### Reviews & Ratings ⭐
- Patients review nurses (1-5 stars + comment)
- Nurse rating aggregation (average + count, updated on create/update/delete)
- Paginated listing with sortable fields

### Admin Panel 🛠️
- List nurses by verification status
- Approve / reject with reasons

### Clinical Reports 📋
- AI-generated structured handover report for nurses
- Sections: Patient Overview, Medical History, Allergies, Medications, Care Considerations, Risk Flags
- "Never invent facts" — empty sections show "Not recorded"
- Access control: profile owner or assigned nurse only

### Notifications 🔔
- CRUD + mark-read + delete
- Filter by time
- Types: OFFER, CHAT, SYSTEM, etc.

---

## Architecture 🏗️

```
┌─ Client ─────────────────────────────────────────────┐
│  Android app (REST + WebSocket/STOMP + SSE)          │
└──────────────────────┬───────────────────────────────┘
                       ▼
┌─ Spring Boot 4.x ────────────────────────────────────┐
│  REST API │ WebSocket Controller │ Spring AI (Nursy) │
│  Service layer: auth, booking, offers, credit,       │
│  reviews, reports, notifications                     │
└───────┬──────────────┬──────────────┬────────────────┘
        ▼              ▼              ▼
  PostgreSQL 16     Redis 7        Cloudinary
  (data + pgvector  (JWT refresh,  (profile images,
   HNSW, cosine,     rate limits,   nurse documents)
   768-dim)          FAQ markers)
        │
        ▼
  Google Gemini (Spring AI 2.0)
  • gemini-3.5-flash-lite (chat)
  • gemini-embedding-001 (768-dim)
```

### Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI 2.0 + Google Gemini (`gemini-3.5-flash-lite`, `gemini-embedding-001`) |
| Database | PostgreSQL 16 + pgvector (HNSW index, cosine distance, 768-dim) |
| Cache | Redis (JWT refresh tokens, rate limiting, FAQ ingestion markers) |
| Messaging | WebSocket / STOMP (SimpMessagingTemplate) |
| File Storage | Cloudinary |
| SMS | Twilio |
| Build | Maven, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers (993 tests) |

---

## Getting Started 🚀

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Google AI Studio API key (for Gemini)
- Twilio Account SID + Auth Token (for SMS)
- Cloudinary credentials (for images)

### Environment Variables
Copy `.env.example` to `.env` and fill in the values marked **REQUIRED**:

```bash
# Required — app will not boot without these
JWT_SECRET=generate_a_strong_random_string
ADMIN_API_KEY=your_admin_api_key
DB_NAME=carenest
DB_USER=carenest
DB_PASSWORD=carenest
REDIS_PASSWORD=carenest

# Required — Google Gemini (chat + embeddings)
GEMINI_EMBEDDING_API_KEY=your_gemini_api_key
GEMINI_PROJECT_ID=carebridge
GEMINI_LOCATION=us-central1

# Required — feature functionality
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_FROM_NUMBER=+1xxxxxxxxxx
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id

# Optional — sensible defaults already in .env.example
# GEMINI_CHAT_MODEL=gemini-3.5-flash-lite
# OTP_TTL_SECONDS=300, RATE_LIMIT_* , PENDING_TOKEN_TTL_SECONDS=600, etc.
```

**Note**: leave Twilio credentials empty to log OTP codes to the console instead of sending SMS.

### Run with Docker Compose
```bash
# Full stack (app + PostgreSQL + pgvector + Redis)
docker compose --profile full up -d --build app

# View logs
docker logs -f home-nursing-app
```

---

## API Documentation 📖

- **REST API**: Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) when running
- **WebSocket Protocol**: See `WEBSOCKET.md`

---

## Configuration Highlights ⚙️

Key settings in `application.properties`:

```properties
# AI Models
spring.ai.model.chat=google-genai
spring.ai.google.genai.chat.options.model=${GEMINI_CHAT_MODEL:gemini-3.5-flash-lite}
spring.ai.google.genai.embedding.text.model=gemini-embedding-001
spring.ai.google.genai.embedding.text.dimensions=768

# pgvector
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768

# FAQ RAG
faq.search.top-k=4
faq.search.similarity-threshold=0.35

# Retry (429 excluded to avoid burning free-tier quota)
spring.ai.retry.max-attempts=3
spring.ai.retry.exclude-on-http-codes=429

# Chat rate limit
chat.rate-limit.window-seconds=300
chat.rate-limit.max-requests=20
```

---

## Testing 🧪

```bash
# Run all tests (993 tests)
mvn test
```

993 tests covering unit, integration, and AI tool invocation — including `chatResponse()`-based AI flow tests and FAQ RAG retrieval tests.

---

## Project Structure 📁

```
src/main/java/iti/jets/java/homenursing/
├── ai/                    # Spring AI tools, RAG, config
├── annotation/            # @SortableFields for paginated endpoints
├── config/                # AiConfig, SecurityConfig, JwtAuthenticationFilter, WebSocketConfig
├── controller/            # REST + WebSocket endpoints
│   └── admin/             # Admin-only endpoints
├── dto/                   # Request/Response records
├── entity/                # JPA entities + enums
├── exception/             # Custom exceptions + GlobalExceptionHandler
├── mapper/                # MapStruct mappers
├── repository/            # Spring Data JPA repositories
├── security/              # SecurityUtils
├── service/               # Business logic interfaces
├── service/impl/          # Service implementations
└── util/                  # PriceEstimator, ProfileImageUtil, etc.
```