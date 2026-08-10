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
| Send rule | Clients may only `SEND` to `/app/**` destinations — others are rejected with `ERROR` |

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
7. If `role` is `PATIENT` → the server auto-cancels the patient's **open unassigned** requests (see [Session Lifecycle — Auto-Cancellation](#session-lifecycle--auto-cancellation)).
8. Presence is heartbeat-driven: `/app/heartbeat` (send every ~30 s) refreshes the online + availability timestamps; the server prunes entries stale for > 90 s (covers abrupt kills that never reach `DISCONNECT`).

**Subscription authorization rules (server-enforced at `SUBSCRIBE`):**
- `/user/...` — allowed only for your own user id; subscribing to someone else's `/user/{id}/...` → `ERROR` frame.
- `/topic/reservation/{id}` and `/topic/chat/{id}` — must be a participant: the request owner, the assigned nurse, or a nurse with an **active `PENDING` offer** (offers that were rejected or withdrawn no longer grant access; access is enforced at subscribe time).
- `SEND` frames are only allowed to `/app/**`; sending to `/topic`, `/queue` or `/user` is rejected with `ERROR`.

### Reconnection

Detect close/error events, reconnect with a fresh access token, and re-subscribe to all active topics.

**IMPORTANT:** any `ERROR` frame from the server is terminal — the server closes the WebSocket session right after sending it. Always reconnect on `ERROR`; never retry the offending frame on the same session.

### Session Lifecycle — Auto-Cancellation

When a **patient's** session ends or a **patient** unsubscribes from any destination, the server cancels that patient's open unassigned service requests (`PENDING`/`SEARCHING`/`BOOKING`/`NEGOTIATING`, no nurse assigned). Each cancellation follows the [Cancellation Flow](#cancellation-flow) (offers auto-rejected, `REQUEST_CANCELLED` pushed, nurses notified).

- Triggered by: `DISCONNECT`/socket close, and any `UNSUBSCRIBE` frame while connected.
- Cancelled requests are *unassigned only* — once a nurse is assigned (`ACCEPTED`), the visit survives disconnects of both parties (completing requires the nurse + QR code, per [Visit Completion](#visit-completion-qr)).
- Nurses are unaffected: nurse disconnect only flips presence state.

---

## Topics to Subscribe To

| Topic Pattern | When | Purpose | Access |
|---|---|---|---|
| `/user/queue/notifications` | Always while connected | Receive real-time notifications for the current user | User-scoped |
| `/user/queue/errors` | Always while connected | Receive structured error feedback for failed socket operations (see Error Handling) | User-scoped |
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
| `REQUEST_CANCELLED` | `{}` | Patient or assigned nurse cancels the entire request |
| `COMPLETED` | `null` | Assigned nurse completes the visit via QR scan (REST-driven) |
| `OFFERS_LIST` | `[NurseOfferResponse, ...]` | Response to `/app/reservation/offers/list` |

---

## Commands to Send (SEND to `/app/*`)

### Nurse Presence (nurse role required)

| Destination | Payload | When |
|---|---|---|
| `/app/heartbeat` | (empty) | Every ~30 seconds |
| `/app/reservation/availability` | `{ "available": true, "lat": 30.0, "lng": 31.0 }` | Toggle availability |
| `/app/reservation/availability` | `{ "available": false }` | Go unavailable |
| `/app/reservation/location` | `{ "lat": 30.0, "lng": 31.0 }` | Update current position |

All three require `ROLE_NURSE` **and an `APPROVED` verification status** — non-nurses receive `ERROR`; unreviewed (PENDING) nurses get a `FORBIDDEN` `SocketErrorPayload` on `/user/queue/errors`.

### Offer Management

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/reservation/offer/create` | `{ "serviceRequestId": "uuid", "proposedPrice": 100.00, "proposedDate": "2026-08-15", "proposedTime": "10:00", "message": "optional note" }` | Nurse | Creates a pending offer |
| `/app/reservation/offer/update` | `{ "offerId": "uuid", "proposedPrice": 90.00, "proposedDate": "...", "proposedTime": "...", "message": "..." }` | Nurse | Updates own pending offer's terms |
| `/app/reservation/offer/counter` | `{ "offerId": "uuid", "proposedPrice": 80.00, "proposedDate": "...", "proposedTime": "...", "message": "..." }` | Patient | Proposes counter-terms on a pending offer |
| `/app/reservation/offer/accept` | `{ "offerId": "uuid" }` | Either | Accepts current terms → finalizes reservation |
| `/app/reservation/offer/withdraw` | `{ "offerId": "uuid" }` | Nurse | Withdraws own pending offer |
| `/app/reservation/offer/reject` | `{ "offerId": "uuid" }` | Patient | Rejects an offer |

Counter, reject and withdraw are all also available over REST (see REST Fallbacks).

### Request Management

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/reservation/cancel` | `{ "serviceRequestId": "uuid" }` | Patient or assigned nurse | Cancels request, rejects all pending offers |
| `/app/reservation/offers/list` | `{ "serviceRequestId": "uuid" }` | Participant | Triggers server to push `OFFERS_LIST` on the reservation topic |

Cancellation is also available via REST `PATCH /api/v1/service-requests/{id}/cancel`.

### Visit Completion (QR)

| Step | Call | Who | Effect |
|---|---|---|---|
| 1. Get visit code | `POST /api/v1/service-requests/{id}/visit-code` | Patient | Mints (or reuses) an 8-char code, stored in Redis with TTL; returns `VisitCodeResponse(serviceRequestId, code, expiresAt)` — UI renders it as a QR. Only allowed while status is `ACCEPTED`; notifies the patient on fresh mint |
| 2. Complete | `POST /api/v1/service-requests/{id}/complete` body `{ "visitCode": "..." }` | Assigned nurse | Verifies the code (single-use, 5-attempt lockout), sets status `COMPLETED`, pushes `COMPLETED` on `/topic/reservation/{id}`, notifies the patient |

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

### SocketErrorPayload — on `/user/queue/errors`

Sent when a socket *operation* (SEND) fails at the handler level — validation, authorization, or business rule. The session stays open.

```json
{
  "code": "BAD_REQUEST",
  "message": "Only pending offers can be accepted",
  "timestamp": "2026-07-29T12:00:00+02:00"
}
```

**`code` values:** business-rule codes propagated from the server (`FORBIDDEN`, `BAD_REQUEST`, `RESOURCE_NOT_FOUND`, ...), plus `VALIDATION` for payload validation failures and `INTERNAL` for unexpected errors

### NurseOfferResponse — inside ReservationEvent data

```json
{
  "id": "uuid",
  "serviceRequestId": "uuid",
  "nurse": {
    "id": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "ratingAvg": 4.5,
    "totalReviews": 12
  },
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
Patient (or the assigned nurse) SENDs /app/reservation/cancel
  Body: { serviceRequestId }
  │
  ├── Server: request → CANCELLED, all pending offers → REJECTED
  ├── Pushes to /topic/reservation/{id}   { type: "REQUEST_CANCELLED" }
  └── Notifies via /user/queue/notifications:
        ├── patient cancels with an assigned nurse → that nurse gets "Request Cancelled"
        ├── patient cancels with NO assigned nurse → EVERY nurse holding a PENDING offer
        │       gets "Request Cancelled" (their offers are auto-rejected)
        └── nurse cancels → the patient gets "Request Cancelled"
      The actor is never notified.
      All subscribed participants receive REQUEST_CANCELLED on the topic
      REST fallback: PATCH /api/v1/service-requests/{id}/cancel

Also triggered automatically when a patient's session ends or the patient unsubscribes
(open unassigned requests only — see Session Lifecycle — Auto-Cancellation).
```

### Business Rules (enforced server-side)

| Rule | Statuses | Enforced at |
|---|---|---|
| One active service request per profile | `PENDING`/`SEARCHING`/`BOOKING`/`NEGOTIATING`/`ACCEPTED`/`IN_PROGRESS` | `POST /api/v1/service-requests` |
| One active visit per nurse | `ACCEPTED`/`IN_PROGRESS` | offer create (REST + socket) |
| Offer create eligibility | — | nurse must be `APPROVED`, attached to the request's service type, request must be `SEARCHING` and unassigned |
| Withdraw / update / accept / counter / reject | — | offer must still be `PENDING` |

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
| App launch (always) | `/user/queue/errors` | `SocketErrorPayload` — operation-level failures |
| App launch (always) | `/user/queue/nearby-request` | `NearbyNurseServiceRequestResponse` — new matching requests pushed in real-time |
| Viewing a reservation | `/topic/reservation/{reservationId}` | `ReservationEvent` — all offer events, cancellation |
| Chat in a reservation | `/topic/chat/{reservationId}` | `ChatMessageResponse` |

### Patient

| Screen / State | Subscribe To | Expected Payload |
|---|---|---|
| App launch (always) | `/user/queue/notifications` | `NotificationResponse` |
| App launch (always) | `/user/queue/errors` | `SocketErrorPayload` — operation-level failures |
| Viewing a reservation | `/topic/reservation/{reservationId}` | `ReservationEvent` — all offer events, cancellation |
| Chat in a reservation | `/topic/chat/{reservationId}` | `ChatMessageResponse` |

### Minimal Walkthrough — Nurse Journey

```
CONNECT (JWT)
  ├── SUBSCRIBE /user/queue/notifications
  ├── SUBSCRIBE /user/queue/errors
  └── SUBSCRIBE /user/queue/nearby-request

[Nurse receives a new request push]
  RECEIVE /user/queue/nearby-request  →  NearbyNurseServiceRequestResponse

[Nurse submits offer]
  SEND /app/reservation/offer/create
  RECEIVE /topic/reservation/{id}  →  { type: "OFFER_CREATED", data: NurseOfferResponse }

Note: as a non-participant (no active `PENDING` offer yet), subscribing to /topic/reservation/{id}
before creating your offer returns an ERROR frame. Subscribe to the topic AFTER
your first offer exists — until then, use REST GET /api/v1/nurse-offers?serviceRequestId=.
A nurse whose offer was rejected or withdrawn loses topic access on the next subscribe.

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
  ├── SUBSCRIBE /user/queue/notifications
  └── SUBSCRIBE /user/queue/errors

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
| `GET /api/v1/service-requests/{id}/preview` | Nurse previews an open request (service details + patient basic medical summary) before offering |
| `GET /api/v1/service-requests/{id}/profile` | Assigned nurse loads the patient's full profile (contact, address, medical summary) after acceptance |
| `GET /api/v1/service-requests/current` | Patient or nurse fetches their current active visit (rich DTO incl. the other party's summary); `404` when none |
| `GET /api/v1/profiles/report/{profileId}/report` | Assigned nurse fetches the patient's medical report; `404` for non-assigned nurses |
| `GET /api/v1/service-requests/{id}/nearby-nurses` | Patient fetches currently-nearby nurses for their open request (on demand — e.g., when nurses go available) |
| `GET /api/v1/nurse-offers?serviceRequestId=` | Patient fetches current offers on reconnect |
| `PUT /api/v1/nurse-offers/{id}` | Nurse updates own offer terms (same as `/app/reservation/offer/update`) |
| `PATCH /api/v1/nurse-offers/{id}/counter` | Patient proposes counter-terms (same as `/app/reservation/offer/counter`) |
| `PATCH /api/v1/nurse-offers/{id}/reject` | Patient rejects an offer (same as `/app/reservation/offer/reject`) |
| `PATCH /api/v1/nurse-offers/{id}/accept` | Accepts current terms → finalizes reservation |
| `DELETE /api/v1/nurse-offers/{id}` | Nurse withdraws own pending offer (same as `/app/reservation/offer/withdraw`) |
| `PATCH /api/v1/service-requests/{id}/cancel` | Patient or assigned nurse cancels the request (same as `/app/reservation/cancel`) |
| `GET /api/v1/reservations/{id}/messages` | Fetch chat messages (`?after=ISO-DATE-TIME` optional — omit for full history) |
| `POST /api/v1/reservations/{id}/messages` | Send a chat message via REST: body `{ "content": "..." }` — persists + broadcasts to `/topic/chat/{id}` + notifies the other participant |
| `POST /api/v1/service-requests/{id}/visit-code` | Patient fetches (mints or reuses) the visit code to render as a QR |
| `POST /api/v1/service-requests/{id}/complete` | Assigned nurse completes the visit: body `{ "visitCode": "..." }` — sets status `COMPLETED`, pushes `COMPLETED` event, notifies the patient |
| `POST /api/v1/notifications` | Create a notification — only for the calling user id (self-only); body userId is ignored |
| `GET /api/v1/notifications?after=` | Fetch missed notifications |

> **Manual harness:** `ws-test.html` (repo root) is a browser-based playground for this protocol —
> connect as nurse or patient with a pasted JWT, subscribe to all topics, send frames,
> run the walkthroughs, and watch `/user/queue/errors` entries live.

---

## Error Handling

There are **two distinct channels** — keep them apart:

### 1. Frame-level rejections → `ERROR` frame, then the server closes the session

A frame is rejected *before it reaches any handler* when it violates a connection-level rule:

- `SEND` to a destination that is not `/app/**` (`/topic`, `/queue`, `/user`, ...)
- `SUBSCRIBE` to another user's `/user/{id}/...`
- `SUBSCRIBE` to `/topic/reservation/{id}` or `/topic/chat/{id}` without being a participant (request owner, the assigned nurse, or a nurse with an active `PENDING` offer)
- `SEND` to a presence endpoint (`/app/heartbeat`, `/app/reservation/availability`, `/app/reservation/location`) without `ROLE_NURSE`
- `CONNECT` with a missing / invalid / expired JWT

| Scenario | What Happens | App Action |
|---|---|---|
| Invalid / expired JWT on CONNECT | `ERROR` frame | Refresh token and reconnect |
| Subscribe to unauthorized topic | `ERROR` frame | Alert user, do not retry |
| Subscribe to another user's `/user/{id}/...` | `ERROR` frame | Alert user (likely a client bug), do not retry |
| `SEND` to `/topic`, `/queue` or `/user` | `ERROR` frame | Fix client bug — the server never accepts client publishes to broker destinations |
| Non-nurse uses presence endpoints | `ERROR` frame | Alert user |

The `message` header of these `ERROR` frames is **always** the generic
`Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]` — the semantic reason
(e.g. "Not a participant", "Only nurses...") is **only written to the server logs**, never sent to the
client. **The server then closes the connection (close code 1002).** Treat any `ERROR` frame as
terminal: reconnect with a fresh access token and re-subscribe (never retry the failing frame).

### 2. Operation-level failures → `SocketErrorPayload` on `/user/queue/errors`

The frame passed the connection rules but the operation itself failed (validation, authorization, or a
business rule inside the handler). The server sends a structured error to the session's user queue —
the session **stays open**.

| Scenario | `code` | `message` example |
|---|---|---|
| Role/ownership check inside the operation | `FORBIDDEN` | `Only nurses can use presence endpoints` |
| Unapproved nurse uses presence endpoints (handler-level) | `FORBIDDEN` | `Only approved nurses can use presence endpoints` |
| Invalid offer operation | `BAD_REQUEST` | `Only pending offers can be accepted` |
| Missing entity (e.g. unknown offer id) | `RESOURCE_NOT_FOUND` | `Nurse offer not found: <id>` |
| Payload failed `@Valid` validation | `VALIDATION` | `content: Message content is required` |
| Unexpected server error | `INTERNAL` | `Unexpected server error` (details log-only) |

App action: show feedback to the user and let them fix/retry the operation.

### 3. Transport-level

| Scenario | What Happens | App Action |
|---|---|---|
| Connection lost | WebSocket close | Offline indicator, auto-reconnect with backoff, re-subscribe, pull missed data via REST |
| Invalid / locked-out visit code | `400` from `POST .../complete` | Prompt the patient to regenerate the QR via `POST .../visit-code` |

---

## Redis Data Model (reference)

| Redis Key | Type | Purpose |
|---|---|---|
| `ws:nurse:online` | Set | User IDs of connected nurses |
| `ws:nurse:online:ts` | Hash | User ID → epoch timestamp of last activity (connect/heartbeat/location) — used for stale pruning |
| `ws:nurse:available` | GeoSet | User IDs + lat/lng of available nurses |
| `ws:nurse:available:ts` | Hash | User ID → epoch timestamp of last heartbeat/location update |

Stale entries purged every 30s (entries older than 90s removed; members without a timestamp are treated as stale).
