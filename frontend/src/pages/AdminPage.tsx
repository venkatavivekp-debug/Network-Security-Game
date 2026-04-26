import { useEffect, useState } from "react";
import { adminApi } from "../api/admin";
import { ApiError } from "../api/client";
import type {
  AuditEventView,
  HeldMessageView,
  SystemPressureResponse,
  UserAtRiskView,
} from "../api/types";
import { useAuth } from "../state/auth/AuthProvider";
import { ThreatBanner } from "../components/cyber/ThreatBanner";
import { NetworkViz, type NetworkEdge, type NetworkNode } from "../components/cyber/NetworkViz";

const ALERT_TONE: Record<string, string> = {
  ADAPTIVE_ESCALATION: "tone-attack",
  PUZZLE_SOLVE_FAILURE: "tone-attack",
  PUZZLE_SOLVE_SUCCESS: "tone-defense",
  MESSAGE_RELEASE: "tone-recovery",
  MESSAGE_HOLD: "tone-recovery",
  MESSAGE_SEND: "tone-info",
  MESSAGE_DECRYPT: "tone-info",
};

const CRITICAL_EVENTS = new Set(["ADAPTIVE_ESCALATION", "MESSAGE_HOLD", "PUZZLE_SOLVE_FAILURE"]);

export function AdminPage() {
  const { user } = useAuth();
  const [held, setHeld] = useState<HeldMessageView[]>([]);
  const [risk, setRisk] = useState<UserAtRiskView[]>([]);
  const [audit, setAudit] = useState<AuditEventView[]>([]);
  const [intensity, setIntensity] = useState<number>(0);
  const [pressure, setPressure] = useState<SystemPressureResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const isAdmin = user?.role === "ADMIN";

  async function refresh() {
    setBusy(true);
    setNotice(null);
    try {
      const [h, u, a, t, p] = await Promise.all([
        adminApi.heldMessages(),
        adminApi.usersAtRisk(),
        adminApi.recentAudit(),
        adminApi.threatLevel(),
        adminApi.systemPressure().catch(() => null),
      ]);
      setHeld(h);
      setRisk(u);
      setAudit(a.slice(0, 20));
      setIntensity(t.attackIntensity01 ?? 0);
      setPressure(p);
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Could not load admin data.");
    } finally {
      setBusy(false);
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
    try {
      await adminApi.release(messageId);
      setNotice(`Message #${messageId} released.`);
      await refresh();
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Release failed.");
    }
  }

  async function reset(username: string) {
    try {
      await adminApi.resetFailures(username);
      setNotice(`Failure counters reset for ${username}.`);
      await refresh();
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Reset failed.");
    }
  }

  async function setThreat(value: number) {
    try {
      await adminApi.setThreatLevel(value);
      setIntensity(value);
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Threat level update failed.");
    }
  }

  return (
    <div className="cc-page">
      <ThreatBanner snapshot={pressure} />
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Security Operations Center</div>
          <button className="cc-btn cc-btn--ghost" onClick={() => void refresh()} disabled={busy}>
            {busy ? "Refreshing…" : "Refresh"}
          </button>
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
        </div>
      </section>
    </div>
  );
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
