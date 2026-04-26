export type Phase = "attack" | "defense" | "recovery";

export interface PhaseStep {
  phase: Phase;
  title: string;
  sub?: string;
  active?: boolean;
}

export function AttackTimeline({ steps }: { steps: PhaseStep[] }) {
  return (
    <div className="cc-timeline">
      {steps.map((s, i) => (
        <div
          key={`${s.phase}-${i}`}
          className={`cc-timeline__step is-${s.phase} ${s.active ? "is-active" : ""}`}
        >
          <div className="cc-timeline__phase">{s.phase}</div>
          <div className="cc-timeline__title">{s.title}</div>
          {s.sub ? <div className="cc-timeline__sub">{s.sub}</div> : null}
        </div>
      ))}
    </div>
  );
}
