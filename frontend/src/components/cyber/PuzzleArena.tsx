import { useEffect, useMemo, useRef, useState } from "react";
import { ApiError } from "../../api/client";
import { puzzleApi } from "../../api/message";
import type { PuzzleChallengeResponse, PuzzleType } from "../../api/types";

type Outcome = "idle" | "success" | "failure";

const TYPE_PILL_CLASS: Record<PuzzleType, string> = {
  POW_SHA256: "is-pow",
  ARITHMETIC: "is-arith",
  ENCODED: "is-encoded",
  PATTERN: "is-pattern",
};

const TYPE_LABEL: Record<PuzzleType, string> = {
  POW_SHA256: "SECURITY CHALLENGE · HASH PROOF",
  ARITHMETIC: "SECURITY CHALLENGE · ARITHMETIC",
  ENCODED: "SECURITY CHALLENGE · ENCODED MESSAGE",
  PATTERN: "SECURITY CHALLENGE · PATTERN MATCH",
};

const TYPE_TAGLINE: Record<PuzzleType, string> = {
  POW_SHA256:
    "Hash proof: search a small nonce so SHA-256(challenge:nonce) equals the target. Demonstrates CPHS gating; not a hardness claim.",
  ARITHMETIC:
    "Arithmetic gate: evaluate the expression with operator precedence and submit the integer.",
  ENCODED:
    "Encoded gate: decode the base64 phrase and submit the original text. Pedagogical, not crypt-hard.",
  PATTERN:
    "Pattern gate: continue the numeric sequence (arithmetic, geometric, or Fibonacci-style) and submit the next value.",
};

export function PuzzleArena({
  challenge,
  onSolved,
  onNotice,
}: {
  challenge: PuzzleChallengeResponse | null;
  onSolved: () => Promise<void> | void;
  onNotice: (s: string | null) => void;
}) {
  const [running, setRunning] = useState(false);
  const [progress, setProgress] = useState(0);
  const [solving, setSolving] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>("idle");
  const [outcomeMessage, setOutcomeMessage] = useState<string | null>(null);
  const [answer, setAnswer] = useState("");

  const startedAt = useRef<number>(Date.now());
  const totalSeconds = useTotalWindow(challenge?.expiresAt ?? null);

  useEffect(() => {
    startedAt.current = Date.now();
    setOutcome("idle");
    setOutcomeMessage(null);
    setAnswer("");
    setProgress(0);
  }, [challenge?.messageId, challenge?.puzzleType]);

  const remainingSeconds = useExpiryCountdown(challenge?.expiresAt ?? null);

  const attemptsLeft = challenge ? Math.max(0, challenge.attemptsAllowed - challenge.attemptsUsed) : 0;

  const expiryPill = useMemo(() => {
    if (remainingSeconds == null) return null;
    let cls = "";
    if (remainingSeconds < 30) cls = "is-crit";
    else if (remainingSeconds < 90) cls = "is-warn";
    return (
      <span className={`cc-puzzle-timer ${cls}`}>{formatTimer(remainingSeconds)}</span>
    );
  }, [remainingSeconds]);

  const timerFraction = useMemo(() => {
    if (remainingSeconds == null || totalSeconds == null || totalSeconds <= 0) return null;
    return Math.max(0, Math.min(1, remainingSeconds / totalSeconds));
  }, [remainingSeconds, totalSeconds]);

  if (!challenge) {
    return <div className="cc-empty">Loading puzzle…</div>;
  }

  if (challenge.solved) {
    return (
      <div className="cc-puzzle is-solved">
        <div className="cc-puzzle-banner is-success">
          Puzzle solved. Message key recovered — Decrypt is unlocked.
        </div>
      </div>
    );
  }

  const expired = remainingSeconds != null && remainingSeconds <= 0;
  const lockedOut = attemptsLeft <= 0;

  async function submit(payload: { nonce?: number; answer?: string }) {
    if (!challenge) return;
    setSolving(true);
    setOutcome("idle");
    setOutcomeMessage(null);
    onNotice(null);
    try {
      const res = await puzzleApi.solve(challenge.messageId, payload);
      if (res.solved) {
        const elapsed = ((Date.now() - startedAt.current) / 1000).toFixed(1);
        setOutcome("success");
        setOutcomeMessage(`Solved in ${elapsed}s. Decrypt is now available.`);
        onNotice("Puzzle solved.");
        await onSolved();
      } else {
        const left = res.attemptsAllowed - res.attemptsUsed;
        setOutcome("failure");
        setOutcomeMessage(
          left > 0
            ? `Wrong answer. ${left} attempt${left === 1 ? "" : "s"} left before the message is held.`
            : "No attempts remaining. Message moved to admin review.",
        );
        onNotice(null);
        await onSolved();
      }
    } catch (err) {
      setOutcome("failure");
      const msg = err instanceof ApiError ? err.message : "Server rejected the answer.";
      setOutcomeMessage(msg);
      onNotice(null);
      try { await onSolved(); } catch { /* noop */ }
    } finally {
      setSolving(false);
    }
  }

  async function autoSolveHash() {
    if (!challenge) return;
    const target = challenge.targetHash;
    if (!target) {
      setOutcome("failure");
      setOutcomeMessage("Hash puzzle is missing target hash.");
      return;
    }
    setRunning(true);
    setProgress(0);
    setOutcome("idle");
    setOutcomeMessage(null);
    onNotice(null);
    try {
      const max = challenge.maxIterations;
      const checkpoint = Math.max(1, Math.floor(max / 60));
      for (let n = 0; n < max; n++) {
        const h = await sha256Hex(`${challenge.challenge}:${n}`);
        if (h === target) {
          setProgress(100);
          await submit({ nonce: n });
          return;
        }
        if (n % checkpoint === 0) {
          setProgress(Math.min(99, Math.floor((n / max) * 100)));
          await new Promise((r) => setTimeout(r, 0));
        }
      }
      setOutcome("failure");
      setOutcomeMessage("No valid nonce found within the iteration budget.");
    } finally {
      setRunning(false);
    }
  }

  const pillClass = TYPE_PILL_CLASS[challenge.puzzleType] ?? "is-pow";
  const headerLabel = TYPE_LABEL[challenge.puzzleType] ?? challenge.puzzleType;
  const tagline = TYPE_TAGLINE[challenge.puzzleType] ?? "";

  const arenaClass = `cc-puzzle ${outcome === "success" ? "is-success" : ""} ${outcome === "failure" ? "is-failure" : ""}`;

  return (
    <div className={arenaClass} aria-label={`Security challenge: ${headerLabel}`}>
      <div className="cc-puzzle-header">
        <div className="cc-puzzle-title">Security challenge</div>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <span className={`cc-puzzle-type-pill ${pillClass}`}>{headerLabel}</span>
          {expiryPill}
        </div>
      </div>

      {timerFraction != null ? (
        <div
          className={`cc-puzzle-timerbar ${remainingSeconds! < 30 ? "is-crit" : remainingSeconds! < 90 ? "is-warn" : ""}`}
          aria-hidden
        >
          <span style={{ width: `${(timerFraction * 100).toFixed(1)}%` }} />
        </div>
      ) : null}

      <div className="cc-puzzle-question">{challenge.question || tagline}</div>
      <div className="cc-puzzle-tagline" style={{ fontSize: 12, color: "var(--cc-muted)", marginTop: -2 }}>
        {tagline}
      </div>

      {challenge.puzzleType === "POW_SHA256" ? (
        <>
          <div className="cc-puzzle-challenge">{challenge.challenge}</div>
          <div className="cc-puzzle-progress" aria-hidden>
            <span style={{ width: `${progress}%` }} />
          </div>
          <div className="cc-puzzle-actions">
            <button
              className="cc-btn"
              type="button"
              onClick={() => void autoSolveHash()}
              disabled={running || solving || lockedOut || expired}
            >
              {running ? `Searching… ${progress}%` : "Run nonce search"}
            </button>
            <span style={{ fontSize: 12, color: "var(--cc-muted)" }}>
              max iterations {challenge.maxIterations.toLocaleString()}
            </span>
          </div>
        </>
      ) : challenge.puzzleType === "ARITHMETIC" ? (
        <>
          <div className="cc-puzzle-challenge">{challenge.challenge}</div>
          <ManualAnswerForm
            placeholder="Enter integer result"
            inputMode="numeric"
            answer={answer}
            setAnswer={setAnswer}
            disabled={solving || lockedOut || expired}
            onSubmit={() => void submit({ answer: answer.trim() })}
          />
        </>
      ) : challenge.puzzleType === "ENCODED" ? (
        <>
          <div className="cc-puzzle-challenge" style={{ fontSize: 14 }}>{challenge.challenge}</div>
          <ManualAnswerForm
            placeholder="Enter the decoded phrase"
            answer={answer}
            setAnswer={setAnswer}
            disabled={solving || lockedOut || expired}
            onSubmit={() => void submit({ answer: answer.trim() })}
          />
        </>
      ) : challenge.puzzleType === "PATTERN" ? (
        <>
          <div className="cc-puzzle-challenge">{challenge.challenge}</div>
          <ManualAnswerForm
            placeholder="Enter the next value"
            inputMode="numeric"
            answer={answer}
            setAnswer={setAnswer}
            disabled={solving || lockedOut || expired}
            onSubmit={() => void submit({ answer: answer.trim() })}
          />
        </>
      ) : null}

      <div className="cc-puzzle-meta">
        <span className="cc-puzzle-attempts" aria-label={`attempts: ${challenge.attemptsUsed}/${challenge.attemptsAllowed}`}>
          {Array.from({ length: challenge.attemptsAllowed }).map((_, i) => (
            <span key={i} className={`cc-puzzle-dot ${i < challenge.attemptsUsed ? "is-used" : ""}`} />
          ))}
          <span style={{ marginLeft: 6 }}>{attemptsLeft}/{challenge.attemptsAllowed} left</span>
        </span>
        <span>#{challenge.messageId} · {challenge.puzzleType}</span>
      </div>

      {outcome === "success" && outcomeMessage ? (
        <div className="cc-puzzle-banner is-success">{outcomeMessage}</div>
      ) : null}
      {outcome === "failure" && outcomeMessage ? (
        <div className="cc-puzzle-banner is-failure">{outcomeMessage}</div>
      ) : null}
      {expired && outcome !== "success" ? (
        <div className="cc-puzzle-banner is-failure">Challenge window expired. Ask the sender to re-issue.</div>
      ) : null}
      {lockedOut && outcome !== "success" ? (
        <div className="cc-puzzle-banner is-failure">No attempts remaining. Message moved to admin review.</div>
      ) : null}
    </div>
  );
}

function ManualAnswerForm({
  placeholder,
  inputMode,
  answer,
  setAnswer,
  disabled,
  onSubmit,
}: {
  placeholder: string;
  inputMode?: "numeric" | "text";
  answer: string;
  setAnswer: (s: string) => void;
  disabled: boolean;
  onSubmit: () => void;
}) {
  return (
    <div className="cc-puzzle-actions">
      <input
        className="cc-input"
        value={answer}
        onChange={(e) => setAnswer(e.target.value)}
        placeholder={placeholder}
        inputMode={inputMode ?? "text"}
        disabled={disabled}
        style={{ flex: "1 1 200px", minWidth: 0 }}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !disabled && answer.trim()) onSubmit();
        }}
      />
      <button
        className="cc-btn"
        type="button"
        onClick={onSubmit}
        disabled={disabled || !answer.trim()}
      >
        Submit answer
      </button>
    </div>
  );
}

function useExpiryCountdown(expiresAt: string | null): number | null {
  const [remaining, setRemaining] = useState<number | null>(null);

  useEffect(() => {
    if (!expiresAt) {
      setRemaining(null);
      return;
    }
    const target = new Date(expiresAt).getTime();
    const tick = () => {
      const r = Math.max(0, Math.floor((target - Date.now()) / 1000));
      setRemaining(r);
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [expiresAt]);

  return remaining;
}

function useTotalWindow(expiresAt: string | null): number | null {
  const ref = useRef<{ exp: string | null; total: number | null }>({ exp: null, total: null });
  if (expiresAt !== ref.current.exp) {
    if (!expiresAt) {
      ref.current = { exp: null, total: null };
    } else {
      const total = Math.max(1, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
      ref.current = { exp: expiresAt, total };
    }
  }
  return ref.current.total;
}

function formatTimer(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
}

async function sha256Hex(text: string): Promise<string> {
  const enc = new TextEncoder();
  const buf = await crypto.subtle.digest("SHA-256", enc.encode(text));
  const bytes = new Uint8Array(buf);
  let out = "";
  for (let i = 0; i < bytes.length; i++) out += bytes[i].toString(16).padStart(2, "0");
  return out;
}
