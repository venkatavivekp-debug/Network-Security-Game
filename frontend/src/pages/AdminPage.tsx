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
      setAudit(a.slice(0, 50));
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
      await refresh();
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Release failed.");
    }
  }

  async function reset(username: string) {
    try {
      await adminApi.resetFailures(username);
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
    <div className="cc-page" style={{ display: "grid", gap: 16 }}>
      <ThreatBanner snapshot={pressure} />
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Security Operations Center</div>
          <button className="cc-btn cc-btn--ghost" onClick={() => void refresh()} disabled={busy}>
            {busy ? "Refreshing…" : "Refresh"}
          </button>
        </div>
        <div className="cc-panel-body" style={{ display: "grid", gap: 14 }}>
          {notice ? <div className="cc-notice">{notice}</div> : null}

          <div style={{ display: "grid", gap: 12, gridTemplateColumns: "1fr 1fr" }}>
            <div className="cc-surface" style={{ padding: 12, display: "grid", gap: 8 }}>
              <strong>Global threat level</strong>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={intensity}
                  onChange={(e) => void setThreat(parseFloat(e.target.value))}
                  style={{ flex: 1 }}
                />
                <code>{intensity.toFixed(2)}</code>
              </div>
              <div style={{ color: "var(--cc-muted)", fontSize: 12 }}>
                Drives the adaptive engine's attack intensity input.
              </div>
            </div>

            <div className="cc-surface" style={{ padding: 12, display: "grid", gap: 8 }}>
              <strong>System pressure</strong>
              {pressure ? (
                <>
                  <div className="cc-result-row">
                    <span>Level</span>
                    <span>{pressure.level}</span>
                  </div>
                  <div className="cc-result-row">
                    <span>Pressure</span>
                    <span>{(pressure.pressure * 100).toFixed(0)}%</span>
                  </div>
                  <div className="cc-result-row">
                    <span>Recent puzzle failures</span>
                    <span>
                      {pressure.details.recentPuzzleFailures} / {pressure.details.recentPuzzleAttempts}
                    </span>
                  </div>
                  <div className="cc-result-row">
                    <span>Users at risk</span>
                    <span>{pressure.details.usersAtRisk}</span>
                  </div>
                  <div className="cc-result-row">
                    <span>Recent escalations</span>
                    <span>{pressure.details.recentEscalations}</span>
                  </div>
                </>
              ) : (
                <div className="cc-empty">Pressure not available.</div>
              )}
            </div>
          </div>

          <div className="cc-surface" style={{ padding: 12, display: "grid", gap: 10 }}>
            <strong>Network status map</strong>
            <NetworkViz nodes={buildNodes(held, risk)} edges={buildEdges(held)} />
            <div style={{ color: "var(--cc-muted)", fontSize: 12 }}>
              Synthesized from held messages and users-at-risk. Compromised nodes are users with active failures or
              held inboxes; recovered nodes are users whose recovery counter is non-zero.
            </div>
          </div>

          <div className="cc-surface" style={{ padding: 12 }}>
            <strong>
              <span className="cc-pulse-dot" /> Live alert feed
            </strong>
            <ul className="cc-feed-list" style={{ marginTop: 8 }}>
              {audit.length === 0 ? <li className="cc-feed-empty">No recent activity.</li> : null}
              {audit.slice(0, 12).map((e) => (
                <li key={e.id} className="cc-feed-item">
                  <span className="cc-feed-time">{new Date(e.createdAt).toLocaleTimeString()}</span>
                  <span className={`cc-feed-msg ${ALERT_TONE[e.eventType] ?? "tone-info"}`}>
                    <strong>{e.eventType.replace(/_/g, " ")}</strong>
                    {e.actorUsername ? ` · actor ${e.actorUsername}` : ""}
                    {e.subjectUsername ? ` · subject ${e.subjectUsername}` : ""}
                    {e.riskScore != null ? ` · risk ${(e.riskScore as number).toFixed(2)}` : ""}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="cc-surface" style={{ padding: 12 }}>
            <strong>Held messages</strong>
            <div style={{ marginTop: 8, display: "grid", gap: 8 }}>
              {held.length === 0 ? <div className="cc-empty">No messages currently held.</div> : null}
              {held.map((h) => (
                <div key={h.messageId} className="cc-result-row" style={{ flexDirection: "column", alignItems: "stretch", gap: 4 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                    <strong>#{h.messageId}</strong>
                    <button className="cc-btn cc-btn--ghost" onClick={() => void release(h.messageId)}>
                      Release
                    </button>
                  </div>
                  <div style={{ color: "var(--cc-muted)", fontSize: 12 }}>
                    {h.senderUsername} → {h.receiverUsername} · enforced: {h.enforcedMode ?? "?"} · risk: {h.riskLevel ?? "?"} · reason: {h.holdReason ?? "?"}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="cc-surface" style={{ padding: 12 }}>
            <strong>Suspicious activity (users at risk)</strong>
            <div style={{ marginTop: 8, display: "grid", gap: 8 }}>
              {risk.length === 0 ? <div className="cc-empty">No flagged users.</div> : null}
              {risk.map((r) => (
                <div key={r.username} className="cc-result-row" style={{ flexDirection: "column", alignItems: "stretch", gap: 4 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                    <strong>{r.username}</strong>
                    <button className="cc-btn cc-btn--ghost" onClick={() => void reset(r.username)}>
                      Reset failures
                    </button>
                  </div>
                  <div style={{ color: "var(--cc-muted)", fontSize: 12 }}>
                    consecutive: {r.consecutiveFailures} · failures: {r.puzzleFailures} / attempts: {r.puzzleAttempts}
                    · avg solve: {Math.round(r.avgSolveTimeMs)} ms · recoveries: {r.recoveryEvents}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="cc-surface" style={{ padding: 12 }}>
            <strong>Audit log</strong>
            <div style={{ marginTop: 8, maxHeight: 320, overflow: "auto", display: "grid", gap: 4 }}>
              {audit.length === 0 ? <div className="cc-empty">No audit events.</div> : null}
              {audit.map((e) => (
                <div key={e.id} style={{ fontFamily: "ui-monospace, Menlo, monospace", fontSize: 12 }}>
                  <span style={{ color: "var(--cc-muted)" }}>{new Date(e.createdAt).toLocaleTimeString()}</span>{" "}
                  <strong>{e.eventType}</strong>{" "}
                  {e.actorUsername ? `actor=${e.actorUsername} ` : ""}
                  {e.subjectUsername ? `subject=${e.subjectUsername}` : ""}
                </div>
              ))}
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
    if (h.senderUsername) ensure(nodes, h.senderUsername).state = pickWorse(ensure(nodes, h.senderUsername).state, "stable");
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

  // Limit to 12 nodes for the small visualization.
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

function pickWorse(a: NetworkNode["state"], b: NetworkNode["state"]): NetworkNode["state"] {
  const order = { stable: 0, recovered: 1, compromised: 2 } as const;
  return order[a] >= order[b] ? a : b;
}

function pickBetter(a: NetworkNode["state"], b: NetworkNode["state"]): NetworkNode["state"] {
  if (a === "compromised") return a;
  return b;
}
