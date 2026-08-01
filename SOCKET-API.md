# WebSocket / STOMP API

## Overview

The server uses **STOMP over WebSocket** for real-time communication.

| Property | Value |
|---|---|
| Endpoint | `ws://<host>:8080/ws` |
| Broker prefixes | `/topic` (pub/sub), `/queue` (point-to-point) |
| Application prefix | `/app` |
| User prefix | `/user` |
| Auth | JWT Bearer token in CONNECT frame header |
| Protocol versions | STOMP 1.0, 1.1, 1.2 |

---

## Connection & Authentication

1. Open raw WebSocket to `ws://<host>:8080/ws`.
2. Send STOMP `CONNECT` frame:

```
CONNECT
Authorization: Bearer <accessToken>
accept-version:1.2,1.1,1.0
heart-beat:10000,10000
```

3. Server validates the JWT — extracts `userId` and `role`.
4. On success → `CONNECTED` response.
5. If `role` is `NURSE`, the nurse is auto-added to the online set in Redis.
6. On `DISCONNECT` (or close) → nurse removed from online + available sets.

### Reconnection

Detect close/error events, reconnect with a fresh access token, and re-subscribe to all active topics.

---

## Topics to Subscribe To

| Topic Pattern | When | Purpose | Access |
|---|---|---|---|
| `/user/queue/notifications` | Always while connected | Receive real-time notifications for the current user | User-scoped |
| `/user/queue/nearby-request` | Always while connected (nurse only) | Receive new matching service requests pushed in real-time | Nurse-scoped |
| `/topic/chat/{reservationId}` | When entering a reservation chat view | Receive chat messages | Must be participant |
| `/topic/reservation/{reservationId}` | When viewing a reservation | Receive all reservation & offer events | Must be participant |

---

## Reservation Events (on `/topic/reservation/{id}`)

All reservation-scoped events are pushed as a single JSON wrapper:

```json
{
  "type": "OFFER_CREATED",
  "reservationId": "uuid",
  "data": { ... }
}
```

| `type` Value | `data` Shape | When |
|---|---|---|
| `OFFER_CREATED` | `NurseOfferResponse` | Nurse creates an offer |
| `OFFER_UPDATED` | `NurseOfferResponse` | Nurse updates their offer terms |
| `OFFER_COUNTERED` | `NurseOfferResponse` | Patient proposes counter-terms |
| `OFFER_ACCEPTED` | `NurseOfferResponse` | Either party accepts → reservation assigned |
| `OFFER_WITHDRAWN` | `{ "offerId": "uuid" }` | Nurse withdraws their offer |
| `OFFER_REJECTED` | `{ "offerId": "uuid" }` | Patient rejects an offer |
| `REQUEST_CANCELLED` | `{}` | Patient cancels the entire request |
| `OFFERS_LIST` | `[NurseOfferResponse, ...]` | Response to `/app/reservation/offers/list` |

---

## Commands to Send (SEND to `/app/*`)

### Nurse Presence

| Destination | Payload | When |
|---|---|---|
| `/app/heartbeat` | (empty) | Every ~30 seconds |
| `/app/reservation/availability` | `{ "available": true, "lat": 30.0, "lng": 31.0 }` | Toggle availability |
| `/app/reservation/availability` | `{ "available": false }` | Go unavailable |
| `/app/reservation/location` | `{ "lat": 30.0, "lng": 31.0 }` | Update current position |

### Offer Management

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/reservation/offer/create` | `{ "serviceRequestId": "uuid", "proposedPrice": 100.00, "proposedDate": "2026-08-15", "proposedTime": "10:00", "message": "optional note" }` | Nurse | Creates a pending offer |
| `/app/reservation/offer/update` | `{ "offerId": "uuid", "proposedPrice": 90.00, "proposedDate": "...", "proposedTime": "...", "message": "..." }` | Nurse | Updates own pending offer's terms |
| `/app/reservation/offer/counter` | `{ "offerId": "uuid", "proposedPrice": 80.00, "proposedDate": "...", "proposedTime": "...", "message": "..." }` | Patient | Proposes counter-terms on a pending offer |
| `/app/reservation/offer/accept` | `{ "offerId": "uuid" }` | Either | Accepts current terms → finalizes reservation |
| `/app/reservation/offer/withdraw` | `{ "offerId": "uuid" }` | Nurse | Withdraws own pending offer |
| `/app/reservation/offer/reject` | `{ "offerId": "uuid" }` | Patient | Rejects an offer |

### Request Management

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/reservation/cancel` | `{ "serviceRequestId": "uuid" }` | Patient | Cancels request, rejects all pending offers |
| `/app/reservation/offers/list` | `{ "serviceRequestId": "uuid" }` | Participant | Triggers server to push `OFFERS_LIST` on the reservation topic |

### Chat

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/chat/{reservationId}/send` | `{ "content": "Hello" }` | Participant | Persists + broadcasts to topic + notifies the other participant (type `MESSAGE`) |

---

## Response Shapes

### ChatMessageResponse — on `/topic/chat/{id}`

```json
{
  "id": "uuid",
  "serviceRequestId": "uuid",
  "senderUserId": "uuid",
  "senderName": "John Doe",
  "senderPhone": "+201234567890",
  "content": "Hello",
  "createdAt": "2026-07-29T12:00:00"
}
```

### NotificationResponse — on `/user/queue/notifications`

```json
{
  "id": "uuid",
  "userId": "uuid",
  "title": "New Offer Received",
  "message": "A nurse has submitted an offer.",
  "type": "BOOKING",
  "isRead": false,
  "relatedEntityType": "SERVICE_REQUEST",
  "relatedEntityId": "uuid",
  "createdAt": "2026-07-29T12:00:00",
  "updatedAt": "2026-07-29T12:00:00"
}
```

**`type` values:** `BOOKING`, `PAYMENT`, `SYSTEM`, `MESSAGE`, `REMINDER`

### NurseOfferResponse — inside ReservationEvent data

```json
{
  "id": "uuid",
  "serviceRequestId": "uuid",
  "nurseId": "uuid",
  "proposedPrice": 100.00,
  "proposedDate": "2026-08-15",
  "proposedTime": "10:00",
  "message": "optional note",
  "status": "PENDING",
  "createdAt": "2026-07-29T12:00:00",
  "updatedAt": "2026-07-29T12:00:00"
}
```

**`status` values:** `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`

### NearbyNurseServiceRequestResponse — on `/user/queue/nearby-request`

```json
{
  "serviceRequestId": "uuid",
  "profileId": "uuid",
  "serviceTypeId": "uuid",
  "serviceName": "Physical Therapy",
  "serviceDescription": null,
  "preferredDate": null,
  "preferredTime": null,
  "status": "SEARCHING",
  "latitude": 30.0444,
  "longitude": 31.2357,
  "distanceKm": 2.5,
  "estimatedPrice": null,
  "createdAt": "2026-07-29T12:00:00"
}
```

---

## Complete Flows

### Reservation Creation Flow

```
── REST ────────────────────────────────────────────
Patient POST /api/v1/service-requests
  Body: { profileId, serviceTypeId, latitude, longitude, ... }
  ├── Response 201: {
  │     serviceRequestId, status: "SEARCHING",
  │     nearbyNurses: [{ nurseId, lat, lng, distanceKm }, ...]
  │   }
  │
  └── Server then pushes to each nearby nurse:
      └── /user/{nurseId}/queue/nearby-request
          Payload: NearbyNurseServiceRequestResponse

── SOCKET ──────────────────────────────────────────
Nurse receives nearby-request push
(Or nurse polls REST GET /api/v1/service-requests/nearby on cold start)
```

### Offer / Negotiation / Acceptance Flow

```
── All over socket ─────────────────────────────────

Nurse SENDs /app/reservation/offer/create
  Body: { serviceRequestId, proposedPrice, proposedDate, proposedTime }
  │
  ├── Server persists NurseOffer (status=PENDING)
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_CREATED", data: NurseOfferResponse }
  └── Notifies patient via /user/queue/notifications

Nurse SENDs /app/reservation/offer/update
  Body: { offerId, proposedPrice: 90 }
  │
  ├── Server updates offer fields
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_UPDATED", data }
  └── Notifies patient

Patient SENDs /app/reservation/offer/counter
  Body: { offerId, proposedPrice: 80, proposedDate: "2026-08-16" }
  │
  ├── Server updates same offer with new terms
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_COUNTERED", data }
  └── Notifies nurse

Nurse SENDs /app/reservation/offer/accept   (or Patient sends)
  Body: { offerId }
  │
  ├── Server: offer → ACCEPTED
  │           request → ACCEPTED, nurse assigned
  │           all other pending offers → REJECTED
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_ACCEPTED", data }
  └── Notifies both parties

Nurse SENDs /app/reservation/offer/withdraw
  Body: { offerId }
  │
  ├── Server: offer → WITHDRAWN
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_WITHDRAWN", data }
  └── Notifies patient

Patient SENDs /app/reservation/offer/reject
  Body: { offerId }
  │
  ├── Server: offer → REJECTED
  ├── Pushes to /topic/reservation/{id}   { type: "OFFER_REJECTED", data }
  └── Notifies nurse

Patient SENDs /app/reservation/offers/list   (on entering reservation screen)
  Body: { serviceRequestId }
  │
  └── Server pushes to /topic/reservation/{id}   { type: "OFFERS_LIST", data: [...] }
```

### Cancellation Flow

```
Patient SENDs /app/reservation/cancel
  Body: { serviceRequestId }
  │
  ├── Server: request → CANCELLED, all pending offers → REJECTED
  ├── Pushes to /topic/reservation/{id}   { type: "REQUEST_CANCELLED" }
  └── Notifies patient + assigned nurse via /user/queue/notifications
      All other subscribed participants receive REQUEST_CANCELLED on the topic
```

### Chat Flow

```
Subscribe to /topic/chat/{reservationId}   (enter chat screen)
  │
  └── Server validates participant access

SEND /app/chat/{reservationId}/send   Body: { "content": "..." }
  │
  ├── Server persists to DB
  ├── Broadcasts ChatMessageResponse to /topic/chat/{reservationId}
  └── Notifies the other participant(s) via /user/queue/notifications
      (patient always, plus the assigned nurse if any; never the sender)

Fallback (no socket): POST /api/v1/reservations/{reservationId}/messages
  Body: { "content": "..." }  →  same persist + broadcast + notify behavior
```

### Notification Flow

```
Any event triggers (offer created, accepted, chat message, etc.)
  │
  ├── Notification persisted to PostgreSQL
  └── Server pushes to /user/queue/notifications   (in real-time)

Chat messages create a MESSAGE notification with
  relatedEntityType: "SERVICE_REQUEST",
  relatedEntityId:   the reservation (service request) id.

On reconnect:
  Call REST endpoint to fetch missed notifications since last known timestamp.
```

---

## Client Subscription Guide by Role & Screen

### Nurse

| Screen / State | Subscribe To | Expected Payload |
|---|---|---|
| App launch (always) | `/user/queue/notifications` | `NotificationResponse` |
| App launch (always) | `/user/queue/nearby-request` | `NearbyNurseServiceRequestResponse` — new matching requests pushed in real-time |
| Viewing a reservation | `/topic/reservation/{reservationId}` | `ReservationEvent` — all offer events, cancellation |
| Chat in a reservation | `/topic/chat/{reservationId}` | `ChatMessageResponse` |

### Patient

| Screen / State | Subscribe To | Expected Payload |
|---|---|---|
| App launch (always) | `/user/queue/notifications` | `NotificationResponse` |
| Viewing a reservation | `/topic/reservation/{reservationId}` | `ReservationEvent` — all offer events, cancellation |
| Chat in a reservation | `/topic/chat/{reservationId}` | `ChatMessageResponse` |

### Minimal Walkthrough — Nurse Journey

```
CONNECT (JWT)
  ├── SUBSCRIBE /user/queue/notifications
  └── SUBSCRIBE /user/queue/nearby-request

[Nurse receives a new request push]
  RECEIVE /user/queue/nearby-request  →  NearbyNurseServiceRequestResponse
  └── SUBSCRIBE /topic/reservation/{id}

[Nurse submits offer]
  SEND /app/reservation/offer/create
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_CREATED", data: NurseOfferResponse }

[Patient counters]
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_COUNTERED", data }

[Nurse accepts the counter]
  SEND /app/reservation/offer/accept
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_ACCEPTED", data }
  RECEIVE /user/queue/notifications  →  "Offer accepted. Reservation confirmed!"

[Nurse enters chat]
  SUBSCRIBE /topic/chat/{id}
  SEND /app/chat/{id}/send  +  RECEIVE ChatMessageResponse
  (or POST /api/v1/reservations/{id}/messages when the socket is unavailable)

On disconnect/reconnect:
  SUBSCRIBE to all the same topics again
  REST GET /api/v1/service-requests/nearby — pull open requests
  REST GET /api/v1/reservations/{id}/messages — pull missed chat (after is optional)
  REST GET /api/v1/notifications?after= — pull missed notifications
```

### Minimal Walkthrough — Patient Journey

```
CONNECT (JWT)
  └── SUBSCRIBE /user/queue/notifications

[Patient creates a service request via REST]
  POST /api/v1/service-requests
  RECEIVE 201  →  NearbyServiceRequestResponse (with nearbyNurses list)

[Patient receives notification about an offer]
  RECEIVE /user/queue/notifications  →  "New offer received"
  └── SUBSCRIBE /topic/reservation/{id}
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_CREATED", data }

[Patient counters]
  SEND /app/reservation/offer/counter
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_COUNTERED", data }

[Patient accepts]
  SEND /app/reservation/offer/accept
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_ACCEPTED", data }

[Patient enters chat]
  SUBSCRIBE /topic/chat/{id}
  SEND /app/chat/{id}/send  +  RECEIVE ChatMessageResponse
  (or POST /api/v1/reservations/{id}/messages when the socket is unavailable)

On reconnect:
  SEND /app/reservation/offers/list  →  RECEIVE OFFERS_LIST
  REST GET /api/v1/nurse-offers?serviceRequestId=  — pull offers
  REST GET /api/v1/reservations/{id}/messages — pull missed chat (after is optional)
  REST GET /api/v1/notifications?after=  — pull missed notifications
```

---

## REST Fallbacks (for cold start / reconnect)

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/service-requests/nearby` | Nurse fetches all open requests on first load |
| `GET /api/v1/nurse-offers?serviceRequestId=` | Patient fetches current offers on reconnect |
| `GET /api/v1/reservations/{id}/messages` | Fetch chat messages (`?after=ISO-DATE-TIME` optional — omit for full history) |
| `POST /api/v1/reservations/{id}/messages` | Send a chat message via REST: body `{ "content": "..." }` — persists + broadcasts to `/topic/chat/{id}` + notifies the other participant |
| `GET /api/v1/notifications?after=` | Fetch missed notifications |

---

## Error Handling

| Scenario | What Happens | App Action |
|---|---|---|
| Invalid / expired JWT | `ERROR` on CONNECT | Refresh token or redirect to login |
| Subscribe to unauthorized topic | `ERROR` (403, "Not a participant") | Alert user, do not retry |
| Connection lost | WebSocket close | Offline indicator, auto-reconnect with backoff, re-subscribe, pull missed data via REST |
| Invalid offer operation | `ERROR` (e.g., "Only pending offers can be accepted") | Show feedback to user |

---

## Redis Data Model (reference)

| Redis Key | Type | Purpose |
|---|---|---|
| `ws:nurse:online` | Set | User IDs of connected nurses |
| `ws:nurse:available` | GeoSet | User IDs + lat/lng of available nurses |
| `ws:nurse:available:ts` | Hash | User ID → epoch timestamp of last heartbeat |

Stale entries purged every 30s (entries older than 90s removed).
