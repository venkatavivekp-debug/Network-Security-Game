import { useEffect, useState } from "react";
import { adminApi } from "../api/admin";
import { ApiError } from "../api/client";
import type { AuditEventView, HeldMessageView, UserAtRiskView } from "../api/types";
import { useAuth } from "../state/auth/AuthProvider";

export function AdminPage() {
  const { user } = useAuth();
  const [held, setHeld] = useState<HeldMessageView[]>([]);
  const [risk, setRisk] = useState<UserAtRiskView[]>([]);
  const [audit, setAudit] = useState<AuditEventView[]>([]);
  const [intensity, setIntensity] = useState<number>(0);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const isAdmin = user?.role === "ADMIN";

  async function refresh() {
    setBusy(true);
    setNotice(null);
    try {
      const [h, u, a, t] = await Promise.all([
        adminApi.heldMessages(),
        adminApi.usersAtRisk(),
        adminApi.recentAudit(),
        adminApi.threatLevel(),
      ]);
      setHeld(h);
      setRisk(u);
      setAudit(a.slice(0, 50));
      setIntensity(t.attackIntensity01 ?? 0);
    } catch (err) {
      setNotice(err instanceof ApiError ? err.message : "Could not load admin data.");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    if (isAdmin) void refresh();
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
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Security Operations Center</div>
          <button className="cc-btn cc-btn--ghost" onClick={() => void refresh()} disabled={busy}>
            Refresh
          </button>
        </div>
        <div className="cc-panel-body" style={{ display: "grid", gap: 14 }}>
          {notice ? <div className="cc-notice">{notice}</div> : null}

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
            <strong>Users at risk</strong>
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
            <strong>Recent audit events</strong>
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
