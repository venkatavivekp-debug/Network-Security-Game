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
  - A small `ConnectionSecurityService` evaluates session/device consistency
    against the last-seen fingerprint and emits a `SESSION_ANOMALY` audit
    event when the fingerprint changes mid-session.

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

## 5. Puzzle system (CPHS)

Each puzzle is bound to a `(message, receiver, generated-at)` tuple,
non-replayable, attempt-limited, and time-bounded.

- **Hash puzzle (POW)** — find a nonce so `SHA-256(challenge:nonce)` matches
  a target.
- **Arithmetic** — evaluate a generated expression.
- **Encoded** — base64 / simple cipher decode.
- **Pattern** — continue an arithmetic / geometric / Fibonacci-like sequence.

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

## 11. Limitations (honest)

- Puzzle types are intentionally **simplified** (POW, simple arithmetic,
  base64, basic sequences). They illustrate the gating mechanism, not
  full hardness arguments.
- The adaptive policy is **heuristic** — buckets of risk + threat level.
  There is no SPE / Nash solver behind it.
- The simulator is **abstracted**: graph attacks, budgets, and "user
  effort" are proxies. Good for relative comparisons inside the project,
  weak for absolute claims.
- "Static / Layered / Challenge / Adaptive" labels are mapping conventions
  for exposition, not citations of specific external systems.
- The connection security check uses an IP+User-Agent fingerprint as a
  weak signal; it is intentionally not bound into key derivation, so it
  can flag suspicious sessions without breaking the messaging flow when
  someone simply moves networks.
- Results above are one reproducible scenario, not a benchmark suite;
  different seeds, sizes, or strategies can shift which mode "wins".
