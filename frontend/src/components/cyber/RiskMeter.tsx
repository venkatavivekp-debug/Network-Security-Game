import type { RiskLevel } from "../../api/types";

const LEVEL_COPY: Record<RiskLevel, string> = {
  LOW: "Stable",
  ELEVATED: "Watch",
  HIGH: "Elevated",
  CRITICAL: "Critical",
};

const LEVEL_CLASS: Record<RiskLevel, string> = {
  LOW: "is-low",
  ELEVATED: "is-watch",
  HIGH: "is-high",
  CRITICAL: "is-critical",
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
  const cls = level ? LEVEL_CLASS[level] : "is-low";
  return (
    <div className={`cc-risk-meter ${cls}`}>
      <div className="cc-risk-meter__row">
        <span className="cc-risk-meter__label">Risk</span>
        <span className="cc-risk-meter__value">
          {level ? LEVEL_COPY[level] : "—"}
          {score == null ? "" : ` · ${score.toFixed(2)}`}
        </span>
      </div>
      <div className="cc-risk-meter__bar">
        <div className="cc-risk-meter__fill" style={{ width: `${pct.toFixed(1)}%` }} />
      </div>
      {caption ? <div className="cc-risk-meter__caption">{caption}</div> : null}
    </div>
  );
}
