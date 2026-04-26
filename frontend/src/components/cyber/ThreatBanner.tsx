import type { SystemPressureResponse } from "../../api/types";

const COPY: Record<SystemPressureResponse["level"], { tone: string; title: string; sub: string }> = {
  CALM: { tone: "is-watch", title: "All systems calm", sub: "No active escalation. Background telemetry is steady." },
  WATCH: { tone: "is-watch", title: "Watch", sub: "Activity above baseline. Adaptive engine is monitoring." },
  ELEVATED: {
    tone: "is-elevated",
    title: "Elevated threat",
    sub: "Puzzle pressure or admin signals are pushing the policy upward.",
  },
  CRITICAL: {
    tone: "is-critical",
    title: "Critical pressure",
    sub: "Multiple signals stacking — expect step-up enforcement and held messages.",
  },
};

export function ThreatBanner({ snapshot }: { snapshot: SystemPressureResponse | null }) {
  if (!snapshot) return null;
  const meta = COPY[snapshot.level];
  return (
    <div className={`cc-threat-banner ${meta.tone}`}>
      <span className="cc-pulse-dot" aria-hidden />
      <div style={{ display: "grid", gap: 2 }}>
        <span style={{ fontWeight: 950 }}>{meta.title}</span>
        <span style={{ color: "var(--cc-muted)", fontWeight: 700, fontSize: 12 }}>{meta.sub}</span>
      </div>
      <span style={{ marginLeft: "auto", fontVariantNumeric: "tabular-nums", fontWeight: 900 }}>
        pressure {(snapshot.pressure * 100).toFixed(0)}%
      </span>
    </div>
  );
}
