import type { RiskLevel } from "../../api/types";

const LEVEL_COPY: Record<RiskLevel, string> = {
  LOW: "Stable",
  ELEVATED: "Watch",
  HIGH: "Elevated",
  CRITICAL: "Critical",
};

export function RiskMeter({
  score,
  level,
  caption,
}: {
  score: number | null;
  level: RiskLevel | null;
  caption?: string;
}) {
  const pct = Math.max(0, Math.min(1, score ?? 0)) * 100;
  return (
    <div className="cc-risk-meter">
      <div className="cc-risk-meter__row">
        <span className="cc-risk-meter__label">Risk</span>
        <span className="cc-risk-meter__value">
          {level ? LEVEL_COPY[level] : "—"} {score == null ? "" : `(${score.toFixed(2)})`}
        </span>
      </div>
      <div className="cc-risk-meter__bar">
        <div className="cc-risk-meter__fill" style={{ width: `${pct.toFixed(1)}%` }} />
      </div>
      {caption ? (
        <div className="cc-risk-meter__row" style={{ textTransform: "none", letterSpacing: "0.02em" }}>
          <span style={{ color: "var(--cc-muted)" }}>{caption}</span>
        </div>
      ) : null}
    </div>
  );
}
