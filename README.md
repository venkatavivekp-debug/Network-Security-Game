# Network-Security-Game

Game-theoretic adaptive cybersecurity system for secure communication and recovery.

## 1. Core idea

- A **sender** picks a protection mode for each message.
- The **receiver** must pass a challenge to unlock it.
- The system **adapts** to user behavior and global threat signals.
- An **admin** supervises recovery without ever seeing plaintext.

The point is not just sending an encrypted message. It is treating the link
between sender, receiver, attacker, and admin as a small game whose state moves
through *attack → defense → recovery*.

## 2. Security modes

- **NORMAL** — AES-GCM. Baseline confidentiality.
- **SHCS** — packaging with hidden/committed metadata; layered posture.
- **CPHS** — receiver must solve a cryptographic challenge before the key is recoverable.
- **ADAPTIVE** — risk-driven: the engine may upgrade the requested mode (e.g. NORMAL → CPHS) or place a message on hold for admin review.

## 3. Puzzle system (CPHS)

Each puzzle is bound to a `(message, receiver, generated-at)` tuple,
non-replayable, attempt-limited, and time-bounded.

- **Hash puzzle** — find a nonce so `SHA-256(challenge:nonce)` matches a target.
- **Arithmetic challenge** — evaluate a generated math expression.
- **Encoded message** — base64 / simple cipher decode.
- **Pattern puzzle** — continue a numeric sequence (arithmetic / geometric / Fibonacci-like).

A correct answer derives the wrapping key; a wrong answer is recorded but never leaks anything about the plaintext.

## 4. Adaptive security

- **Risk score** in `[0, 1]` derived from puzzle pressure, login anomalies, fingerprint, and global threat level.
- **Levels**: `LOW`, `ELEVATED`, `HIGH`, `CRITICAL`.
- **Escalation rules**:
  - `ELEVATED` → prefer SHCS for NORMAL requests.
  - `HIGH` → enforce CPHS.
  - `CRITICAL` → enforce CPHS + temporary hold for admin review.
- **Behavior tracking**: per-user puzzle attempts, failures, average solve time, recovery events; bursts decay over time.

Every `MessageSendResponse` includes `riskScore`, `riskLevel`, `riskReasons`,
`recoveryState`, and (when relevant) `warningMessage`.

## 5. Recovery model

The recovery state machine has **no dead ends**:

```
NORMAL → CHALLENGE_REQUIRED → ESCALATED → HELD → ADMIN_REVIEW_REQUIRED
                                                   ↓
                              RECOVERY_IN_PROGRESS → RECOVERED
                                                   ↘ FAILED → (admin reset / re-issue)
```

Every blocked state has a path back: solve the puzzle, admin release, or
counter reset. Plaintext never crosses the admin layer.

## 6. Simulation + evaluation

A graph-based simulator runs an *attack → defense → recovery* loop on a
parametrized topology. Metrics:

- `compromiseRatio`
- `resilienceScore`
- `averageRecoveryTime`
- `userEffortScore`
- `attackSuccessRate`
- `falsePositiveRate` (admin-hold rate for ADAPTIVE)

Runs are **reproducible** when a `seed` is provided; otherwise a stable seed is
derived from scenario parameters. The harness is exposed via `GET /evaluation/compare`
and `GET /evaluation/analysis`.

## 7. Results

Reproducible run: `seed=20260424`, `numberOfRuns=30`, `numNodes=20`, `numEdges=35`,
`defenseStrategy=REDUNDANCY`.

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

- **CPHS** has the lowest compromise and highest resilience at every tested intensity, at the cost of high user effort.
- **SHCS** gives a small, consistent improvement over NORMAL with low effort.
- **NORMAL** degrades fastest under attack.
- **ADAPTIVE** stayed close to NORMAL on this seed; the harness exists exactly so this kind of result is honest, not hand-waved.
- The system maintained valid recovery paths under all conditions (no `FAILED` end-state without a re-entry route).

## 8. How to run

### Backend

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
# → http://localhost:8080
```

For a no-setup run with MySQL bundled, copy `.env.example` to `.env`, set two
base64 32-byte AES keys, and run `docker compose up --build`.

### Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

The frontend proxies `/api` to the backend; `npm run build` produces a static
bundle.

### Tests

```bash
cd backend && mvn -q test
cd frontend && npm run build
```

## 9. UI

The React frontend is themed as a cyber command center:

- **Sender / Receiver console** — pick a mode, pick a CPHS puzzle type, see the requested vs enforced mode, the risk meter, the recovery state, and the puzzle arena (per-type UI, timer bar, attempt dots, success/failure animations).
- **Simulation** — animated attack/defense/recovery battlefield with live KPIs and a system-pressure tile.
- **Admin SOC** — global threat slider, system-pressure card, network status map, live alert feed (critical events highlighted), held-messages and users-at-risk lists. Admin never sees plaintext.

## 10. Limitations (honest)

- Puzzle types are intentionally **simplified** (POW, simple arithmetic, base64, basic sequences). They illustrate the gating mechanism, not full hardness arguments.
- The adaptive policy is **heuristic** — buckets of risk + threat level. There is no SPE / Nash solver behind it.
- The simulator is **abstracted**: graph attacks, budgets, and "user effort" are proxies. Good for relative comparisons inside the project, weak for absolute claims.
- "Static / Layered / Challenge / Adaptive" labels are mapping conventions for exposition, not citations of specific external systems.
- Results above are one reproducible scenario, not a benchmark suite; different seeds, sizes, or strategies can shift which mode "wins".
