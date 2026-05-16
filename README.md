# Network-Security-Game

Network-Security-Game is a full-stack cybersecurity project based on the original report idea: model secure communication as an attack, defense, and recovery game.

A sender chooses a protection path, the receiver unlocks the message only after the required security step, and the system adapts when threat signals rise. The admin console supports recovery without exposing plaintext.

## Core Idea

The project focuses on resilient connectivity under selective jamming and packet-classification threats. It does not try to be a production messenger. It is a working research system that shows how protection mode, puzzles, adaptive escalation, attack simulation, and recovery fit together.

## Security Modes

- `NORMAL`: AES-GCM encrypted message storage with no receiver puzzle.
- `SHCS`: encrypted payload with hidden metadata packaging to reduce packet classification value.
- `CPHS`: encrypted payload protected by a receiver puzzle gate before decryption.
- `ADAPTIVE`: sender-selected auto mode. The backend chooses `NORMAL`, `SHCS`, or `CPHS` from risk, threat pressure, puzzle failures, and session signals.

## Adaptive Security

The adaptive engine scores system pressure, recent failures, session anomalies, and admin threat level. Low risk keeps the requested path. Elevated risk can step up to `SHCS`. High or critical risk enforces `CPHS`; critical risk can hold a message for admin review.

This is a transparent heuristic, not a Nash-equilibrium solver. The game-theoretic part is represented through the attack, defense, and recovery workflow and the evaluation simulator.

## Puzzle Engine

`CPHS` messages require the receiver to solve a challenge before decrypting. Current puzzle types are SHA-256 proof-of-work, arithmetic, encoded text, and pattern continuation.

Puzzles are time-bounded, attempt-limited, and tied to the message. Wrong answers increase risk and can move a message into recovery.

## Admin Recovery

Admins can view held messages, risk signals, audit events, and recovery state. They can release held messages or reset puzzle failure counters after a password step-up.

Admins cannot decrypt messages and no admin endpoint returns plaintext.

## OWASP-Aligned Controls

- Role-based access control for sender, receiver, and admin paths.
- Session regeneration after login.
- HttpOnly/SameSite session cookies.
- Request replay protection with timestamp and nonce checks.
- HMAC request integrity checks for sensitive actions.
- Rate limits on login, registration, send, puzzle solve, and admin actions.
- Security headers and strict API error handling.
- No plaintext message storage.

## Evaluation Results

The built-in evaluation harness compares protection paths under attack/defense/recovery simulations. The current recorded sample uses `numNodes=20`, `numEdges=35`, and `defenseStrategy=REDUNDANCY`.

| Attack intensity | Mode | Compromise ratio | Resilience score | Recovery time | User effort |
|---:|---|---:|---:|---:|---:|
| 0.20 | NORMAL | 0.114 | 0.728 | 11.84 | 0.00 |
| 0.20 | SHCS | 0.087 | 0.792 | 10.02 | 0.15 |
| 0.20 | CPHS | 0.029 | 0.908 | 7.23 | 14.33 |
| 0.20 | ADAPTIVE | 0.111 | 0.736 | 11.77 | 0.00 |
| 0.50 | NORMAL | 0.162 | 0.639 | 16.20 | 0.00 |
| 0.50 | SHCS | 0.150 | 0.660 | 15.14 | 0.15 |
| 0.50 | CPHS | 0.043 | 0.842 | 10.59 | 14.33 |
| 0.50 | ADAPTIVE | 0.177 | 0.631 | 16.53 | 0.00 |
| 0.85 | NORMAL | 0.215 | 0.582 | 20.37 | 0.00 |
| 0.85 | SHCS | 0.173 | 0.636 | 18.16 | 0.15 |
| 0.85 | CPHS | 0.065 | 0.782 | 14.36 | 14.33 |
| 0.85 | ADAPTIVE | 0.240 | 0.540 | 21.45 | 0.00 |

CPHS gives the strongest resilience in this sample, with higher user effort. SHCS improves over NORMAL with low friction. ADAPTIVE stayed close to NORMAL in this recorded run, which is useful to show honestly: its value depends on live risk signals being high enough to step up.

## Run Locally

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Validation:

```bash
cd backend
mvn -q test

cd ../frontend
npm run build
```

## Main Screens

- Sender console: choose mode, send message, see risk and recovery plan.
- Receiver screen: solve CPHS challenge and decrypt only after unlock.
- Admin SOC: review holds, risk signals, audit events, and recovery actions.
- Simulation: run attack, defense, and recovery scenarios.
- Evaluation: compare security modes across repeated experiments.

## Honest Limitations

- The adaptive policy is explainable but heuristic.
- The simulator abstracts network behavior; it is not a real RF or UAV stack.
- Puzzle challenges demonstrate gated decryption; they are not a claim of production-grade puzzle hardness.
- Local development uses in-memory replay/rate-limit state.
- Key rotation is handled operationally, not by an automated re-encryption workflow.
