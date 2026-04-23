# Game-Theoretic Framework for Resilient Network Security and Recovery

This system is a secure simulation platform for resilient message security, adversarial disruption analysis, and network recovery evaluation with validated APIs, controlled workflows, and **explicit seeding semantics** for repeatable experiments.

## Core Capabilities
- Secure user authentication
- Validated message workflows
- Controlled simulation execution
- Exportable results
- Reproducible demo via seeding

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
- **Reproducible simulations when seeds are provided** (advanced + evaluation); otherwise the system derives stable default seeds from scenario parameters (see “Reproducibility semantics”)

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
