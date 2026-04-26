import { useEffect, useMemo, useState } from "react";
import type {
  AlgorithmType,
  MessageSendResponse,
  MessageSummaryResponse,
  PuzzleChallengeResponse,
  PuzzleType,
  RecoveryState,
  RiskLevel,
  SystemPressureResponse,
} from "../api/types";
import { ApiError } from "../api/client";
import { messageApi, puzzleApi } from "../api/message";
import { adminApi } from "../api/admin";
import { useAuth } from "../state/auth/AuthProvider";
import { PuzzleArena } from "../components/cyber/PuzzleArena";
import { ThreatBanner } from "../components/cyber/ThreatBanner";
import { RiskMeter } from "../components/cyber/RiskMeter";
import { AttackTimeline, type PhaseStep } from "../components/cyber/AttackTimeline";

export function MessagingPage() {
  const { user } = useAuth();
  const [mode, setMode] = useState<"send" | "inbox">("send");
  const [notice, setNotice] = useState<string | null>(null);
  const [pressure, setPressure] = useState<SystemPressureResponse | null>(null);

  const canSend = user?.role === "SENDER";
  const canReceive = user?.role === "RECEIVER";

  useEffect(() => {
    if (canReceive) setMode("inbox");
    else setMode("send");
  }, [canReceive]);

  useEffect(() => {
    let cancelled = false;
    async function tick() {
      try {
        const snap = await adminApi.systemPressure();
        if (!cancelled) setPressure(snap);
      } catch {
        // best-effort polling; ignore failures
      }
    }
    void tick();
    const id = setInterval(tick, 8000);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  return (
    <div className="cc-page">
      <ThreatBanner snapshot={pressure} />
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Secure Messaging</div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button className="cc-btn" type="button" onClick={() => setMode("send")} disabled={!canSend}>
              Send
            </button>
            <button className="cc-btn" type="button" onClick={() => setMode("inbox")} disabled={!canReceive}>
              Receive
            </button>
          </div>
        </div>
        <div className="cc-panel-body">
          {notice ? <div className="cc-notice">{notice}</div> : null}
          {mode === "send" ? (
            <SendPanel enabled={canSend} onNotice={setNotice} />
          ) : (
            <InboxPanel enabled={canReceive} onNotice={setNotice} />
          )}
        </div>
      </section>
    </div>
  );
}

const RISK_COLORS: Record<RiskLevel, string> = {
  LOW: "rgba(61, 220, 151, 0.18)",
  ELEVATED: "rgba(246, 196, 69, 0.18)",
  HIGH: "rgba(255, 142, 95, 0.20)",
  CRITICAL: "rgba(255, 93, 108, 0.22)",
};

const RECOVERY_COPY: Record<RecoveryState, string> = {
  NORMAL: "Normal — message is unlocked.",
  CHALLENGE_REQUIRED: "Challenge required — solve the puzzle to unlock.",
  ESCALATED: "Escalated — system upgraded the protection level.",
  HELD: "Held — admin must review before you can proceed.",
  ADMIN_REVIEW_REQUIRED: "Admin review required.",
  RECOVERY_IN_PROGRESS: "Recovery in progress.",
  RECOVERED: "Recovered — message unlocked through admin-supervised recovery.",
  FAILED: "Failed — puzzle expired or attempts exhausted.",
};

function RiskBadge({ level, score }: { level: RiskLevel | null; score: number | null }) {
  if (!level) return null;
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: "2px 10px",
        borderRadius: 999,
        background: RISK_COLORS[level] ?? "rgba(61, 220, 151, 0.18)",
        color: "var(--cc-ink)",
        fontSize: 12,
        fontWeight: 800,
        letterSpacing: "0.06em",
        textTransform: "uppercase",
      }}
    >
      {level}
      {score == null ? null : <span style={{ opacity: 0.7 }}>{score.toFixed(2)}</span>}
    </span>
  );
}

function StateBadge({ state }: { state: RecoveryState | null }) {
  if (!state) return null;
  return (
    <span
      style={{
        display: "inline-flex",
        padding: "2px 10px",
        borderRadius: 999,
        background: "rgba(100, 181, 255, 0.16)",
        fontSize: 12,
        fontWeight: 800,
        letterSpacing: "0.06em",
      }}
    >
      {state}
    </span>
  );
}

function SendPanel({ enabled, onNotice }: { enabled: boolean; onNotice: (s: string | null) => void }) {
  const [receiverUsername, setReceiverUsername] = useState("");
  const [content, setContent] = useState("");
  const [algorithmType, setAlgorithmType] = useState<AlgorithmType>("SHCS");
  const [puzzleType, setPuzzleType] = useState<PuzzleType>("POW_SHA256");
  const [busy, setBusy] = useState(false);
  const [last, setLast] = useState<MessageSendResponse | null>(null);

  async function onSend() {
    onNotice(null);
    setBusy(true);
    try {
      const res = await messageApi.send({
        receiverUsername,
        content,
        algorithmType,
        ...(algorithmType === "CPHS" ? { puzzleType } : {}),
      });
      setLast(res);
      onNotice("Message stored securely.");
      setContent("");
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Send failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 12 }}>
      {!enabled ? (
        <div className="cc-notice">This account is not allowed to send messages. Login as `SENDER`.</div>
      ) : null}

      <label className="cc-label">
        Receiver username
        <input className="cc-input" value={receiverUsername} onChange={(e) => setReceiverUsername(e.target.value)} disabled={!enabled || busy} />
      </label>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
        <label className="cc-label">
          Algorithm
          <select className="cc-select" value={algorithmType} onChange={(e) => setAlgorithmType(e.target.value as AlgorithmType)} disabled={!enabled || busy}>
            <option value="NORMAL">NORMAL</option>
            <option value="SHCS">SHCS</option>
            <option value="CPHS">CPHS</option>
          </select>
        </label>

        <label className="cc-label" style={{ opacity: algorithmType === "CPHS" ? 1 : 0.55 }}>
          Puzzle type {algorithmType === "CPHS" ? "" : "(CPHS only)"}
          <select
            className="cc-select"
            value={puzzleType}
            onChange={(e) => setPuzzleType(e.target.value as PuzzleType)}
            disabled={!enabled || busy || algorithmType !== "CPHS"}
          >
            <option value="POW_SHA256">HASH PUZZLE (SHA-256)</option>
            <option value="ARITHMETIC">ARITHMETIC CHALLENGE</option>
            <option value="ENCODED">ENCODED MESSAGE CHALLENGE</option>
            <option value="PATTERN">PATTERN MATCH CHALLENGE</option>
          </select>
        </label>
      </div>

      <label className="cc-label" style={{ textTransform: "none", letterSpacing: "0.02em" }}>
        Message content
        <textarea
          className="cc-input"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={5}
          disabled={!enabled || busy}
          style={{ resize: "vertical" }}
        />
      </label>

      <button className="cc-btn" type="button" onClick={() => void onSend()} disabled={!enabled || busy || !receiverUsername || !content}>
        {busy ? "Sending…" : "Send message"}
      </button>

      {last ? (
        <div className="cc-surface" style={{ padding: 12, display: "grid", gap: 8 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
            <strong>Send result</strong>
            <RiskBadge level={last.riskLevel} score={last.riskScore} />
            <StateBadge state={last.recoveryState} />
          </div>
          <RiskMeter score={last.riskScore} level={last.riskLevel} />
          <div className="cc-result-row">
            <span>Requested mode</span>
            <span>{last.requestedAlgorithmType}</span>
          </div>
          <div className="cc-result-row">
            <span>Enforced mode</span>
            <span>
              {last.effectiveAlgorithmType}
              {last.escalated ? " (escalated)" : ""}
            </span>
          </div>
          <div className="cc-result-row">
            <span>Admin review required</span>
            <span>{last.adminReviewRequired ? "yes" : "no"}</span>
          </div>
          {last.warningMessage ? <div className="cc-notice">{last.warningMessage}</div> : null}
          {last.riskReasons && last.riskReasons.length ? (
            <div style={{ fontSize: 12, color: "var(--cc-muted)" }}>Reasons: {last.riskReasons.join(", ")}</div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function InboxPanel({ enabled, onNotice }: { enabled: boolean; onNotice: (s: string | null) => void }) {
  const [busy, setBusy] = useState(false);
  const [items, setItems] = useState<MessageSummaryResponse[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [decrypted, setDecrypted] = useState<string | null>(null);
  const [puzzle, setPuzzle] = useState<PuzzleChallengeResponse | null>(null);

  const selected = useMemo(() => items.find((m) => m.id === selectedId) ?? null, [items, selectedId]);

  async function load() {
    onNotice(null);
    setBusy(true);
    try {
      const data = await messageApi.received();
      setItems(data);
      if (data.length && selectedId == null) setSelectedId(data[0].id);
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Failed to load inbox.");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    if (enabled) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]);

  async function loadPuzzle(id: number) {
    try {
      const refreshed = await puzzleApi.challenge(id);
      setPuzzle(refreshed);
    } catch {
      setPuzzle(null);
    }
  }

  useEffect(() => {
    setPuzzle(null);
    setDecrypted(null);
    if (!selectedId) return;
    if (selected?.algorithmType !== "CPHS") return;
    if (selected?.status === "UNLOCKED") return;
    if (selected?.recoveryState === "HELD" || selected?.recoveryState === "ADMIN_REVIEW_REQUIRED") return;
    void loadPuzzle(selectedId);
  }, [selectedId, selected?.algorithmType, selected?.status, selected?.recoveryState]);

  async function decrypt() {
    if (!selectedId) return;
    onNotice(null);
    setBusy(true);
    try {
      const res = await messageApi.decrypt(selectedId);
      setDecrypted(res.decryptedContent);
      onNotice("Message decrypted.");
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Decrypt failed.");
    } finally {
      setBusy(false);
    }
  }

  if (!enabled) {
    return <div className="cc-notice">This account is not allowed to receive messages. Login as `RECEIVER`.</div>;
  }

  return (
    <div style={{ display: "grid", gridTemplateColumns: "380px minmax(0,1fr)", gap: 12 }}>
      <div className="cc-surface" style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
          <div style={{ fontWeight: 900, letterSpacing: "-0.01em" }}>Inbox</div>
          <button className="cc-btn cc-btn--ghost" type="button" onClick={() => void load()} disabled={busy}>
            Refresh
          </button>
        </div>
        <div style={{ marginTop: 10, display: "grid", gap: 8, maxHeight: 460, overflow: "auto" }}>
          {items.length ? (
            items.map((m) => (
              <button
                key={m.id}
                type="button"
                className="cc-btn cc-btn--ghost"
                onClick={() => {
                  setSelectedId(m.id);
                  setDecrypted(null);
                }}
                style={{
                  textAlign: "left",
                  borderColor: selectedId === m.id ? "rgba(100, 181, 255, 0.28)" : "rgba(123, 145, 189, 0.18)",
                  display: "grid",
                  gap: 4,
                }}
              >
                <div style={{ fontWeight: 900 }}>{m.senderUsername}</div>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  <span style={{ color: "var(--cc-muted)", fontSize: 12, fontWeight: 650 }}>{m.algorithmType}</span>
                  <RiskBadge level={m.riskLevel} score={m.riskScore} />
                  <StateBadge state={m.recoveryState} />
                </div>
              </button>
            ))
          ) : (
            <div className="cc-empty" style={{ padding: 10 }}>
              No messages received.
            </div>
          )}
        </div>
      </div>

      <div className="cc-surface" style={{ padding: 12 }}>
        {selected ? (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
              <div style={{ fontWeight: 950, letterSpacing: "-0.02em" }}>Message #{selected.id}</div>
              <button
                className="cc-btn"
                type="button"
                onClick={() => void decrypt()}
                disabled={
                  busy ||
                  selected.recoveryState === "HELD" ||
                  selected.recoveryState === "ADMIN_REVIEW_REQUIRED" ||
                  (selected.algorithmType === "CPHS" && (puzzle == null || !puzzle.solved))
                }
                title={
                  selected.algorithmType === "CPHS" && (puzzle == null || !puzzle.solved)
                    ? "Solve the puzzle first"
                    : undefined
                }
              >
                {busy ? "Working…" : "Decrypt"}
              </button>
            </div>
            <div style={{ marginTop: 10, display: "grid", gap: 8 }}>
              <div className="cc-result-row">
                <span>From</span>
                <span>{selected.senderUsername}</span>
              </div>
              <div className="cc-result-row">
                <span>Algorithm</span>
                <span>{selected.algorithmType}</span>
              </div>
              <div className="cc-result-row">
                <span>Status</span>
                <span style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                  {selected.status ?? "?"} <RiskBadge level={selected.riskLevel} score={selected.riskScore} />
                  <StateBadge state={selected.recoveryState} />
                </span>
              </div>

              <RiskMeter
                score={selected.riskScore}
                level={selected.riskLevel}
                caption={selected.recoveryState ? RECOVERY_COPY[selected.recoveryState] : undefined}
              />

              <AttackTimeline steps={timelineFor(selected.recoveryState)} />

              {selected.warningMessage || selected.warning ? (
                <div className="cc-notice">{selected.warningMessage ?? selected.warning}</div>
              ) : null}

              {selected.algorithmType === "CPHS" && selected.status !== "UNLOCKED" ? (
                <PuzzleArena
                  challenge={puzzle}
                  onSolved={async () => {
                    if (selectedId) {
                      await loadPuzzle(selectedId);
                    }
                    await load();
                  }}
                  onNotice={onNotice}
                />
              ) : null}

              <div style={{ marginTop: 8, color: "var(--cc-muted)", fontWeight: 650, fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase" }}>
                Encrypted payload
              </div>
              <pre
                style={{
                  margin: 0,
                  padding: 12,
                  borderRadius: 14,
                  border: "1px solid rgba(123, 145, 189, 0.18)",
                  background: "rgba(7, 11, 18, 0.50)",
                  color: "rgba(234, 242, 255, 0.90)",
                  overflow: "auto",
                  maxHeight: 180,
                }}
              >
                {selected.encryptedContent}
              </pre>

              {decrypted ? (
                <>
                  <div style={{ marginTop: 8, color: "rgba(103, 232, 179, 0.90)", fontWeight: 850, fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase" }}>
                    Decrypted content
                  </div>
                  <pre
                    style={{
                      margin: 0,
                      padding: 12,
                      borderRadius: 14,
                      border: "1px solid rgba(103, 232, 179, 0.22)",
                      background: "rgba(7, 11, 18, 0.50)",
                      color: "rgba(234, 242, 255, 0.92)",
                      overflow: "auto",
                      maxHeight: 220,
                    }}
                  >
                    {decrypted}
                  </pre>
                </>
              ) : null}
            </div>
          </>
        ) : (
          <div className="cc-empty">Select a message to inspect.</div>
        )}
      </div>
    </div>
  );
}

function timelineFor(state: RecoveryState | null): PhaseStep[] {
  const baseAttack: PhaseStep = {
    phase: "attack",
    title: "Attack vector",
    sub: "Sender adopts a posture under risk pressure.",
  };
  const baseDefense: PhaseStep = {
    phase: "defense",
    title: "Defense gate",
    sub: "Adaptive engine picks the enforced mode.",
  };
  const baseRecovery: PhaseStep = {
    phase: "recovery",
    title: "Recovery",
    sub: "Receiver clears the challenge or admin releases.",
  };

  switch (state) {
    case "CHALLENGE_REQUIRED":
    case "ESCALATED":
      return [
        baseAttack,
        { ...baseDefense, active: true },
        baseRecovery,
      ];
    case "HELD":
    case "ADMIN_REVIEW_REQUIRED":
    case "RECOVERY_IN_PROGRESS":
      return [
        baseAttack,
        baseDefense,
        { ...baseRecovery, active: true, sub: "Admin-supervised recovery in progress." },
      ];
    case "RECOVERED":
      return [
        baseAttack,
        baseDefense,
        { ...baseRecovery, active: true, sub: "Recovered through admin release." },
      ];
    case "FAILED":
      return [
        baseAttack,
        baseDefense,
        { ...baseRecovery, active: true, sub: "Puzzle expired or attempts exhausted." },
      ];
    case "NORMAL":
    case null:
    default:
      return [
        baseAttack,
        baseDefense,
        { ...baseRecovery, sub: "Idle — no recovery needed." },
      ];
  }
}
