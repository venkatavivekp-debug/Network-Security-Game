import { useEffect, useRef, useState } from "react";
import { adminApi } from "../api/admin";
import { ApiError, adminStepUp } from "../api/client";
import type {
  AdminConfirmationStatus,
  AuditEventView,
  HeldMessageView,
  RecoveryPolicyEntry,
  SystemPressureResponse,
  UserAtRiskView,
} from "../api/types";
import { useAuth } from "../state/auth/AuthProvider";
import { ThreatBanner } from "../components/cyber/ThreatBanner";
import { NetworkViz, type NetworkEdge, type NetworkNode } from "../components/cyber/NetworkViz";
import { AdminStepUpModal } from "../components/cyber/AdminStepUpModal";
import { RiskPolicyPanel } from "../components/cyber/RiskPolicyPanel";
import { ExternalThreatPanel } from "../components/cyber/ExternalThreatPanel";

const ALERT_TONE: Record<string, string> = {
  ADAPTIVE_ESCALATION: "tone-attack",
  PUZZLE_SOLVE_FAILURE: "tone-attack",
  PUZZLE_SOLVE_SUCCESS: "tone-defense",
  MESSAGE_RELEASE: "tone-recovery",
  MESSAGE_HOLD: "tone-recovery",
  MESSAGE_SEND: "tone-info",
  MESSAGE_DECRYPT: "tone-info",
  MESSAGE_UNLOCKED: "tone-defense",
  SESSION_REGENERATED: "tone-info",
  SESSION_ANOMALY: "tone-attack",
  RATE_LIMIT_BLOCKED: "tone-attack",
  AUTH_LOGIN_FAILURE: "tone-attack",
  AUTH_ACCOUNT_LOCKED: "tone-attack",
  AUTH_LOGOUT: "tone-info",
};

const CRITICAL_EVENTS = new Set([
  "ADAPTIVE_ESCALATION",
  "MESSAGE_HOLD",
  "PUZZLE_SOLVE_FAILURE",
  "SESSION_ANOMALY",
  "RATE_LIMIT_BLOCKED",
  "AUTH_ACCOUNT_LOCKED",
]);

export function AdminPage() {
  const { user } = useAuth();
  const [held, setHeld] = useState<HeldMessageView[]>([]);
  const [risk, setRisk] = useState<UserAtRiskView[]>([]);
  const [audit, setAudit] = useState<AuditEventView[]>([]);
  const [suspicious, setSuspicious] = useState<AuditEventView[]>([]);
  const [recoveryPolicy, setRecoveryPolicy] = useState<RecoveryPolicyEntry[]>([]);
  const [intensity, setIntensity] = useState<number>(0);
  const [pressure, setPressure] = useState<SystemPressureResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<AdminConfirmationStatus | null>(null);
  const [stepUp, setStepUp] = useState<{ open: boolean; reason?: string }>({ open: false });
  const pendingActionRef = useRef<null | (() => Promise<void>)>(null);

  const isAdmin = user?.role === "ADMIN";

  async function refresh() {
    setBusy(true);
    setNotice(null);
    try {
      const [h, u, a, t, p, s, rp, cs] = await Promise.all([
        adminApi.heldMessages(),
        adminApi.usersAtRisk(),
        adminApi.recentAudit(),
        adminApi.threatLevel(),
        adminApi.systemPressure().catch(() => null),
        adminApi.suspiciousSessions().catch(() => [] as AuditEventView[]),
        adminApi.recoveryPolicy().catch(() => [] as RecoveryPolicyEntry[]),
        adminApi.confirmationStatus().catch(() => null),
      ]);
      setHeld(h);
      setRisk(u);
      setAudit(a.slice(0, 20));
      setIntensity(t.attackIntensity01 ?? 0);
      setPressure(p);
      setSuspicious(s.slice(0, 12));
      setRecoveryPolicy(rp);
      setConfirmation(cs);
      if (!cs?.active) adminStepUp.clear();
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Could not load admin data.");
    } finally {
      setBusy(false);
    }
  }

  /**
   * Run a sensitive admin action. If the backend reports that step-up is
   * required, open the modal, remember the action, and retry on confirmation.
   */
  async function runWithStepUp(reason: string, action: () => Promise<void>) {
    try {
      await action();
    } catch (err) {
      if (err instanceof ApiError && err.isAdminStepUpRequired()) {
        pendingActionRef.current = action;
        setStepUp({ open: true, reason });
        return;
      }
      setNotice(err instanceof ApiError ? err.message : "Action failed.");
    }
  }

  useEffect(() => {
    if (!isAdmin) return;
    void refresh();
    const id = setInterval(() => {
      void refresh();
    }, 6000);
    return () => clearInterval(id);
  }, [isAdmin]);

  if (!isAdmin) {
    return (
      <div className="cc-page">
        <div className="cc-notice">This page is for ADMIN accounts only.</div>
      </div>
    );
  }

  async function release(messageId: number) {
    await runWithStepUp(`Release held message #${messageId}`, async () => {
      await adminApi.release(messageId);
      setNotice(`Message #${messageId} released.`);
      await refresh();
    });
  }

  async function reset(username: string) {
    await runWithStepUp(`Reset failure counters for ${username}`, async () => {
      await adminApi.resetFailures(username);
      setNotice(`Failure counters reset for ${username}.`);
      await refresh();
    });
  }

  async function setThreat(value: number) {
    await runWithStepUp(`Change global threat level to ${value.toFixed(2)}`, async () => {
      await adminApi.setThreatLevel(value);
      setIntensity(value);
    });
  }

  async function onStepUpConfirmed() {
    setStepUp({ open: false });
    setNotice("Admin step-up confirmed. Retrying action…");
    const pending = pendingActionRef.current;
    pendingActionRef.current = null;
    if (pending) {
      try {
        await pending();
      } catch (err) {
        setNotice(err instanceof ApiError ? err.message : "Action failed after confirmation.");
      }
    }
    await refresh();
  }

  return (
    <div className="cc-page">
      <ThreatBanner snapshot={pressure} />
      <AdminStepUpModal
        open={stepUp.open}
        reason={stepUp.reason}
        onClose={() => setStepUp({ open: false })}
        onConfirmed={() => void onStepUpConfirmed()}
      />
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Security Operations Center</div>
          <div className="cc-panel-actions">
            <span
              className={`cc-chip ${confirmation?.active ? "cc-chip--ok" : "cc-chip--muted"}`}
              title={
                confirmation?.active
                  ? `Step-up active${confirmation.expiresAt ? ` until ${new Date(confirmation.expiresAt).toLocaleTimeString()}` : ""}`
                  : "Step-up not active. Sensitive actions will prompt for password."
              }
            >
              {confirmation?.active ? "Step-up: active" : "Step-up: required for sensitive actions"}
            </span>
            <button className="cc-btn cc-btn--ghost" onClick={() => void refresh()} disabled={busy}>
              {busy ? "Refreshing…" : "Refresh"}
            </button>
          </div>
        </div>
        <div className="cc-panel-body cc-soc">
          {notice ? <div className="cc-notice">{notice}</div> : null}

          <div className="cc-soc__row cc-soc__row--two">
            <div className="cc-soc__card">
              <div className="cc-soc__card-title">Global threat level</div>
              <div className="cc-soc__slider">
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={intensity}
                  onChange={(e) => void setThreat(parseFloat(e.target.value))}
                />
                <code>{intensity.toFixed(2)}</code>
              </div>
              <div className="cc-soc__hint">Drives the adaptive engine's attack-intensity input.</div>
            </div>

            <div className="cc-soc__card">
              <div className="cc-soc__card-title">System pressure</div>
              {pressure ? (
                <div className="cc-soc__pressure">
                  <div><span>Level</span><strong>{pressure.level}</strong></div>
                  <div><span>Pressure</span><strong>{(pressure.pressure * 100).toFixed(0)}%</strong></div>
                  <div><span>Puzzle failures</span><strong>{pressure.details.recentPuzzleFailures} / {pressure.details.recentPuzzleAttempts}</strong></div>
                  <div><span>Users at risk</span><strong>{pressure.details.usersAtRisk}</strong></div>
                  <div><span>Escalations</span><strong>{pressure.details.recentEscalations}</strong></div>
                </div>
              ) : (
                <div className="cc-empty">Pressure not available.</div>
              )}
            </div>
          </div>

          <div className="cc-soc__card">
            <div className="cc-soc__card-title">Network status</div>
            <NetworkViz nodes={buildNodes(held, risk)} edges={buildEdges(held)} />
            <div className="cc-soc__hint">
              Compromised: users with active failures or held inboxes. Recovered: users with non-zero recovery counter.
            </div>
          </div>

          <div className="cc-soc__row cc-soc__row--two">
            <div className="cc-soc__card">
              <div className="cc-soc__card-title">
                <span className="cc-pulse-dot" /> Live alert feed
              </div>
              <ul className="cc-feed-list">
                {audit.length === 0 ? <li className="cc-feed-empty">No recent activity.</li> : null}
                {audit.slice(0, 12).map((e) => {
                  const tone = ALERT_TONE[e.eventType] ?? "tone-info";
                  const critical = CRITICAL_EVENTS.has(e.eventType);
                  return (
                    <li key={e.id} className={`cc-feed-item ${critical ? "is-critical" : ""}`}>
                      <span className="cc-feed-time">{new Date(e.createdAt).toLocaleTimeString()}</span>
                      <span className={`cc-feed-msg ${tone}`}>
                        <strong>{e.eventType.replace(/_/g, " ")}</strong>
                        {e.actorUsername ? ` · actor ${e.actorUsername}` : ""}
                        {e.subjectUsername ? ` · subject ${e.subjectUsername}` : ""}
                        {e.riskScore != null ? ` · risk ${(e.riskScore as number).toFixed(2)}` : ""}
                      </span>
                    </li>
                  );
                })}
              </ul>
            </div>

            <div className="cc-soc__card">
              <div className="cc-soc__card-title">
                <span className="cc-pulse-dot" /> Suspicious sessions
              </div>
              <ul className="cc-feed-list">
                {suspicious.length === 0 ? (
                  <li className="cc-feed-empty">No anomalies, lockouts or rate-limit blocks recorded.</li>
                ) : null}
                {suspicious.slice(0, 10).map((e) => {
                  const tone = ALERT_TONE[e.eventType] ?? "tone-attack";
                  const critical = CRITICAL_EVENTS.has(e.eventType);
                  return (
                    <li key={e.id} className={`cc-feed-item ${critical ? "is-critical" : ""}`}>
                      <span className="cc-feed-time">{new Date(e.createdAt).toLocaleTimeString()}</span>
                      <span className={`cc-feed-msg ${tone}`}>
                        <strong>{prettyEvent(e.eventType)}</strong>
                        {e.subjectUsername ? ` · ${e.subjectUsername}` : ""}
                        {e.ipHash ? ` · ip ${e.ipHash.slice(0, 8)}` : ""}
                      </span>
                    </li>
                  );
                })}
              </ul>
              <div className="cc-soc__hint">
                Session anomalies, account lockouts, and rate-limit blocks. No plaintext is ever logged.
              </div>
            </div>
          </div>

          <div className="cc-soc__row cc-soc__row--two">
            <div className="cc-soc__card">
              <div className="cc-soc__card-title">Held messages</div>
              <div className="cc-soc__list">
                {held.length === 0 ? <div className="cc-empty">No messages currently held.</div> : null}
                {held.map((h) => (
                  <div key={h.messageId} className="cc-held">
                    <div className="cc-held__head">
                      <strong>#{h.messageId}</strong>
                      <button className="cc-btn cc-btn--ghost" onClick={() => void release(h.messageId)}>
                        Release
                      </button>
                    </div>
                    <div className="cc-held__meta">
                      <span>{h.senderUsername} → {h.receiverUsername}</span>
                      <span>{h.enforcedMode ?? "—"} · {h.riskLevel ?? "—"}</span>
                    </div>
                    {h.holdReason ? <div className="cc-held__reason">{h.holdReason}</div> : null}
                  </div>
                ))}
              </div>
            </div>

            <div className="cc-soc__card">
              <div className="cc-soc__card-title">Users at risk</div>
              <div className="cc-soc__list">
                {risk.length === 0 ? <div className="cc-empty">No flagged users.</div> : null}
                {risk.map((r) => (
                  <div key={r.username} className="cc-held">
                    <div className="cc-held__head">
                      <strong>{r.username}</strong>
                      <button className="cc-btn cc-btn--ghost" onClick={() => void reset(r.username)}>
                        Reset
                      </button>
                    </div>
                    <div className="cc-held__meta">
                      <span>fail {r.consecutiveFailures} · total {r.puzzleFailures}/{r.puzzleAttempts}</span>
                      <span>avg {Math.round(r.avgSolveTimeMs)}ms · rec {r.recoveryEvents}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <ExternalThreatPanel />

          <RiskPolicyPanel />

          {recoveryPolicy.length > 0 ? (
            <div className="cc-soc__card">
              <div className="cc-soc__card-title">Recovery playbook</div>
              <div className="cc-recovery-playbook">
                {recoveryPolicy.map((p) => (
                  <div
                    key={p.state}
                    className={`cc-recovery-playbook__row ${p.terminalGood ? "is-good" : ""}`}
                  >
                    <span className="cc-recovery-playbook__state">{p.state.replace(/_/g, " ")}</span>
                    <span className="cc-recovery-playbook__summary">{p.summary}</span>
                    {p.nextSteps.length > 0 ? (
                      <ul className="cc-recovery-playbook__steps">
                        {p.nextSteps.map((s, i) => (
                          <li key={i}>{s}</li>
                        ))}
                      </ul>
                    ) : (
                      <span className="cc-recovery-playbook__steps cc-recovery-playbook__steps--empty">
                        Terminal good state — no further action required.
                      </span>
                    )}
                  </div>
                ))}
              </div>
              <div className="cc-soc__hint">
                Every recovery state has an explicit next step. No dead-ends.
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function prettyEvent(eventType: string): string {
  switch (eventType) {
    case "SESSION_ANOMALY":
      return "Session anomaly";
    case "SESSION_REGENERATED":
      return "Session regenerated";
    case "RATE_LIMIT_BLOCKED":
      return "Rate-limit blocked";
    case "AUTH_LOGIN_FAILURE":
      return "Login failure";
    case "AUTH_ACCOUNT_LOCKED":
      return "Account locked";
    case "AUTH_LOGOUT":
      return "Logout";
    default:
      return eventType.replace(/_/g, " ");
  }
}

function buildNodes(held: HeldMessageView[], risk: UserAtRiskView[]): NetworkNode[] {
  const nodes = new Map<string, NetworkNode>();

  held.forEach((h) => {
    if (h.senderUsername) ensure(nodes, h.senderUsername);
    if (h.receiverUsername) ensure(nodes, h.receiverUsername).state = "compromised";
  });

  risk.forEach((r) => {
    const node = ensure(nodes, r.username);
    if (r.consecutiveFailures > 0 || r.puzzleFailures > 0) {
      node.state = "compromised";
    } else if (r.recoveryEvents > 0) {
      node.state = pickBetter(node.state, "recovered");
    }
  });

  return Array.from(nodes.values()).slice(0, 12);
}

function buildEdges(held: HeldMessageView[]): NetworkEdge[] {
  return held
    .filter((h) => h.senderUsername && h.receiverUsername)
    .map((h) => ({ a: h.senderUsername!, b: h.receiverUsername!, cut: true }));
}

function ensure(map: Map<string, NetworkNode>, id: string): NetworkNode {
  let n = map.get(id);
  if (!n) {
    n = { id, label: id.length > 8 ? id.slice(0, 8) + "…" : id, state: "stable" };
    map.set(id, n);
  }
  return n;
}

function pickBetter(a: NetworkNode["state"], b: NetworkNode["state"]): NetworkNode["state"] {
  if (a === "compromised") return a;
  return b;
}
