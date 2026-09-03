# Service Analysis Document — Template

This is the structure the `analyze-service` skill fills. Every section
below must appear in the output in this order. Fill each section from
actual code and configuration. Where information cannot be found, write
`_Not found in codebase — confirm with team_` — do not infer, guess,
or substitute a typical default.

The template supports four scopes: whole service, multiple endpoints,
single endpoint, or specific method. The structure is the same for all
scopes — just fewer entries appear in the scope-specific sections when
the scope is narrower.

---

## 1. Overview

- **Service Name:**
- **Scope of this analysis:** _(whole service / specific endpoints / specific method — state which)_
- **Purpose:** _(one paragraph — what business capability does this service own?)_
- **Technology Stack:** _(language, framework, runtime version if detectable)_
- **Repository Path:**
- **Owner / Team:**
- **Last Analysed:** _(date of generation, YYMMDD)_

---

## 2. Endpoints / Methods in Scope

For **every** target in scope (REST endpoint, gRPC method, GraphQL
operation, event consumer, scheduled job, or method-scoped target),
produce a sub-section in this exact shape. If the scope is a single
endpoint or method, there will be one sub-section. If the scope is the
whole service, there will be one sub-section per exposed endpoint.

### 2.x `<HTTP_METHOD> <PATH>` — `<Short Title>`

(For method-scoped analysis, use `<ClassName>.<methodName>` as the
heading instead of HTTP method and path.)

| Field | Detail |
|---|---|
| **Method** | GET / POST / PUT / PATCH / DELETE / EVENT / METHOD |
| **Path / Topic / Signature** | Full path, queue/topic name, or method signature |
| **Description** | What this endpoint or method does |
| **Auth / Access Control** | JWT, API key, role required, public, etc. |
| **Rate Limiting** | If present |

**Request Payload / Method Inputs**

```json
{
  "field": "type — description — required/optional"
}
```

**Response Payloads / Method Outputs**

| Status | Condition | Body |
|---|---|---|
| 200/201 | Success | `{ ... }` |
| 400 | Validation failure | `{ "error": "...", "details": [...] }` |
| 401 | Unauthenticated | `{ "error": "Unauthorized" }` |
| 403 | Forbidden | `{ "error": "Forbidden" }` |
| 404 | Not found | `{ "error": "Not found" }` |
| 409 | Conflict | `{ "error": "..." }` |
| 422 | Business rule violation | `{ "error": "..." }` |
| 500 | Server error | `{ "error": "Internal server error" }` |
| 503 | Downstream unavailable | `{ "error": "..." }` |

(For method-scoped analysis, replace status codes with return
type and exception list.)

**Dependencies Called by This Target**

| # | Dependency | Type | Endpoint / Method Called | Purpose | Sync / Async |
|---|---|---|---|---|---|
| 1 | `<ServiceName>` | HTTP / gRPC / DB / Cache / Queue | `POST /path` | Why it's called | Sync |

**Special Notes**
- Idempotency guarantees (if any)
- Retry / circuit-breaker behaviour
- Caching (TTL, cache key strategy)
- Feature flags gating this target
- Conditional logic by user type, role, or entitlement
- Known limitations or TODOs

---

## 3. Data Models

List every model touched by the targets in scope.

### 3.x `<ModelName>`

| Field | Type | Required | Validation Rules | Description |
|---|---|---|---|---|
| `id` | UUID | Yes | — | Primary identifier |

---

## 4. Dependency Map (Recursive Tree)

Show the full call tree starting from the in-scope targets.

```
<SERVICE_NAME or TARGET>
├── [sync]  UserService  →  GET /users/{id}
│   └── [sync]  MongoDB  →  users collection
├── [sync]  TokenService  →  POST /token/validate
├── [async] NotificationQueue  →  publish: user.created
└── [sync]  ExternalService  →  POST /verify  (EXTERNAL)
```

---

## 5. Sequence Diagram

A Mermaid sequence diagram of the request flow for each endpoint/target
in scope. Produce **one diagram per endpoint** (or one per method for
method-scoped analysis). Use only participants and calls that were
actually traced in Section 2 and Section 4 — do not invent flows,
participants, or calls to make the diagram look complete.

Conventions:
- Solid arrow `->>` for synchronous requests
- Dashed arrow `-->>` for responses returned to the caller
- For asynchronous publishes, use `->>` and label the message with
  `(async)` plus the topic/queue name
- Mark external systems with an `(EXTERNAL)` note
- Show the happy path in the diagram; describe major error branches in
  a short bullet list beneath it

### 5.x Sequence — `<HTTP_METHOD> <PATH>` (or `<ClassName>.<methodName>`)

```mermaid
sequenceDiagram
    participant Client
    participant <ServiceName>
    participant <Downstream1>
    participant <Downstream2>

    Client->><ServiceName>: <HTTP_METHOD> <path>
    <ServiceName>->><Downstream1>: <call / query>
    <Downstream1>-->><ServiceName>: <response>
    <ServiceName>->><Downstream2>: <call> (async: <topic>)
    <ServiceName>-->>Client: <status + body>
```

**Error branches:**
- _(e.g., Downstream1 timeout → 503 returned to Client)_
- _(e.g., validation failure → 400 before any downstream call)_

If no downstream calls exist for a target, still show the
Client ↔ Service exchange, and note that the target is
self-contained (no downstream dependencies).

---

## 6. Communication Layer

| Dependency | Protocol | Auth Mechanism | Base URL / Config Key | Timeout | Retry Policy |
|---|---|---|---|---|---|

---

## 7. Configuration & Environment Variables

| Variable | Purpose | Default | Required |
|---|---|---|---|

---

## 8. Error Handling & Resilience Patterns

- Global error handler / middleware
- Circuit breakers, bulkheads, or fallbacks
- Dead-letter queue handling (if async)
- Logging and tracing conventions (correlation IDs, trace headers)

---

## 9. Security Considerations

- Authentication and authorisation model
- PII / sensitive fields — are they masked in logs?
- Input sanitisation approach
- Any known security TODOs in the code

---

## 10. Open Issues / TODOs

Scan for `TODO`, `FIXME`, `HACK`, `NOTE` comments in the in-scope code
and list them:

| Ref | File:Line | Comment | Severity |
|---|---|---|---|
