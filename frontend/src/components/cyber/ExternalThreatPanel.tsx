import { useEffect, useState } from "react";
import { adminApi } from "../../api/admin";
import { ApiError } from "../../api/client";
import type { ExternalThreatSummary } from "../../api/types";

/**
 * Outside-threat protection card for the SOC console. Aggregates rate-limit
 * blocks, forbidden-access attempts, validation rejections, session anomalies
 * and puzzle failures over a rolling window. Pulls from the audit log so
 * everything shown is already redacted server-side.
 */
export function ExternalThreatPanel() {
  const [data, setData] = useState<ExternalThreatSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    adminApi
      .externalThreats()
      .then((s) => {
        setData(s);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : "Could not load external threats.");
      });
  };

  useEffect(() => {
    load();
    const id = window.setInterval(load, 15000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <div className="cc-soc__card">
      <div className="cc-soc__card-title">External threat protection</div>
      <div className="cc-soc__hint">
        OWASP-aligned controls in action. Window: {data?.windowMinutes ?? 60} min.
      </div>
      {error ? (
        <div className="cc-soc__hint" style={{ color: "var(--cc-warn, #f6a609)" }}>
          {error}
        </div>
      ) : null}
      {data ? (
        <>
          <div className="cc-threat-grid">
            <ThreatStat label="Blocked requests" value={data.blockedRequests} accent="alert" />
            <ThreatStat label="Rate-limit (429)" value={data.counters.rateLimitBlocked} />
            <ThreatStat label="Forbidden (403)" value={data.counters.forbiddenAccess} />
            <ThreatStat label="Validation (400)" value={data.counters.validationRejected} />
            <ThreatStat label="Session anomalies" value={data.counters.sessionAnomaly} />
            <ThreatStat label="Puzzle failures" value={data.counters.puzzleSolveFailure} />
            <ThreatStat label="Login failures" value={data.counters.loginFailure} />
          </div>
          <div className="cc-threat-events">
            {data.recent.length === 0 ? (
              <div className="cc-soc__hint">No recent external-threat events.</div>
            ) : (
              data.recent.map((e) => (
                <div key={e.id} className="cc-threat-event">
                  <span className={`cc-tag cc-tag--${eventTone(e.eventType)}`}>{prettyType(e.eventType)}</span>
                  <span className="cc-threat-event__actor">{e.actor ?? "anonymous"}</span>
                  <span className="cc-threat-event__time">
                    {new Date(e.createdAt).toLocaleTimeString()}
                  </span>
                </div>
              ))
            )}
          </div>
        </>
      ) : null}
    </div>
  );
}

interface ThreatStatProps {
  label: string;
  value: number;
  accent?: "alert" | "default";
}

function ThreatStat({ label, value, accent = "default" }: ThreatStatProps) {
  return (
    <div className={`cc-threat-stat${accent === "alert" ? " cc-threat-stat--alert" : ""}`}>
      <div className="cc-threat-stat__value">{value}</div>
      <div className="cc-threat-stat__label">{label}</div>
    </div>
  );
}

function prettyType(eventType: string): string {
  switch (eventType) {
    case "RATE_LIMIT_BLOCKED":
      return "Rate-limit";
    case "FORBIDDEN_ACCESS":
      return "Forbidden";
    case "VALIDATION_REJECTED":
      return "Validation";
    case "SESSION_ANOMALY":
      return "Session";
    case "PUZZLE_SOLVE_FAILURE":
      return "Puzzle fail";
    case "AUTH_LOGIN_FAILURE":
      return "Login fail";
    case "AUTH_ACCOUNT_LOCKED":
      return "Lockout";
    default:
      return eventType.toLowerCase().replace(/_/g, " ");
  }
}

function eventTone(eventType: string): string {
  switch (eventType) {
    case "RATE_LIMIT_BLOCKED":
    case "FORBIDDEN_ACCESS":
    case "AUTH_ACCOUNT_LOCKED":
      return "danger";
    case "VALIDATION_REJECTED":
    case "AUTH_LOGIN_FAILURE":
      return "warn";
    case "SESSION_ANOMALY":
      return "warn";
    case "PUZZLE_SOLVE_FAILURE":
      return "muted";
    default:
      return "muted";
  }
}
