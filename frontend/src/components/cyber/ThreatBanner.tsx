import type { SystemPressureResponse } from "../../api/types";

const COPY: Record<SystemPressureResponse["level"], { tone: string; title: string; sub: string }> = {
  CALM: {
    tone: "is-watch",
    title: "All systems calm",
    sub: "No active escalation. Telemetry steady.",
  },
  WATCH: {
    tone: "is-watch",
    title: "Watch",
    sub: "Activity above baseline. Engine is monitoring.",
  },
  ELEVATED: {
    tone: "is-elevated",
    title: "Elevated threat",
    sub: "Adaptive policy is stepping up enforcement.",
  },
  CRITICAL: {
    tone: "is-critical",
    title: "Critical pressure",
    sub: "Step-up enforced. Some messages may be held.",
  },
};

export function ThreatBanner({ snapshot }: { snapshot: SystemPressureResponse | null }) {
  if (!snapshot) return null;
  const meta = COPY[snapshot.level];
  const reason = inferReason(snapshot);
  return (
    <div className={`cc-threat-banner ${meta.tone}`}>
      <span className="cc-pulse-dot" aria-hidden />
      <div className="cc-threat-banner__body">
        <span className="cc-threat-banner__title">{meta.title}</span>
        <span className="cc-threat-banner__sub">
          {meta.sub}
          {reason ? ` · ${reason}` : ""}
        </span>
      </div>
      <span className="cc-threat-banner__pressure">
        {(snapshot.pressure * 100).toFixed(0)}%
      </span>
    </div>
  );
}

function inferReason(snap: SystemPressureResponse): string | null {
  const d = snap.details;
  if (d.threatLevel >= 0.7) return `threat ${d.threatLevel.toFixed(2)}`;
  if (d.recentPuzzleFailures > 0 && d.puzzleFailureRate >= 0.4) {
    return `puzzle failure rate ${(d.puzzleFailureRate * 100).toFixed(0)}%`;
  }
  if (d.usersAtRisk > 0) return `${d.usersAtRisk} user${d.usersAtRisk === 1 ? "" : "s"} at risk`;
  if (d.recentEscalations > 0) return `${d.recentEscalations} recent escalation${d.recentEscalations === 1 ? "" : "s"}`;
  return null;
}
