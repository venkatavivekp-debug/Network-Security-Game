# Network-Security-Game

Adaptive game-theoretic secure messaging and recovery system.

A sender and a receiver communicate through a small game-theoretic protocol.
The sender picks a protection mode. The receiver clears a security challenge to
unlock the message. The system adapts when behavior looks suspicious. An admin
supervises recovery without ever seeing plaintext.

## 1. What this is

This project takes the idea from the source report — modelling a secure
connection as an *attack → defense → recovery* game — and turns it into a
working full-stack system:

- A **Spring Boot** backend that holds the protocol, adaptive policy, audit
  trail, and recovery state machine.
- A **React** command-center frontend that surfaces the same state visually
  (security loadout, puzzle arena, threat panel, SOC).
- An evaluation harness that runs reproducible attack/defense/recovery
  scenarios and reports comparable metrics across modes.

It is a research-style sandbox, not a production secure-messenger. Limitations
are listed honestly at the end.

## 2. Security modes

- **NORMAL** — AES-GCM. Baseline confidentiality.
- **SHCS** — Self-Healing Cipher: layered packaging with hidden/committed
  metadata; the adaptive engine can rotate posture under pressure.
- **CPHS** — Challenge-Protected Hidden Seal: the receiver must solve a
  cryptographic challenge before the wrapping key is recoverable.
- **ADAPTIVE** — risk-driven: the engine may upgrade the requested mode
  (e.g. NORMAL → CPHS) or place a message on hold for admin review.

## 3. Secure connection flow

```
sender ──► /message/send ──► adaptive engine ──► encrypted payload + recovery plan
                                  │
                                  ├─ risk LOW         → enforce requested mode
                                  ├─ risk ELEVATED    → step up to SHCS / CPHS
                                  └─ risk CRITICAL    → CPHS + HOLD (admin review)

receiver ──► /puzzle/challenge ──► CPHS challenge ──► /puzzle/solve ──► /message/decrypt
                                                            │
                                                            └─ failure burst → HELD

admin    ──► /admin/release ──► RECOVERY_IN_PROGRESS ──► RECOVERED
admin    ──► /admin/reset-failures (no plaintext access ever)
```

Every send returns its current `connectionSecurityState`, `recoveryState`,
`recoverySummary`, and `recoveryNextSteps`. Every recovery state has an
explicit next step — no dead ends.

## 4. OWASP-inspired hardening

Applied where it actually fits the secure-connection model. Not generic
boilerplate.

- **Session security**
  - Session ID is regenerated after a successful login (defense against
    session fixation), and an `AUTH_LOGIN_SUCCESS` + `SESSION_REGENERATED`
    audit pair is recorded.
  - Cookies are `HttpOnly`, `SameSite=Lax`, `Secure` in the docker profile,
    with a 30-minute idle timeout.
  - Logout invalidates the HTTP session and clears `NSG_SESSION` /
    `JSESSIONID` cookies. A JSON `POST /auth/logout` is also exposed.
  - `ConnectionSecurityService` keeps a layered, hashed fingerprint per user
    (IP, User-Agent, Accept-Language, session ID, plus first/last-seen
    timestamps and an anomaly counter). It emits one of four states per
    request — `FIRST_SEEN`, `STABLE`, `SHIFTED` (one signal changed),
    `ANOMALOUS` (multiple shifts or repeated drift). Anomalous states are
    audited as `SESSION_ANOMALY` and fed into the adaptive engine; users
    are never auto-blocked from this signal alone.

- **Adaptive verification (defense in depth, not lockout)**
  - Risk-based step-up: elevated risk requires SHCS/CPHS; critical risk
    triggers a temporary admin-supervised hold.
  - Repeated wrong puzzle answers escalate, but a single anomaly never
    permanently locks a user — admin reset and re-issuance always exist.

- **Rate limiting and brute-force protection**
  - Token-bucket rate limits on `POST /auth/login`, `POST /auth/register`,
    `POST /message/send`, `POST /puzzle/solve/*`, and `POST /admin/*`.
  - Returns clean HTTP `429` with `Retry-After` header and a `retry-after-
    seconds` hint in the JSON body. Each block emits a `RATE_LIMIT_BLOCKED`
    audit event.

- **Crypto / key hygiene**
  - No default production secrets. The startup validator fails fast outside
    `dev` if `APP_CRYPTO_MASTER_KEY` / `APP_CRYPTO_SHCS_KEY` are missing.
  - `.env` is git-ignored; only `.env.example` is checked in, and it
    contains placeholders, not keys.
  - Plaintext is never persisted — only AES-GCM ciphertext, IV, and
    SHCS/CPHS metadata land in the database.
  - Key rotation is intentionally a re-deploy concern: rotate the two AES
    keys and existing rows simply require re-encryption (or re-issue), there
    is no "wrap-around" decrypt path.

- **Access control**
  - `SENDER` cannot decrypt or read receiver-only data. `RECEIVER` cannot
    access another receiver's messages. `ADMIN` can hold/release/reset but
    cannot decrypt — there is no admin code path that returns plaintext.
  - Tested in `MessageAccessControlTest` (sender→decrypt blocked,
    receiver→other-inbox blocked, HELD→decrypt blocked) and
    `AdminControllerTest` (held-message endpoint never exposes content).

- **Error handling**
  - Spring `server.error.include-stacktrace=never`,
    `include-message=never`, `include-exception=false`. The frontend renders
    the JSON `ApiErrorResponse.details` array, not raw stack traces.

## 4a. Connection security model

The fingerprint is **opaque** (each signal is hashed) and **decoupled from
key derivation**, so a network move does not break decryption. It is used
as adaptive risk input and audit signal only.

| Signal | Source | Stored as |
|---|---|---|
| IP | `X-Forwarded-For` / `RemoteAddr` | SHA-256 hash |
| User-Agent | request header | SHA-256 hash |
| Accept-Language | request header | SHA-256 hash (optional) |
| Session ID | servlet `HttpSession` | SHA-256 hash |
| `firstSeen` / `lastSeen` | server clock | timestamps |
| `anomalyCount` | derived | integer |

| State | Trigger | Effect |
|---|---|---|
| `FIRST_SEEN` | first request for the user | seeded only, no risk added |
| `STABLE` | all signals match | no extra risk |
| `SHIFTED` | exactly one signal changed | small adaptive bump, audit |
| `ANOMALOUS` | multiple signals changed or repeated shifts | `SESSION_ANOMALY` audit; adaptive engine may step `NORMAL → SHCS` |

## 4b. Admin step-up protection

Admin role alone is not enough for sensitive actions. After login, the
admin must additionally **confirm their password** to mint a 5-minute
step-up token. The token is required for:

- `POST /admin/hold-message`, `POST /admin/release-message`
- `POST /admin/reset-failures`, `POST /admin/lock-user`, `POST /admin/unlock-user`
- `POST /admin/threat-level`

Read-only dashboards (`GET /admin/held-messages`, `GET /admin/audit/*`,
`GET /admin/users-at-risk`, `GET /admin/recovery-policy`,
`GET /admin/system-pressure`, `GET /admin/risk-policy`) **do not** require
step-up.

API:

```
POST /admin/confirm-action     { "password": "..." }   → { token, expiresAt, ttlSeconds }
GET  /admin/confirmation-status                        → { active, expiresAt, ttlSeconds }
```

The token is presented on subsequent calls via the `X-Admin-Confirm` header.
The frontend opens a small password modal automatically when a sensitive
action returns `403 Admin step-up required`, and retries the action after
confirmation. Plaintext passwords are never echoed back.

## 4c. Rate limiting deployment note

Rate limiting is split into a **service** (`RateLimiterService`) and a
**backend** (`RateLimiterBackend` interface).

| Profile | Backend | Notes |
|---|---|---|
| local / dev | `InMemoryRateLimiterBackend` (default, `@Primary`) | per-process token buckets, no setup |
| production | swap to a shared store (e.g. Redis) | implement `RateLimiterBackend` and register it as a Spring bean; `app.ratelimit.backend=memory` switches the in-memory one off |

Behaviour at the edge does not change — the same `429` response, the same
`Retry-After` header, and the same `RATE_LIMIT_BLOCKED` audit event are
emitted regardless of backend.

## 4d. Adaptive policy (rule table, not a solver)

`AdaptiveRiskPolicyService` exposes the rule table at `GET /admin/risk-policy`
so the SOC console can render exact thresholds, weights, and limitations.

| Risk level | Score range | Action |
|---|---|---|
| `LOW` | `< low` | enforce requested mode |
| `ELEVATED` | `[low, high)` | step `NORMAL → SHCS`, keep `SHCS`/`CPHS` as requested |
| `HIGH` | `[high, critical)` | enforce `CPHS`, harder puzzle |
| `CRITICAL` | `≥ critical` | enforce `CPHS` and `HOLD` for admin review |

Risk score `= clamp(Σ wᵢ · signalᵢ, 0, 1)` where `signalᵢ` is the live value
of `pressure`, `consecutive_failures`, `failure_rate`,
`unstable_solve_time`, `fingerprint_changed`, `connection_shifted`,
`connection_anomalous`, etc. Exact weights and thresholds live in
`application.yml` under `app.adaptive.*` and are echoed by the
`/admin/risk-policy` endpoint. This is a **transparent heuristic**, not an
SPE/Nash solver.

## 5. Puzzle system (CPHS)

The puzzle layer **demonstrates CPHS gating and adaptive challenge cost**.
It is intentionally pedagogical — not a hardness claim. The frontend
labels all four types as *security challenges* for clarity.

Each puzzle is bound to a `(message, receiver, generated-at)` tuple,
non-replayable, attempt-limited, and time-bounded.

- **Hash proof (POW)** — find a nonce so `SHA-256(challenge:nonce)` matches
  a target. Difficulty (max iterations) scales with adaptive risk.
- **Arithmetic** — evaluate a small expression with operator precedence;
  submit the integer.
- **Encoded** — base64-encoded short phrase; submit the original text
  (case-insensitive).
- **Pattern** — continue a numeric sequence (arithmetic, geometric, or
  Fibonacci-like) and submit the next value.

A correct answer derives the wrapping key. A wrong answer is recorded but
never leaks anything about the plaintext.

## 6. Adaptive learning

- **Risk score** in `[0, 1]` from puzzle pressure, login anomalies,
  fingerprint changes, and the global threat level.
- **Levels**: `LOW`, `ELEVATED`, `HIGH`, `CRITICAL`.
- **Escalation rules**:
  - `ELEVATED` → prefer SHCS for NORMAL requests.
  - `HIGH` → enforce CPHS.
  - `CRITICAL` → enforce CPHS + temporary hold for admin review.
- **Behavior tracking**: per-user puzzle attempts, failures, average solve
  time, recovery events; bursts decay over time so a single bad session
  cannot poison the user's profile forever.

## 7. Admin-supervised recovery

The recovery state machine has **no dead ends**:

```
NORMAL → CHALLENGE_REQUIRED → ESCALATED → HELD → ADMIN_REVIEW_REQUIRED
                                                   ↓
                              RECOVERY_IN_PROGRESS → RECOVERED
                                                   ↘ FAILED → (admin reset / re-issue)
```

Admin powers:

- **Hold / release** a message (no plaintext access).
- **Reset puzzle failure counters** for a user.
- **View** the live alert feed, the suspicious-session feed
  (`SESSION_ANOMALY`, `RATE_LIMIT_BLOCKED`, `AUTH_ACCOUNT_LOCKED`,
  failed logins), the held-messages list, and the recovery playbook
  (every `RecoveryState` and its explicit next step).

## 8. Evaluation results

Reproducible run: `seed=20260424`, `numberOfRuns=30`, `numNodes=20`,
`numEdges=35`, `defenseStrategy=REDUNDANCY`.

| attackIntensity | mode | compromiseRatio | resilienceScore | recoveryTime | userEffort |
|---:|---|---:|---:|---:|---:|
| 0.20 | NORMAL   | 0.114 | 0.728 | 11.84 | 0.000 |
| 0.20 | SHCS     | 0.087 | 0.792 | 10.02 | 0.150 |
| 0.20 | CPHS     | 0.029 | 0.908 |  7.23 | 14.33 |
| 0.20 | ADAPTIVE | 0.111 | 0.736 | 11.77 | 0.000 |
| 0.50 | NORMAL   | 0.162 | 0.639 | 16.20 | 0.000 |
| 0.50 | SHCS     | 0.150 | 0.660 | 15.14 | 0.150 |
| 0.50 | CPHS     | 0.043 | 0.842 | 10.59 | 14.33 |
| 0.50 | ADAPTIVE | 0.177 | 0.631 | 16.53 | 0.000 |
| 0.85 | NORMAL   | 0.215 | 0.582 | 20.37 | 0.000 |
| 0.85 | SHCS     | 0.173 | 0.636 | 18.16 | 0.150 |
| 0.85 | CPHS     | 0.065 | 0.782 | 14.36 | 14.33 |
| 0.85 | ADAPTIVE | 0.240 | 0.540 | 21.45 | 0.000 |

What the run shows:

- **CPHS** has the lowest compromise and highest resilience at every tested
  intensity, at the cost of high user effort.
- **SHCS** gives a small, consistent improvement over NORMAL with low
  effort.
- **NORMAL** degrades fastest under attack.
- **ADAPTIVE** stayed close to NORMAL on this seed; the harness exists
  exactly so this kind of result is honest, not hand-waved.
- The system maintained valid recovery paths under all conditions (no
  `FAILED` end-state without a re-entry route).

## 9. How to run

### Backend

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
# → http://localhost:8080
```

For a no-setup run with MySQL bundled, copy `backend/.env.example` to
`backend/.env`, set two base64 32-byte AES keys
(`openssl rand -base64 32`), and run `docker compose up --build`. The
docker profile enforces `Secure` cookies; for local HTTP development the
default profile keeps `Secure=false` so the session cookie still flows.

### Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

The frontend proxies `/api` to the backend; `npm run build` produces a
static bundle.

### Tests

```bash
cd backend  && mvn -q test
cd frontend && npm run build
```

The backend test pack includes access-control checks (sender cannot
decrypt, cross-receiver access blocked, HELD messages cannot decrypt),
admin-plaintext checks (no admin endpoint returns plaintext), rate-limit
behavior (HTTP 429 + `Retry-After` + `RATE_LIMIT_BLOCKED` audit), and
connection-security evaluation (`STABLE` / `FIRST_SEEN` / `ANOMALOUS`).

## 10. UI at a glance

The React frontend is themed as a dark cyber command center:

- **Sender console** — security loadout chip group (mode + puzzle + tier),
  requested vs enforced mode shown side by side with a step-up indicator,
  risk reasons after send, and a recovery plan card with concrete next
  steps.
- **Receiver console** — challenge arena per puzzle type, timer bar,
  attempt dots, success/failure animations, recovery plan after a failed
  burst or admin hold.
- **Admin SOC** — global threat slider, system-pressure card, network
  status map, live alert feed, suspicious-session feed, held-messages
  cards (release in one click), users-at-risk cards (reset counters),
  and the recovery playbook (every state + its next step). Plaintext is
  never rendered.
- **Battlefield simulation** — animated attack/defense/recovery loop with
  live KPIs and a system-pressure tile bound to the backend.

## 10a. External threat protection (OWASP-aligned controls)

These controls are aimed at outside web/API threats — what the system does
when an unauthenticated or low-privilege actor pokes at the edge. The design
goal is *engineering value*, not OWASP marketing: every row below points to
real code and at least one test.

### Control summary

| Concern | Control | Where to look |
|---|---|---|
| Object-level authorization | `AccessPolicyService` (participant-scoped lookups) | `security/AccessPolicyService.java`, `MessageController#getById`, `AttackController#simulate` |
| Object-property exposure | Safe DTOs + redacted `metadata` (no `wrappedKey` / `targetHash` / `challenge` to senders) | `MessageService#redactMetadata`, `dto/ResponseExposureTest` |
| Input validation | `@Valid` DTOs + bounded `@Min/@Max/@Pattern`, clean 400s for malformed JSON | `dto/*Request.java`, `GlobalExceptionHandler` |
| CSRF (compensating control) | `X-Requested-With: XMLHttpRequest` required on every mutating call; `SameSite=Lax` cookies; locked-down CORS | `security/CustomHeaderCsrfFilter.java` |
| Security headers | CSP, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, `Cache-Control: no-store` for sensitive paths | `security/SecurityHeadersFilter.java` |
| CORS | Strict allow-list via `app.security.cors.allowed-origins`; never wildcard with credentials | `security/SecurityConfig#corsConfigurationSource` |
| Rate limiting | Token-bucket on login / register / send / puzzle solve / admin actions; clean 429 + `Retry-After` | `security/ratelimit/RateLimitFilter.java` |
| Body / payload bounds | `spring.servlet.multipart.max-request-size=2MB`; DTO `@Size`; simulation/eval `@Min`/`@Max` | `application.properties`, `dto/SimulationRunRequest.java` |
| Audit & monitoring | `FORBIDDEN_ACCESS`, `VALIDATION_REJECTED`, `RATE_LIMIT_BLOCKED`, `SESSION_ANOMALY` recorded with hashed IP/UA | `audit/AuditEventType.java`, `GlobalExceptionHandler`, `ApiAccessDeniedHandler` |
| Error leakage | `server.error.include-stacktrace=never`, `include-message=never`; `ApiErrorResponse.details` only | `application.properties`, `GlobalExceptionHandler` |
| External-threat SOC card | `/admin/external-threats` (counts + recent slice) + frontend `ExternalThreatPanel` | `security/ExternalThreatSummaryService.java`, `components/cyber/ExternalThreatPanel.tsx` |

### OWASP Top 10 mapping

| Risk | What it means here | Control | Test |
|---|---|---|---|
| **A01 Broken Access Control** | Sender / receiver / admin should each see only what they're authorized to. | `@PreAuthorize` + `AccessPolicyService` participant scope; admin-step-up for sensitive admin actions | `MessageAccessControlTest`, `AccessPolicyServiceTest`, `AdminControllerTest` |
| **A02 Cryptographic Failures** | Plaintext leaks; weak crypto; hard-coded keys. | AES-GCM with random IV; AES-256 keys from env; no plaintext persisted; CPHS metadata stays server-side | `MessageAccessControlTest` (admin never sees plaintext) |
| **A03 Injection** | Hostile JSON / params crashing parsers or running SQL. | Bean Validation everywhere; JPA repositories only; clean 400 for `HttpMessageNotReadableException` | `GlobalExceptionHandlerTest` |
| **A04 Insecure Design** | No recovery path; silent failures; flag-only "lock the user". | `RecoveryPolicyService` covers every `RecoveryState`; no dead ends; risk levels documented at `/admin/risk-policy` | `MessageAccessControlTest#recoveryPolicyServiceCoversEveryRecoveryStateWithoutDeadEnds` |
| **A05 Security Misconfiguration** | Wildcards, default secrets, error leakage. | Strict CORS allow-list; CSP/headers filter; `include-stacktrace=never`; missing crypto keys fail-fast outside dev | `SecurityHeadersFilterTest`, env-validator startup check |
| **A07 Identification and Authentication Failures** | Session fixation, credential stuffing. | Session ID rotation on login; SameSite cookies; rate-limit on `/auth/login` and `/auth/register`; admin step-up | `RateLimitFilterTest`, `AdminStepUpServiceTest` |
| **A09 Security Logging and Monitoring Failures** | Silent abuse. | All forbidden / validation / 429 / session anomaly events emit hashed-context audit records, surfaced in `/admin/external-threats` | `ExternalThreatSummaryServiceTest`, `GlobalExceptionHandlerTest` |

### OWASP API Security mapping

| Risk | Control | Test |
|---|---|---|
| **API1 Broken Object Level Authorization** | Participant-scoped lookups for `/message/{id}`, `/attack/simulate/{id}`, `/simulation/run` (when `messageId` is provided). Cross-receiver / cross-sender ids surface the same NotFound shape. | `AccessPolicyServiceTest#requireParticipantThrowsNotFoundForCrossReceiver`, `MessageAccessControlTest` |
| **API2 Broken Authentication** | BCrypt, session ID rotation, login rate-limit, admin step-up | `RateLimitFilterTest`, `AdminStepUpServiceTest` |
| **API3 Broken Object Property Level Authorization** | `MessageSummaryResponse` strips `wrappedKey`/`targetHash`/`challenge` from metadata; admin held-messages list only shows safe metadata | `ResponseExposureTest`, `AdminControllerTest` |
| **API4 Unrestricted Resource Consumption** | DTO bounds + Spring multipart caps + rate-limit buckets; clean 429 with `Retry-After` | `RateLimitFilterTest`, simulation/eval DTO `@Max` |
| **API5 Broken Function Level Authorization** | Method security + admin step-up + custom-header CSRF gate | `AdminControllerTest`, `CustomHeaderCsrfFilterTest` |

### CSRF posture (Option B, documented)

We deliberately keep the API stateless rather than threading a Spring CSRF
token through the React client. The compensating controls are:

1. `X-Requested-With: XMLHttpRequest` required on every mutating request
   (`CustomHeaderCsrfFilter`). Browsers won't let cross-origin HTML forms
   set this header without a preflight.
2. `SameSite=Lax` session cookie (configurable via `APP_COOKIE_SAME_SITE`).
3. Strict CORS allow-list — explicit origins, never `*` with credentials.
4. JSON-only mutating endpoints (`Content-Type: application/json`).
5. Admin step-up for the few mutating actions that matter most.

Honest limitation: this stack is solid against the common CSRF / cross-site
HTML form vector. It is not a substitute for a full anti-CSRF token if you
later host browser content from multiple origins under the same backend.

### CORS posture

`SecurityConfig#corsConfigurationSource` reads `app.security.cors.allowed-origins`
(comma-separated). Defaults are dev-only (`http://localhost:5173`, etc.).
Production deployments override `APP_CORS_ALLOWED_ORIGINS` to the public
frontend origin(s); credentials are allowed but the origin list is never
wildcarded.

### What this does NOT do (honest)

- No WAF; we recommend fronting with one in production.
- The custom-header CSRF gate stops cross-origin HTML POSTs but does **not**
  defend against an attacker who already has cross-site script execution on
  the frontend origin (XSS). The CSP narrows that surface but does not
  eliminate it.
- Audit logs are stored in the same database as the application data; in a
  real deployment they should fan out to an append-only sink (see §11).

## 11. Production hardening recommendations

If you wanted to take this beyond a research sandbox:

- Run behind TLS-terminating reverse proxy and enable `Secure` cookies
  (the docker profile already does).
- Replace `InMemoryRateLimiterBackend` with a Redis (or equivalent shared)
  backend so per-process buckets become per-cluster buckets.
- Move audit events into an append-only sink (Loki, OpenSearch, S3) and
  retain the database table only as a hot cache.
- Add a real second factor (TOTP/WebAuthn) for admin step-up; the current
  password re-check is the minimum bar, not the ceiling.
- Rotate `APP_CRYPTO_MASTER_KEY` / `APP_CRYPTO_SHCS_KEY` on a schedule
  and re-encrypt rows with a background job (the schema is intentionally
  re-encryptable; there is no wrap-around decrypt path).
- Front the API with a WAF for L7 abuse, and feed `RATE_LIMIT_BLOCKED` /
  `SESSION_ANOMALY` audit events into your SIEM.

## 12. Limitations (honest)

- Puzzle types are intentionally **simplified** (POW, simple arithmetic,
  base64, basic sequences). They illustrate CPHS gating and adaptive
  challenge cost — they are **not** a hardness argument and the project
  does not claim cryptographic puzzle hardness.
- The adaptive policy is a **transparent heuristic** — weighted signals
  + thresholds, exposed at `/admin/risk-policy`. There is no SPE / Nash
  solver behind it.
- The simulator is **abstracted**: graph attacks, budgets, and "user
  effort" are proxies. Good for relative comparisons inside the project,
  weak for absolute claims.
- "Static / Layered / Challenge / Adaptive" labels are mapping conventions
  for exposition, not citations of specific external systems.
- The connection fingerprint is a **layered but still soft** signal
  (hashed IP + User-Agent + Accept-Language + session ID). It is
  intentionally not bound into key derivation, so it can flag suspicious
  sessions without breaking the messaging flow when someone simply moves
  networks. NAT, browser updates, and travel can produce `SHIFTED` /
  `ANOMALOUS` states without an actual attacker.
- Admin step-up is a password re-check token (5-minute TTL). It is a
  meaningful uplift over role-only protection, but it is not WebAuthn /
  hardware-key strength.
- Rate limiting in the default profile is **per-process**, so a horizontally
  scaled deployment must wire in a shared backend (see §4c).
- Results above are one reproducible scenario, not a benchmark suite;
  different seeds, sizes, or strategies can shift which mode "wins".
