# Game-Theoretic Framework for Resilient Network Security and Recovery

This system is a secure simulation platform for resilient message security, adversarial disruption analysis, and network recovery evaluation with validated APIs, controlled workflows, and **explicit seeding semantics** for repeatable experiments.

## Core Capabilities
- Secure user authentication
- Validated message workflows
- Controlled simulation execution
- Exportable results
- Reproducible demo via seeding
- Adaptive security escalation (risk-aware message protection)
- Persisted CPHS puzzle gating with abuse protection
- Admin supervision and recovery controls (without plaintext access)

## Quick Demo Flow (2 minutes)
1. Configure crypto keys for Docker (required):
   - `cd backend`
   - `cp .env.example .env`
   - Set `APP_CRYPTO_MASTER_KEY` and `APP_CRYPTO_SHCS_KEY` to two different **base64-encoded 32-byte** AES keys (see comments in `.env.example`)
2. Start the stack:
   - `docker compose up --build`
3. Seed data auto-loads (enabled by default in `docker-compose.yml` with `APP_SEED_DEMO_DATA=true`).
4. Login with demo credentials:
   - sender: `demo_sender` / `DemoSender#123`
   - receiver: `demo_receiver` / `DemoReceiver#123`
5. Send a message from `/send` (choose NORMAL, SHCS, or CPHS).
6. Run a simulation from `/simulation/dashboard`.
7. View saved runs at `/simulation/history-page`.
8. Export CSV from `/simulation/export` (or from dashboard/history links).

## Primary UI routes (Thymeleaf)
These are the main browser pages used in demos (all require authentication except `/login`):

- **Access / registration**: `/login` (sign-in + account creation on the same page)
- **Secure messaging**
  - **Send**: `/send` (SENDER role)
  - **Receive / decrypt**: `/receive` (RECEIVER role)
- **Base simulation**
  - **Run + charts**: `/simulation/dashboard`
  - **NORMAL vs SHCS vs CPHS compare**: `/simulation/compare-page`
  - **History + filters**: `/simulation/history-page`
  - **Run detail**: `/simulation/history-page/{id}`
  - **CSV export**: `/simulation/export`
- **Advanced simulation**
  - **Run + charts + round table**: `/simulation/advanced-dashboard`
- **Evaluation framework**
  - **Run + charts + recent runs preview**: `/simulation/evaluation-dashboard`
  - **Benchmark comparisons / sweeps**: `/simulation/evaluation-compare`
  - **History + filters**: `/simulation/evaluation-history`
  - **Run detail**: `/simulation/evaluation-history/{id}`
  - **CSV export**: `/simulation/evaluate/export`

### Demo walkthrough (UI-first)
1. Open `/login`, register two accounts (one **SENDER**, one **RECEIVER**) or use the Docker demo users.
2. As sender, open `/send`, pick **NORMAL / SHCS / CPHS**, send to the receiver username.
3. As receiver, open `/receive`, decrypt a message, and note algorithm-specific outputs.
4. Open `/simulation/dashboard`, run a base scenario, then open **Base history** from the header and inspect a saved **detail** row.
5. Open `/simulation/compare-page` to generate the three-way comparison table + charts.
6. Open `/simulation/advanced-dashboard`, run a multi-round scenario (optionally enable MTD/deception), then inspect the timeline + per-round table.
7. Open `/simulation/evaluation-dashboard`, run a repeated evaluation (start with **VARIED** + `repetitions > 1`), then try **FIXED** with `repetitions = 1` + a **base seed** to see the enforced semantics reflected in validation messages.
8. Open `/simulation/evaluation-compare` for benchmark-style comparisons, then browse `/simulation/evaluation-history` for persisted runs.

### UI notes / limitations
- **Charts load from a CDN** (`chart.js`); if the browser cannot reach the CDN, charts degrade to an inline notice while tables still show the numeric results.
- **Role-gated routes** (send vs receive) will redirect to login/forbidden if accessed with the wrong role—this is expected.

## System Guarantees
- All inputs validated at API and service layer
- Structured error handling across all endpoints
- Authentication and access control enforced
- No silent failures
- **Adaptive enforcement is bounded and recoverable** (step-up verification before hard denial)
- **Reproducible simulations when seeds are provided** (advanced + evaluation); otherwise the system derives stable default seeds from scenario parameters (see “Reproducibility semantics”)

## Adaptive Security Engine (research upgrade)
This platform models secure communication as a staged attacker/defender/recovery game. The **Adaptive Security Engine** operationalizes that idea by:
- computing **risk assessments** from user behavior + system threat signals
- enforcing **step-up security** (NORMAL → SHCS → CPHS) when required
- applying **temporary communication holds** for admin-supervised recovery under critical risk
- emitting **receiver-visible warning signals** without exposing plaintext

### Risk scoring (auditable, non-magical)
At key control points (login, message send, puzzle solve), the engine derives:
\(riskScore \in [0,1]\) and a discrete `riskLevel` (`LOW`, `ELEVATED`, `HIGH`, `CRITICAL`).

Signals used include:
- failed login attempts
- puzzle attempt pressure / failures
- unusual login time (relative to prior login)
- unfamiliar fingerprint (IP + User-Agent hash)
- global threat intensity (operator-set; can be driven by simulation/telemetry)

### Adaptive mode enforcement (POST /message/send)
The sender may request a mode (`NORMAL`, `SHCS`, `CPHS`). The server may enforce a stronger posture:
- **LOW**: honor requested mode
- **ELEVATED**: prefer SHCS when threat/risk suggests step-up
- **HIGH**: enforce CPHS
- **CRITICAL**: enforce CPHS and place message on **temporary hold** for admin review

The response includes:
- `requestedAlgorithmType`
- `effectiveAlgorithmType`
- `escalated` + `escalationReason`
- `riskScore` + `riskLevel`
- `communicationHold` when admin-supervised recovery is required

### CPHS puzzle escalation (bounded difficulty)
CPHS puzzle parameters adapt to risk + threat level:
- `maxIterations` increases with risk (bounded by server max)
- `attemptsAllowed` decreases modestly at high risk (never below 1)
- `expiresAt` shortens at higher risk (never below 60s)

This balances security with operational continuity: puzzles remain solvable for legitimate users.

### Abuse protection / rate limiting
To protect against brute force and flooding, the backend applies server-side rate limiting (token bucket) on:
- `POST /auth/login`
- `POST /message/send`
- `POST /puzzle/solve/{messageId}`
- sensitive admin actions (`/admin/*`)

On limit exceed, the API returns `429` with `Retry-After` guidance.

### Receiver warning signals
Receivers are informed safely via message metadata fields:
- `riskLevel` / `riskScore` at send time
- warning strings for **high-risk** sessions
- `HELD` status indicating admin-supervised recovery is required

### Admin supervision layer (no plaintext access)
Admins can:
- view risk status (`POST /admin/risk-score`)
- view users currently flagged by the behaviour engine (`GET /admin/users-at-risk`)
- view messages held by the system (`GET /admin/held-messages`)
- view recent audit events (`GET /admin/audit/recent`)
- set the global threat level (`GET/POST /admin/threat-level`)
- place/release messages on hold (`POST /admin/hold-message`, `POST /admin/release-message`)
- lock/unlock users (`POST /admin/lock-user`, `POST /admin/unlock-user`)
- reset puzzle-failure counters after review (`POST /admin/reset-failures`)

Admins **cannot** decrypt messages, retrieve plaintext, or bypass cryptographic gating. The held-messages endpoint deliberately returns metadata only (sender, receiver, mode, risk, hold reason, recovery state) — never ciphertext or plaintext. There is a unit test in `AdminControllerTest` that asserts this property.

### Per-user behaviour profile and recovery state machine
Two pieces sit between "the user did something" and "the engine reacts":

1. **`UserBehaviorProfile`** (`user_behavior_profiles` table) records, per user, puzzle attempts, successes, failures, consecutive recent failures, average solve time and recovery events. Successive failures push the burst counter up; quiet periods decay it. Successful solves shrink the burst and update the moving-average solve time. Admin "reset failures" or release-from-hold both record an explicit recovery event so the audit trail has the full story.
2. **`RecoveryState`** is reported on every locked or held message so the UI and admin can reason about state without seeing plaintext. The states are `NORMAL`, `CHALLENGE_REQUIRED`, `ESCALATED`, `HELD`, `ADMIN_REVIEW_REQUIRED`, `RECOVERY_IN_PROGRESS`, `RECOVERED`, `FAILED`. There is no dead-end: every blocked state has a path back via puzzle solve, admin release, or counter reset.

Send-message responses now include `riskScore`, `riskLevel`, `riskReasons`, `recoveryState`, `adminReviewRequired`, and a human-readable `warningMessage`. Inbox summaries include the same fields so the receiver UI can show risk badges, recovery hints, and a puzzle-solve panel without ever pulling plaintext.

### Frontend cybersecurity console
The React frontend ships three role-aware consoles on top of the existing simulation/evaluation pages:

- **Sender Command Console** (`/messaging`, SENDER role) — pick mode `NORMAL`/`SHCS`/`CPHS`, pick a CPHS **puzzle type**, see the requested vs enforced mode, the risk badge, a live risk meter, the recovery state, and the human warning message in the response card.
- **Receiver Challenge Console** (`/messaging`, RECEIVER role) — inbox with risk and recovery badges per message, a per-type **Puzzle Arena** with timer, attempt indicator dots, success/failure animations, and a Decrypt button gated on the puzzle being solved.
- **Admin SOC** (`/admin`, ADMIN role) — global threat-level slider, system-pressure snapshot, live alert feed (auto-refresh every 6 s), held-messages list with one-click release, users-at-risk list with reset action, and the full recent audit log. Plaintext is never fetched.

### CPHS multi-type puzzle engine (research extension)
The CPHS gate now supports four challenge types. Each puzzle is stored with a `puzzleType` discriminator, tied to (message, receiver, generation timestamp), is non-replayable, and is attempt-limited:

| Type            | Receiver task                                                  | Key recovery (server-side)                                       |
|-----------------|----------------------------------------------------------------|------------------------------------------------------------------|
| `POW_SHA256`    | Find a nonce `n` such that `SHA-256(challenge + ":" + n) == targetHash`. Runs locally in the browser. | Recovers AES message key by XOR-unwrapping with `H(challenge + ":" + n + ":" + salt)`. |
| `ARITHMETIC`    | Evaluate the displayed math expression and submit the integer result. | Canonical answer is hashed with the salt, used as the unwrap key. |
| `ENCODED`       | Decode a base64-encoded phrase and submit it.                  | Canonicalized phrase (`trim` + `lowercase` + collapsed spaces) is hashed with the salt. |
| `PATTERN`       | Continue the numeric sequence (arithmetic, geometric, or Fibonacci-like) and submit the next value. | Same hash-then-XOR mechanism as the other answer-based types.    |

The engine layout:

- `backend.crypto.PuzzleEngine` is the strategy interface (`generate`, `solve`, `questionText`).
- `backend.crypto.PuzzleEngineRegistry` indexes Spring-discovered engines by `PuzzleType`.
- `backend.crypto.AnswerKeyDerivation` is the shared helper that turns canonical answers into a wrapping key, mirroring the cryptographic shape used by the existing PoW puzzle.
- `backend.service.MessagePuzzleService` dispatches via the registry: it stores `recoveredKey` for non-PoW types after a successful solve, so subsequent decrypt calls do not need the answer again. Failed attempts are persisted, audited, and (when attempts are exhausted) push the message into `HELD` and raise the system-wide attack intensity slightly.
- `backend.service.CPHSService` calls into the registry on send and exposes a `decryptWithRecoveredKey` path next to the legacy nonce-driven path.

The frontend `PuzzleArena` (`frontend/src/components/cyber/PuzzleArena.tsx`) renders a different UI for every type: nonce search bar with progress + browser-side SHA-256 for `POW_SHA256`, integer answer field for `ARITHMETIC`/`PATTERN`, free-text decode field for `ENCODED`. Every variant shows the same chrome — challenge text, attempt dots, expiry timer (warn under 90 s, critical under 30 s), success/failure animations.

Tests covering this surface:

- `backend.crypto.PuzzleEngineTest` exercises each engine in isolation (round-trip key recovery, wrong-answer rejection, and registry coverage for every `PuzzleType`).
- `backend.crypto.CphsMultiPuzzleIntegrationTest` calls `CPHSService.encryptWithPuzzle` for every type, parses the metadata the way `MessageService` does, runs the engine `solve`, decrypts the AES-GCM ciphertext, and asserts the original plaintext comes back.

### Unified UI system: cyber command center
The frontend pulls toward a single cyber-defense aesthetic instead of plain forms:

- **Network visualization** (`components/cyber/NetworkViz.tsx`) — small SVG that renders nodes/edges with `stable / compromised / recovered` palette and a legend. The big animated battlefield on `/simulation` keeps the original canvas-based renderer with its grid, edge disruption, and ring layout (`pages/CommandCenterPage.tsx`).
- **Attack/Defense/Recovery timeline** (`components/cyber/AttackTimeline.tsx`) plus the existing `cc-stepper` chips on the simulation page expose phase progression with a phase-tinted active step.
- **Puzzle game screen** (`PuzzleArena.tsx`) — timer pill, progress bar (only shown for the PoW search), attempts dots, success pulse, failure shake.
- **Risk indicators** (`RiskMeter.tsx`, `ThreatBanner.tsx`) — per-message risk meter (low → critical gradient) and a top-of-page system-pressure banner that pulses red when the level is `CRITICAL`.
- **Admin SOC** — live alert feed with `cc-pulse-dot`, suspicious-activity list, system-pressure card next to the existing threat-level slider.

All of the new chrome lives in `frontend/src/components/cyber/` so the legacy pages keep working unchanged; the only behavioral change in `MessagingPage` and `CommandCenterPage` is that they now call `adminApi.systemPressure()` on a slow poll to drive `ThreatBanner`.

### Simulation ↔ messaging unification
The system used to have a real-time graph battlefield on one tab and a CPHS messaging flow on another tab without any cross-talk. We added a single `SystemPressureService` that aggregates four signals into one "pressure" score in `[0, 1]`:

1. Admin-set threat level (`ThreatSignalService.currentAttackIntensity01`).
2. Recent puzzle failure rate over the last 200 audit events.
3. Number of users currently flagged as at-risk by `UserBehaviorProfileService`.
4. Recent escalation/admin-action density in the audit log.

The score is exposed to every authenticated role at `GET /admin/system-pressure` (the endpoint name lives under `/admin` for routing simplicity but is read-only and metadata-only). The frontend polls this endpoint from the messaging page, the simulation page, and the admin SOC, so:

- High `attackIntensity` (manual or simulator-driven) immediately raises the pressure score and the adaptive engine sees the same input it always saw.
- Puzzle failures push two ways: they are recorded in `UserBehaviorProfile` (so future risk scoring escalates this user) and they bump `attackIntensity01` slightly so the simulation telemetry reflects what is happening in messaging.
- Admin threat-level changes show up in both messaging UI (warning banner, bigger risk badge) and simulation UI (the `Pressure` KPI tile).
- Message risk level surfaces in inbox cards and in the SOC alert feed at the same time.

There is no new game-theoretic policy here — only better plumbing of the signals that already exist.

### Adaptive engine transparency
Two changes make the adaptive engine easier to reason about without changing its logic:

- `AdaptiveModePolicyService.decide` now records the *bucketed* user puzzle burst and threat level into `reasons` (`user_puzzle_burst:high`, `threat_level:critical`, etc.). Every `MessageSendResponse.riskReasons` therefore carries the inputs that drove the decision, not just a generic note.
- `MessageService.sendMessage` records a separate `ADAPTIVE_ESCALATION` audit event whenever the engine *changes* the requested mode or *holds* a message. The event captures `riskLevel`, `puzzleType`, `reasons`, requested mode, and enforced mode in one place, which is what the SOC alert feed renders for operators.

### How the system behaves under attack
A short walkthrough of what a reader sees when stress goes up:

1. An admin slides the threat slider to 0.8. `attackIntensity01` is now 0.8.
2. A sender requests `NORMAL`. The adaptive engine sees `threat_level:high` and `recent_failures:low`, so `effectiveAlgorithmType` is escalated to `CPHS`. The send response carries `escalated=true`, `riskLevel=HIGH`, and `riskReasons=["threat_level:high", ...]`.
3. The receiver opens the inbox. The CPHS message shows a yellow risk badge, recovery state `CHALLENGE_REQUIRED`, and the `PuzzleArena` panel is rendered for the chosen puzzle type.
4. The receiver fails the puzzle several times. Each failure is recorded in `UserBehaviorProfile`, audited as `PUZZLE_SOLVE_FAILURE`, and after attempts are exhausted the message goes to `HELD`, attack intensity bumps by 0.05, and the SOC alert feed picks up the audit event.
5. The admin sees `users-at-risk` populated, `held-messages` populated, and `system-pressure` move into `ELEVATED`/`CRITICAL`. They can release the message or reset the user's failure counters; both produce `MESSAGE_RELEASE` / `RESET_FAILURES` audits and the recovery counter on the user goes up.

## Experimental Evaluation (research validation)
This upgrade adds a **research-grade evaluation harness** that turns the platform into an experimental validation system.

### What it does
- Runs **controlled repeated experiments** over security modes:
  - `NORMAL`, `SHCS`, `CPHS`, and `ADAPTIVE`
- Uses the existing **Attack → Defense → Recovery** game simulation engine for repeatable runs.
- Aggregates measurable metrics to support report/paper-aligned claims.

### Endpoint: `GET /evaluation/compare`
Query parameters:
- `attackIntensity` (0..1) — adversarial pressure proxy
- `numberOfRuns` (default 30)
- `defenseStrategy` (default `REDUNDANCY`): `REDUNDANCY`, `DYNAMIC_REROUTING`, `PUZZLE_ESCALATION`
- `numNodes`, `numEdges` (optional; defaults chosen by the service)
- `seed` (optional) — enables exact reproducibility across runs
- `persist` (default `false`) — when true, stores results in `evaluation_results`

Response shape:
- A map keyed by mode: `NORMAL`, `SHCS`, `CPHS`, `ADAPTIVE`
- Each value is `EvaluationMetrics`:
  - `attackSuccessRate`
  - `compromiseRatio`
  - `averageRecoveryTime`
  - `resilienceScore`
  - `userEffortScore`
  - `falsePositiveRate`

### How metrics are calculated (transparent + auditable)
- **attackSuccessRate**: mean of the simulation’s effective attack success probability.
- **compromiseRatio**: \(1 - afterAttackConnectivity\) (clamped to [0,1]).
- **averageRecoveryTime**: deterministic proxy derived from node/edge loss, recovery rate, budgets, and attack intensity.
- **resilienceScore**: composite of \(1 - compromiseRatio\), `recoveryRate`, and a normalized recovery-time term.
- **userEffortScore**:
  - NORMAL: 0
  - SHCS: small constant effort
  - CPHS/ADAPTIVE: proxy effort based on puzzle iteration bounds and expected attempts (reproducible by seed).
- **falsePositiveRate** (ADAPTIVE): fraction of runs that trigger **communication hold** (admin review required).

### Reproducibility
- If `seed` is provided, the service derives per-run seeds deterministically with `SeedUtil.mix64(...)`.
- If omitted, the service derives a stable seed from scenario parameters.

### Endpoint: `GET /evaluation/analysis`
Same query parameters as `/evaluation/compare`. Runs the same harness, then adds a short interpretation layer that only states what the aggregated metrics actually show (ordering, which mode wins on the ranking we use, and the fixed research-style labels below). It also runs three extra comparisons at intensities `0.2`, `0.5`, and `0.85` to fill `recommendedModeByThreatLevel` (`LOW` / `MEDIUM` / `HIGH`). Those recommendations are heuristics from this simulator, not external benchmarks.

Response `data` object:
- `metrics` — same map as `/evaluation/compare`
- `insights` — bullet-style strings derived from those numbers
- `bestMode` — mode name that ranks first on compromise (then resilience, then effort)
- `recommendedModeByThreatLevel` — best mode name per band from the extra runs
- `puzzleFailureEscalationRate` — `puzzleFailures / puzzleAttempts` over the last 200 audit events; `null` when the system has not seen any puzzle attempts yet
- `adminReviewRate` — `(hold + release admin actions) / messageSends` over the same window; `null` when no messages have been sent

## Research Alignment and Experimental Findings

This section is about how we *talk about* the system next to common paper-style baselines, not about claiming new theorems or real-world deployment numbers.

### Evaluation modes vs baseline categories
We did not swap out the crypto or game engine. We only label what already runs:
- **NORMAL** → *Static Security* (single-mode operation, no layered or challenge gate in the experiment harness sense)
- **SHCS** → *Layered Security* (stronger packaging / layered posture in the model)
- **CPHS** → *Challenge-Based Security* (puzzle-style gating reflected in effort and posture)
- **ADAPTIVE** → *Adaptive Security* (policy-driven mode selection and possible hold)

So when you read a chart or API output, “Static vs Layered vs Challenge vs Adaptive” is the same run you were already doing, just named in a way reviewers recognize.

### How this compares to “traditional” setups
Traditional here means fixed-mode systems you hold constant while stress increases. Our harness does exactly that for NORMAL/SHCS/CPHS, then adds ADAPTIVE as a separate curve. The improvement story is never assumed: you have to look at `compromiseRatio`, `resilienceScore`, and `userEffortScore` for the seed and intensity you chose. The `/evaluation/analysis` endpoint exists so those comparisons are spelled out in text tied to the numbers, instead of hand-waving in a write-up.

### What adaptive security is supposed to improve (in this codebase)
The point of ADAPTIVE in the experiment service is to let risk signals change effective mode and difficulty before the simulation step, so you can see whether that policy buys lower damage or faster recovery *in this model* compared to leaving a single mode on. If the metrics do not move, the honest conclusion is that this scenario did not benefit—not that the code “failed silently.”

### Reproducible result run
These numbers were generated from the existing `/evaluation/compare` and `/evaluation/analysis` paths with:
- `attackIntensity`: `0.2`, `0.5`, `0.85`
- `numberOfRuns`: `30`
- `seed`: `20260424`
- `defenseStrategy`: `REDUNDANCY`
- topology defaults: `numNodes=20`, `numEdges=35`
- `persist=false`

| attackIntensity | mode | attackSuccessRate | compromiseRatio | averageRecoveryTime | resilienceScore | userEffortScore | falsePositiveRate |
|---:|---|---:|---:|---:|---:|---:|---:|
| 0.20 | NORMAL | 0.720 | 0.114 | 11.838 | 0.728 | 0.000 | 0.000 |
| 0.20 | SHCS | 0.504 | 0.087 | 10.024 | 0.792 | 0.150 | 0.000 |
| 0.20 | CPHS | 0.396 | 0.029 | 7.230 | 0.908 | 14.334 | 0.000 |
| 0.20 | ADAPTIVE | 0.720 | 0.111 | 11.769 | 0.736 | 0.000 | 0.000 |
| 0.50 | NORMAL | 0.720 | 0.162 | 16.198 | 0.639 | 0.000 | 0.000 |
| 0.50 | SHCS | 0.504 | 0.150 | 15.141 | 0.660 | 0.150 | 0.000 |
| 0.50 | CPHS | 0.396 | 0.043 | 10.587 | 0.842 | 14.334 | 0.000 |
| 0.50 | ADAPTIVE | 0.720 | 0.177 | 16.527 | 0.631 | 0.000 | 0.000 |
| 0.85 | NORMAL | 0.720 | 0.215 | 20.372 | 0.582 | 0.000 | 0.000 |
| 0.85 | SHCS | 0.504 | 0.173 | 18.163 | 0.636 | 0.150 | 0.000 |
| 0.85 | CPHS | 0.396 | 0.065 | 14.358 | 0.782 | 14.334 | 0.000 |
| 0.85 | ADAPTIVE | 0.720 | 0.240 | 21.451 | 0.540 | 0.000 | 0.000 |

The `/evaluation/analysis` endpoint selected `CPHS` as `bestMode` for all three intensities in this run. Its threat-band recommendation map was also `LOW=CPHS`, `MEDIUM=CPHS`, and `HIGH=CPHS` for the same seed and harness settings.

### What the run shows
For this seed, CPHS had the lowest compromise ratio and highest resilience score at every tested attack intensity. That comes with a clear usability cost: its `userEffortScore` is much higher than SHCS or NORMAL, so it is not “free” security. SHCS gave a smaller but consistent improvement over NORMAL with only a small modeled effort cost.

ADAPTIVE did not win in this particular run. It stayed close to NORMAL at low intensity and was worse than NORMAL on compromise and resilience at `0.5` and `0.85`. That is useful to report honestly: this adaptive policy still needs stronger threat coupling or tuning before it can support a claim that it always improves outcomes. Also, every `falsePositiveRate` was `0.000`, so this run did not exercise the communication-hold path.

### Limitations I would put in a report
- Everything is a **controlled simulator**: graph attacks, budgets, and “effort” are proxies. Good for relative comparisons inside the project, weak for absolute security claims.
- Labels like “Static Security” are **mapping conventions** for exposition; they are not citations of a specific third-party product or standard.
- `/evaluation/analysis` can sound authoritative because it uses full sentences, but it is still **post-processing the same random runs**—garbage seed or too few runs still gives noisy text.
- The LOW/MEDIUM/HIGH recommendations use three fixed intensities; a different grid could change which mode “wins” each band.
- These results are one reproducible scenario, not a benchmark suite. Different seeds, graph sizes, defense strategies, and adaptive thresholds should be reported before making a broader claim.

## Advanced Cyber Defense Engine
The advanced layer extends the base simulation with an adaptive cyber-defense engine built on attack-graph progression, stochastic compromise, and repeated attacker-defender rounds.

### Architecture Overview
- Graph core: `AdvancedAttackGraph` with typed assets (`SERVER`, `IOT_DEVICE`, `GATEWAY`, `DATABASE`, `HONEYPOT`) and weighted exploit edges
- Attack execution: `MultiStageAttackEngine` across explicit APT stages
- Probability model: `CompromiseProbabilityService` using vulnerability, defense, exploit edge, attack pressure, and a modeled security posture effect (`algorithmType`, plus optional per-node posture)
- Adaptive loop: `AdaptiveStrategyEngine` updates attacker and defender policies each round
- Defensive controls: `MovingTargetDefenseService` and `HoneypotService`
- Persistence: `advanced_simulation_runs` with input parameters, seed, summary metrics, timeline, and round details

### Attack Stages
- `RECONNAISSANCE`
- `INITIAL_ACCESS`
- `LATERAL_MOVEMENT`
- `PERSISTENCE`
- `IMPACT`

### MTD Concept
- Rewires attacker-favored paths within safety limits
- Rotates logical node profiles
- Rotates encryption mode impacts
- Disables selected high-risk paths while preserving graph validity

**Model honesty note:** “identity rotation” permutes scalar node attributes (vulnerability/defense) for the abstract graph—it is **not** a faithful emulation of real-world asset renaming, VM migration, or network address shuffling.

### Deception Concept
- Injects honeypot assets into the attack graph
- Increases attacker uncertainty and trap engagements
- Wastes attacker budget and raises detection opportunities

### Seeded Reproducibility
- `seed` is optional in advanced requests
- when provided, runs are deterministic and replayable
- when omitted, the engine derives a **stable default seed** from the scenario parameters (and returns `seedUsed` in persisted history) so runs are still replayable without manual seed management

### SHCS/CPHS Influence on Compromise
- **Message layer (API):** NORMAL/SHCS/CPHS implement different packaging/metadata behaviors for stored ciphertext.
- **Advanced simulator layer:** `algorithmType` is treated as a **global modeled security posture knob** that scales compromise probability (and per-node posture may differ in heterogeneity experiments).
- This is **not** a claim that the graph simulator performs real cryptographic cryptanalysis; it is an explicit modeling shortcut for comparative experiments.

### Reproducibility semantics (Evaluation Framework)
- **`VARIED`**: repeated runs use deterministic derived seeds from `baseSeed` (or a stable default base derived from the scenario if `baseSeed` is omitted).
- **`FIXED`**: exactly **one** deterministic trajectory; **`repetitions` must be 1** and `baseSeed` is required.

### Algorithm comparison fairness (Advanced)
- Topology sampling is generated from a topology RNG stream that does **not** depend on `algorithmType`.
- `algorithmType` is applied afterwards as a posture overlay, so NORMAL/SHCS/CPHS comparisons do not “re-roll” the graph per algorithm.

## Model limitations (honest scope)
- No implemented **SPE / Nash solver**: “adaptive” policies are heuristics, not equilibrium solutions.
- No UAV mobility / RF propagation / disaster logistics model: UAV/disaster framing in the academic paper is **not** directly simulated here.
- Metrics like “attack/defense efficiency” are **dashboard scalars** for comparative experiments, not calibrated operational KPIs.

## Research alignment (paper/report vs this codebase)
- **Inspired by** the paper’s staged attacker/defender/recovery framing and the motivation for repeated strategic interaction.
- **Implemented as** an engineering simulation platform with explicit random graph generators, budgets, probabilistic events, persistence, evaluation harness, and UI—i.e., a **research-style instrument**, not a faithful executable transcription of a specific closed-form game solution.

### Advanced API Sample
Request:
```json
{
  "numNodes": 20,
  "numEdges": 35,
  "attackBudget": 6,
  "defenseBudget": 6,
  "recoveryBudget": 3,
  "rounds": 10,
  "enableMTD": true,
  "enableDeception": true,
  "algorithmType": "CPHS",
  "seed": 42
}
```

Response (shape):
```json
{
  "timestamp": "2026-04-19T11:00:00",
  "success": true,
  "message": "Advanced simulation run completed",
  "path": "/simulation/advanced-run",
  "data": {
    "advancedSimulationRunId": 1,
    "seed": 42,
    "resilienceScore": 0.713,
    "attackEfficiency": 0.287,
    "defenseEfficiency": 0.361,
    "deceptionEffectiveness": 0.412,
    "mtdEffectiveness": 0.298,
    "compromiseTimeline": [0.05, 0.10, 0.15],
    "compromisedNodeCountPerRound": [1, 2, 3],
    "roundDetails": [
      {
        "roundNumber": 1,
        "compromisedNodeCount": 1,
        "resilienceScore": 0.89,
        "attackerUtility": -1.1,
        "defenderUtility": -0.3
      }
    ]
  }
}
```

## Project Overview
This project models secure sender/receiver communication under adversarial pressure and extends to graph-based attacker-defender-recovery simulations. It combines cryptographic message protection (NORMAL, SHCS, CPHS) with game-theoretic resilience analysis, scenario comparison, history persistence, CSV export, and demo-ready dashboards.

## Architecture Summary
- Backend: Spring Boot (Java 17), layered architecture (`controller`, `service`, `repository`, `model`, `security`, `crypto`, `simulation`, `config`, `util`)
- Frontend: Thymeleaf + Chart.js for dashboards and comparisons
- Persistence: MySQL (`users`, `messages`, `simulation_runs`)
- Security: Spring Security with role-based access (`SENDER`, `RECEIVER`)
- Error model: standardized JSON API errors + friendly page redirects/messages

## Features by Phase
### Phase 1
- Authentication and role handling
- Secure messaging module
- Algorithms: NORMAL, SHCS, CPHS
- Receiver decrypt and CPHS puzzle solve
- Attack simulation on messages

### Phase 2
- Graph-based network simulation
- Defense, attack, and recovery stages
- Metrics: connectivity, losses, recovery rate, utilities
- REST endpoint for simulation execution

### Phase 3
- Simulation dashboard UI
- Scenario comparison (NORMAL vs SHCS vs CPHS)
- Simulation history and details
- Simulation run persistence
- Chart.js visualizations

### Phase 4 (Hardening and Demo Readiness)
- Stronger validation and bounds checking
- Improved global/API error handling
- Demo data seeding with config flag
- CSV export for simulation history
- Postman collection for API testing
- Dockerization (app + MySQL)
- Focused service-level unit tests
- Professional documentation and demo workflow

## Tech Stack
- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL 8
- Maven
- Chart.js
- Docker / Docker Compose

## Setup Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8+
- (Optional) Docker + Docker Compose

## Environment Variables
Core variables:
- `DB_URL` (default: `jdbc:mysql://localhost:3306/network_security_game?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: `root`)
- `SERVER_PORT` (default: `8080`)

Spring profiles:
- **Default local profile:** `dev` (enabled by default via `spring.profiles.active=dev` in `backend/src/main/resources/application.properties`)
  - Uses placeholder AES keys from `backend/src/main/resources/application-dev.properties` so local bootstrapping works without exporting secrets.
- **Docker Compose profile:** `docker` (enabled via `SPRING_PROFILES_ACTIVE=docker` in `backend/docker-compose.yml`)
  - Requires `APP_CRYPTO_MASTER_KEY` and `APP_CRYPTO_SHCS_KEY` (see `backend/.env.example`).

Demo seed variables:
- `APP_SEED_DEMO_DATA` (`true`/`false`, default: `false`)
- `APP_DEMO_SENDER_USERNAME`
- `APP_DEMO_SENDER_PASSWORD`
- `APP_DEMO_RECEIVER_USERNAME`
- `APP_DEMO_RECEIVER_PASSWORD`

Additional tunables are available for crypto/puzzle/attack/game modules via `APP_CRYPTO_*`, `APP_PUZZLE_*`, `APP_ATTACK_*`, `APP_GAME_*`.

See sample file: [`backend/.env.example`](backend/.env.example)

## Run Locally
1. Set environment variables (or use defaults).
2. Start application:
   - `cd backend`
   - `mvn clean package -DskipTests`
   - `mvn spring-boot:run`
3. Open:
   - `http://localhost:8080/login`

Notes:
- Local default DB URL supports first-boot startup without manual DB setup because the database can be created automatically and schema is initialized on startup.
- For a no-setup run path (database included), use Docker Compose instead.
- Local crypto keys: unless you export `APP_CRYPTO_MASTER_KEY` / `APP_CRYPTO_SHCS_KEY`, the `dev` profile uses **non-secret placeholders** suitable only for local demos.

## Run with Docker
1. Copy env template:
   - `cd backend`
   - `cp .env.example .env`
2. Set **two different** base64-encoded 32-byte AES keys in `.env`:
   - `APP_CRYPTO_MASTER_KEY`
   - `APP_CRYPTO_SHCS_KEY`
3. Start stack:
   - `docker compose up --build`
4. Access app:
   - `http://localhost:8080`

Files:
- [`backend/Dockerfile`](backend/Dockerfile)
- [`backend/docker-compose.yml`](backend/docker-compose.yml)

## API Endpoints
### Auth
- `POST /auth/register`
- `POST /auth/login`

### Messaging
- `POST /message/send`
- `GET /message/received`
- `POST /message/decrypt/{id}`

### Attack Simulation
- `GET /attack/simulate/{messageId}`

### Game Simulation
- `POST /simulation/run`
- `GET /simulation/history`
- `GET /simulation/history/{id}`
- `GET /simulation/compare?numNodes=...&numEdges=...&attackBudget=...&defenseBudget=...&recoveryBudget=...`
- `GET /simulation/export` (optional `algorithmType` filter)
- `POST /simulation/advanced-run`
- `GET /simulation/advanced-history`
- `GET /simulation/advanced-history/{id}`

## UI Pages
- `/login`
- `/send`
- `/receive`
- `/simulation/dashboard`
- `/simulation/history-page`
- `/simulation/history-page/{id}`
- `/simulation/compare-page`
- `/simulation/advanced-dashboard`

## Demo Workflow
1. Login with demo users or register your own sender/receiver accounts.
2. Send NORMAL, SHCS, and CPHS messages from the sender console.
3. Decrypt messages from the receiver console and run attack simulation.
4. Open simulation dashboard and run a scenario.
5. Open compare page to evaluate NORMAL vs SHCS vs CPHS on the same scenario.
6. Review history page and open detailed run views.
7. Export simulation results as CSV.

## Postman Collection
Collection path:
- [`docs/postman/Network-Security-Game.postman_collection.json`](docs/postman/Network-Security-Game.postman_collection.json)

Includes requests for:
- register/login
- send NORMAL/SHCS/CPHS
- decrypt
- attack simulate
- simulation run/history/compare/export

## Testing
Run tests:
- `cd backend`
- `mvn test`

Added focused tests:
- encryption/decryption flow
- puzzle generation/solve
- simulation metric generation
- comparison response shape

## Screenshots (Placeholders)
- `docs/screenshots/login-page.png`
- `docs/screenshots/send-console.png`
- `docs/screenshots/receiver-console.png`
- `docs/screenshots/simulation-dashboard.png`
- `docs/screenshots/simulation-compare.png`
- `docs/screenshots/simulation-history.png`

## Future Enhancements
- richer game strategies (adaptive and repeated games)
- advanced resilience indicators and confidence intervals
- report generation (PDF/HTML)
- CI pipeline with lint, test, and container scan stages
- optional JWT mode for stateless API authentication
