import { useEffect, useState } from "react";
import { adminApi } from "../../api/admin";
import { ApiError } from "../../api/client";
import type { AdminRiskPolicyResponse } from "../../api/types";

/**
 * Read-only view of the adaptive risk policy. The backend owns the rule table;
 * this panel just renders it so the SOC operator can see thresholds, signal
 * weights, level actions, and the engine's stated limitations.
 */
export function RiskPolicyPanel() {
  const [policy, setPolicy] = useState<AdminRiskPolicyResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    adminApi
      .riskPolicy()
      .then((p) => {
        if (!cancelled) setPolicy(p);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : "Could not load risk policy.");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="cc-soc__card">
      <div className="cc-soc__card-title" onClick={() => setOpen((v) => !v)} style={{ cursor: "pointer" }}>
        Adaptive risk policy {open ? "▾" : "▸"}
      </div>
      {!open ? (
        <div className="cc-soc__hint">
          Tap to expand. Heuristic rule table — not a Nash/SPE solver.
        </div>
      ) : null}
      {open ? (
        error ? (
          <div className="cc-notice cc-notice--error">{error}</div>
        ) : !policy ? (
          <div className="cc-empty">Loading policy…</div>
        ) : (
          <div className="cc-policy">
            <p className="cc-policy__desc">{policy.description}</p>

            <div className="cc-policy__section">
              <div className="cc-policy__heading">Thresholds</div>
              <div className="cc-policy__grid">
                {Object.entries(policy.thresholds).map(([k, v]) => (
                  <div key={k} className="cc-policy__cell">
                    <span>{k}</span>
                    <strong>{typeof v === "number" ? v.toFixed(2) : String(v)}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="cc-policy__section">
              <div className="cc-policy__heading">Signals (weighted)</div>
              <ul className="cc-policy__list">
                {policy.signals.map((s) => (
                  <li key={s.name}>
                    <strong>{s.name}</strong>
                    {s.weight !== undefined ? <span className="cc-chip cc-chip--muted"> w={String(s.weight)}</span> : null}
                    <p>{s.description}</p>
                  </li>
                ))}
              </ul>
            </div>

            <div className="cc-policy__section">
              <div className="cc-policy__heading">Level actions</div>
              <ul className="cc-policy__list">
                {policy.levelActions.map((la) => (
                  <li key={la.level}>
                    <strong>{la.level}</strong>
                    <p>{la.action}</p>
                  </li>
                ))}
              </ul>
            </div>

            {policy.connectionStateContribution ? (
              <div className="cc-policy__section">
                <div className="cc-policy__heading">Connection state contribution</div>
                <ul className="cc-policy__list">
                  {Object.entries(policy.connectionStateContribution).map(([k, v]) => (
                    <li key={k}>
                      <strong>{k}</strong>
                      <p>{String(v)}</p>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            <div className="cc-policy__section">
              <div className="cc-policy__heading">Honest limitations</div>
              <ul className="cc-policy__list cc-policy__list--limits">
                {policy.limitations.map((l, i) => (
                  <li key={i}>{l}</li>
                ))}
              </ul>
            </div>
          </div>
        )
      ) : null}
    </div>
  );
}
