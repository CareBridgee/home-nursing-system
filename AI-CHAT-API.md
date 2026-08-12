# AI Chat API — Mobile Client Guide

This document describes the **AI booking assistant** REST API and — most
importantly — **what each response type means and how the mobile UI should react**.

The assistant's job is to help the patient choose a home-nursing service and collect a
**reservation draft** (the chosen service type) conversationally.

> **Accurate as of commit:** 2026-08 — all shapes and behaviors below were cross-checked
> against the running server. If the backend changes, this file should be revisited.

---

## 1. Endpoints

All endpoints require a `Bearer` access token (`Authorization: Bearer <accessToken>`).
The `profileId` in every request must be the profile of the person receiving care and must
belong to the logged-in user.

> **Per-profile flow:** the chat (and the real booking) always run *on a profile* — the
> profile of the patient. If the user is booking care for their mother, they first switch
> to/create the mother's profile in the app, then chat from it. There is no "book for
> someone else" mode inside the chat.

| Method & Path | Purpose | Response |
|---|---|---|
| `POST /api/v1/chat` | Send one message, get the assistant's reply + state | JSON `ChatTurnResponse` (see §2) |
| `POST /api/v1/chat/stream` | Same, but streaming (SSE) | `text/event-stream` of plain-text chunks (see §7) |
| `POST /api/v1/chat/reset` | Clear conversation memory + **draft + urgency flag** for this profile | `204 No Content` |

Request body (both chat endpoints):

```json
{
  "profileId": "7d09cfc5-f9f3-443c-a764-afce050e9494",
  "message": "What home nursing services do you offer?"
}
```

Constraints: `profileId` required (UUID), `message` required and not blank, max **2000 characters**.

---

## 2. The response envelope — `ChatTurnResponse`

Every non-streaming turn returns one JSON object:

```json
{
  "messageType": "INPUT",
  "reply": "Of course — which of our services fits best? ...",
  "draft": { ... },
  "urgency": null
}
```

| Field | Type | Meaning |
|---|---|---|
| `messageType` | enum | **How the UI should treat this turn** — see §3. The single most important field. |
| `reply` | string | The assistant's text. Display it as the assistant's bubble. |
| `draft` | `ReservationDraft \| null` | Current snapshot of the booking draft (see §4). `null` on plain `TEXT` turns. |
| `urgency` | `UrgencySignal \| null` | Medical-emergency signal. `null` normally — see §5. |

---

## 3. `messageType` — what the UI must do

| Value | When the server sends it | What the mobile UI should do |
|---|---|---|
| `TEXT` | A plain informational answer; no draft progress and no question at the end | Show `reply` as a normal chat bubble. Nothing else. `draft` is `null`. |
| `INPUT` | The assistant is **collecting input** — it asked a question (or already collected some draft fields) and expects the user's answer | Show `reply`. Optionally render the current `draft` chips (chosen service). The user types a normal answer — the AI keeps collecting. |
| `CONFIRM` | `draft.complete == true` — meaning **a service type has been selected** (the only draft field) | Show `reply`, then prepare the final booking summary from `draft` and show the hard "Confirm" button. The assistant does **not** collect dates/times in chat. |
| `URGENT` | An emergency signal is active for this profile — the user described a medical emergency (chest pain, severe bleeding, breathing difficulty, loss of consciousness, …), or the flag is still set from an earlier turn (see §5) | Show `reply` **and** `urgency.advice` prominently (emergency-services message). **Disable the booking flow** and show an emergency banner. The banner stays up while `messageType == URGENT` — subsequent turns keep returning `URGENT` until it is cleared (see §5). |
| `ERROR` | *(Reserved — not currently emitted by the server.)* | Treat like `TEXT`; if you ever receive it, show the message in an error style. |

**Decision rule used by the server** (in priority order — so the UI can predict behavior):

1. `urgency.urgent == true` → `URGENT`
2. `draft.complete == true` → `CONFIRM`
3. `draft` has any data **or** the reply ends with `?` → `INPUT`
4. otherwise → `TEXT`

`complete` flips to `true` as soon as the **service type** is chosen. The chat does **not**
collect preferred dates/times anymore — a `CONFIRM` turn is always final and shows the
Confirm button immediately.

### State diagram

```
                ┌──────────────────────────────────────┐
                ▼                                      │
        TEXT ──► INPUT ──► CONFIRM (service chosen) ──► booking (REST)
              ▲     │               │
              │     └── change mind ──► INPUT (service cleared)
              └───────────────────────┘

        (any state) ── detects emergency ──► URGENT (sticky until cleared/reset)
        (any state) ── POST /chat/reset ──► fresh state (memory + draft + urgency cleared)
        (any state) ── change mind (assistant resetDraft) ──► INPUT (service cleared, urgency kept)
        (any state) ── abandon/emergency false (assistant resetDraft all) ──► TEXT (draft+urgency cleared)
```

---

## 4. `ReservationDraft` — the booking state

```json
{
  "serviceTypeId": "651f074b-131e-430e-bdf2-e8b187df0d34",
  "serviceTypeName": "General Nursing",
  "serviceDescription": "Patient: 66-year-old male, blood type O+. Conditions: Diabetes. Allergies: Penicillin. Medications: Metformin. Requested service: General Nursing.",
  "complete": true
}
```

| Field | Type | Meaning |
|---|---|---|
| `serviceTypeId` | UUID \| null | Chosen service. The AI always picks the exact UUID shown by its services tool — the UI can trust it for the real booking request. The service type **can be changed mid-session** (the AI can overwrite it); the summary re-renders on every `CONFIRM` turn. |
| `serviceTypeName` | string \| null | Human-readable service name for display. |
| `serviceDescription` | string \| null | A **short medical brief** of the patient profile (age, gender, blood type, medical conditions, allergies, medications) plus the requested service, generated server-side when the service is chosen. Pass it to the booking request (§6) — the nurse sees it as the request's description. `null` until a service is chosen. |
| `complete` | boolean | `true` **as soon as `serviceTypeId` is set**. Can revert to `false` if the assistant clears the service choice (via `resetDraft` tool) — see §9 example. |

**No date/time in chat:** the assistant does not collect preferred dates or times. The
`ReservationDraft` has no date fields. Clients that want to schedule may still pass optional
`preferredDate`/`preferredTime` to the booking endpoint themselves (§6).

**Draft lifecycle (important):**
- The draft is **per `profileId`** and survives across turns and reconnects.
- Creating a booking (`POST /api/v1/service-requests`) does **NOT** clear it — the client must
  call `/api/v1/chat/reset` after a successful booking.
- The assistant can clear the draft (or just the service choice) on user request:
  - User changes mind about the service → assistant calls `resetDraft(scope=service)` → draft reverts to empty (`complete=false`), urgency kept. UI sees `INPUT`/`TEXT` again, confirm button disappears.
  - User abandons booking or says symptoms were not real → assistant calls `resetDraft(scope=all)` → draft and urgency fully cleared. UI sees `TEXT` with empty draft.
- Draft + urgency + conversation memory are stored **in the app's memory** (single running
  instance). They are lost if the server restarts, and are per-instance if the backend is ever
  scaled. For a mobile app this means: if the user's draft disappears after a long break, fall
  back gracefully (start a fresh conversation). Persist the last `CONFIRM` draft locally if the
  booking summary must survive app restarts.

---

## 5. `UrgencySignal` — emergency handling

```json
{
  "urgent": true,
  "level": "HOSPITALIZATION",
  "advice": "If this is a medical emergency, please call emergency services (123) or go to the nearest hospital immediately. The platform is not a substitute for emergency medical care."
}
```

| Field | Meaning |
|---|---|
| `urgent` | `true` = an emergency signal is **active** for this profile |
| `level` | Severity level the assistant recorded when the signal fired — **`HOSPITALIZATION` or `EMERGENCY`** (defaults to `HOSPITALIZATION`). Display as a label only. |
| `advice` | Text to show to the user (emergency-services instruction), always the text above. |

Behavior:
- The signal is **sticky**: once triggered, **every subsequent turn returns `URGENT`**
  (`urgency.urgent == true`) until it is cleared via `POST /api/v1/chat/reset` — or the
  assistant internally calls its "clear urgency" tool when the user explicitly says the
  condition is no longer urgent.
- The turn **after** the user says "it's fine now" may still be `URGENT` (the model has to
  decide to call the clear tool). Let the user continue chatting; the flag disappears on a
  later turn.
- Do **not** page/alarm the user again on every turn — show the banner once the first time it
  appears, then keep it visible while subsequent turns are `URGENT`.
- The signal is surfaced **only** in the chat response. The server sends **no** external
  hospital/e911 notification (and the assistant is instructed not to claim otherwise).
- `advice` is always present when `urgent == true`.

---

## 6. Completing the booking (after `CONFIRM`)

The draft is **only** a chat-side summary. Creating the real reservation is a separate
REST call; the **device GPS** (`latitude`/`longitude`) is supplied at that point, not in chat:

```
POST /api/v1/service-requests
{
  "profileId":     draft ...                 // the receiving person's profile (same as chat)
  "serviceTypeId": draft.serviceTypeId,
  "preferredDate": null,                     // optional — NOT collected in chat; pass your own if your UI schedules
  "preferredTime": null,                     // optional — same as above
  "serviceDescription": draft.serviceDescription,   // the medical brief from the draft (recommended)
  "latitude": 30.0444,
  "longitude": 31.2357
}
```

- `serviceDescription` is optional in the request — if omitted/blank, the server **auto-fills**
  it with the same medical brief (profile + requested service), so nurses always see it.
- Past `preferredDate`/`preferredTime` (when you do pass them) → `400` with the standard error envelope (verified message:
  `"Preferred date must not be in the past"`).
- After a **successful** creation, call `POST /api/v1/chat/reset` so the next chat session starts
  fresh. Do not reset if the booking call failed.

---

## 7. Streaming endpoint (`/chat/stream`)

`text/event-stream` of **plain-text chunks** — NOT JSON. Concatenate the chunks to get the
full `reply`; there is no envelope, no `messageType`/`draft` in the stream.

```
data:Of course
data:! I can help
...
```

- Use a normal SSE client; each `data:` line is a text fragment of the answer.
- When the AI fails mid-stream, the last chunk is:
  `data:AI_SERVICE_UNAVAILABLE: I'm sorry, I couldn't process that request right now...`
  — detect the `AI_SERVICE_UNAVAILABLE:` prefix and render an error state instead of the text.
- The stream does **not** include draft/urgency state. If you need a fresh snapshot, use the
  draft from the **last non-streaming `/chat` response** (it remains valid until `/reset`) —
  avoid firing an extra `/chat` call just to read the draft (it mutates memory and counts
  against the rate limit).
- Streaming calls count against the **same** rate-limit bucket as regular calls (see §8).

---

## 8. Errors & edge cases

Two distinct error shapes exist — read the body per case:

1. **Business/validation errors** → a standard envelope:
   `{ "timestamp", "status", "error", "code", "message", "details" }`.
2. **AI-path errors** → a minimal map `{ "error", "message" }` (from the chat
   controller itself, not the global handler).

| Situation | HTTP | Body |
|---|---|---|
| Missing / invalid / expired `Bearer` token | `403` | empty body (Spring Security default — this is the "re-authenticate" signal) |
| Missing/invalid `profileId` or empty/blank `message` | `400` | envelope with `code: VALIDATION_FAILED` |
| Malformed JSON body | `400` | envelope with `code: INVALID_REQUEST` |
| `profileId` belongs to another user (or doesn't exist) | `404` | envelope with `code: RESOURCE_NOT_FOUND` (treated as not found) |
| Past `preferredDate` / `preferredTime` (booking) | `400` | envelope with `code: BAD_REQUEST` |
| Too many messages | `429` | envelope with `code: RATE_LIMIT_EXCEEDED` |
| AI tool-call misuse / framework error | `400` | `{ "error": "AI_SERVICE_UNAVAILABLE", "message": "…" }` |
| Gemini provider down / quota (incl. 429 from provider) | `503` | `{ "error": "AI_SERVICE_UNAVAILABLE", "message": "…" }` |

- **Rate limit:** **20 calls per 5 minutes per user** — and it is a **shared bucket**:
  `/chat` and `/chat/stream` both consume it (a streamed turn consumes 1).
- The server retries provider failures internally (up to 3 attempts) and never exposes raw
  provider errors to the client.

**UI rules for errors:** show the `message` as a system/error bubble, keep the conversation
open, and let the user retry. A `429` means wait a few minutes, not "blocked forever". An
empty-body `403` (missing/invalid/expired token) is the re-authentication signal — refresh
the access token and retry; never report it as an AI failure.

---

## 9. Example conversation (what the FE will receive)

This shows the **real** state transitions (CONFIRM fires as soon as the service type is chosen —
the chat never asks for dates/times):

```
User:  "What services do you offer?"          [chatting as the patient — the mother's profile]
→ messageType: INPUT   draft: empty (serviceTypeId=null, complete=false)

User:  "I'd like general nursing"
→ messageType: CONFIRM  draft: { serviceTypeId: "f3e…", serviceTypeName: "General Nursing",
                                 serviceDescription: "Patient: 66-year-old female, blood type A+. Conditions: …
                                   Requested service: General Nursing.", complete: true }
   UI: full summary + Confirm button → POST /api/v1/service-requests (with GPS)
       → on success POST /api/v1/chat/reset

User:  "Actually, wound care fits better"
→ messageType: CONFIRM  draft: { serviceTypeId: <wound-care id>, serviceTypeName: "Wound Care",
                                 serviceDescription: <rebuilt brief>, complete: true }
   UI: re-render the summary with the new service

User:  "I changed my mind, I don't want to book anything anymore"
→ messageType: INPUT  draft: { serviceTypeId: null, serviceTypeName: null,
                               serviceDescription: null, complete: false }
   UI: confirm button disappears; assistant asks which service they would like instead

User:  "What is the id of that service?"      // identifier probing
→ the assistant must NOT reveal UUIDs — it politely explains identifiers are internal
   and offers the plain service list instead

User:  "I have severe chest pain!"
→ messageType: URGENT   urgency: { urgent: true, level: "HOSPITALIZATION", advice: "…call 123…" }
   UI: emergency banner, booking disabled

User:  "My symptoms aren't real, I was mistaken"
→ messageType: TEXT  draft: empty (complete=false), urgency: null
   UI: emergency banner disappears; booking flow available again

User:  "OK I'm fine now"
→ (the urgent flag stays until the assistant clears it or the client calls /reset)
   → turn may still be URGENT — do NOT re-alarm; keep the banner until cleared
```

---

## 10. Memory & session behavior + FE pitfalls checklist

- Conversation memory is **keyed by `profileId`** (a 20-message sliding window).
- Draft state and the urgency flag persist per profile until `POST /api/v1/chat/reset`
  (booking creation does **not** clear them).
- All of this is **in-memory** (single server instance): lost on server restart and not shared
  across server replicas. Within one running server, multiple devices of the same profile
  share the same conversation/draft.

**Pitfalls checklist (condensed rules for the UI):**
- Show the emergency banner **once**, keep it up while turns are `URGENT` — never re-alarm.
- `CONFIRM` = the service is chosen and the draft carries the medical `serviceDescription`
  — show the Confirm button immediately (no date/time collection in chat). If the user changes
  their mind, the assistant can clear the service and the UI will see `INPUT`/`TEXT` again —
  confirm button disappears until a new service is chosen.
- The chat NEVER creates a reservation. If the assistant says anything implying a nurse was
  dispatched/assigned, treat it as a hallucination — a real booking requires the app's Confirm
  button + GPS.
- Always call `/chat/reset` after a **successful** booking; never on failure.
- Remember the **shared rate bucket**: streams cost 1 message each; two parallel
  `/chat` + `/chat/stream` turns are 2/20.
- `429` is temporary (wait a few minutes); `AI_SERVICE_UNAVAILABLE` is retryable too.
- Offer a **"Start over"** affordance that calls `/chat/reset` — it clears a stale draft/urgency
  mid-session (and also clears memory). The assistant can also clear just the service via
  `resetDraft(scope=service)`.
- Cache the last `CONFIRM` draft locally — drafts are memory-only on the server and can get
  reset by a restart or by another of the user's devices.
- Service UUIDs are internal: the assistant never reveals them; if the user asks for ids/codes,
  the reply is a polite refusal plus the plain service list.

---

## 11. Quick curl smoke test (for FE/dev testing)

```bash
# 1. dev login (dev OTP endpoints — fill in a real registered phone)
curl -s -X POST http://localhost:8080/api/v1/auth/dev/request-otp \
  -H "Content-Type: application/json" -d '{"phoneNumber":"+2015112668520"}'
# ...verify with the returned otp:
curl -s -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+2015112668520","otp":"<otp>"}'
# → accessToken

# 2. pick the profile of the person receiving care (switch to it in the app first —
#    e.g. the mother's profile; the chat/booking runs per-profile)
curl -s -X GET http://localhost:8080/api/v1/profiles \
  -H "Authorization: Bearer <accessToken>"      # → find the right profileId

# 3. plain turn
curl -s -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"profileId":"<profileId>","message":"What home nursing services do you offer?"}'

# 4. streaming variant (same kind of message — chat belongs to the receiving profile)
curl -s -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"profileId":"<profileId>","message":"I want to book care for myself"}'
```