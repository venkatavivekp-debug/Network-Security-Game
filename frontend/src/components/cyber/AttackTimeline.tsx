export type Phase = "attack" | "defense" | "recovery";

export interface PhaseStep {
  phase: Phase;
  title: string;
  sub?: string;
  active?: boolean;
}

const PHASE_LABEL: Record<Phase, string> = {
  attack: "Attack",
  defense: "Defense",
  recovery: "Recovery",
};

export function AttackTimeline({ steps }: { steps: PhaseStep[] }) {
  return (
    <div className="cc-timeline" role="list">
      {steps.map((s, i) => (
        <div
          key={`${s.phase}-${i}`}
          role="listitem"
          className={`cc-timeline__step is-${s.phase} ${s.active ? "is-active" : ""}`}
        >
          <div className="cc-timeline__phase">
            <span className="cc-timeline__dot" aria-hidden />
            {PHASE_LABEL[s.phase]}
          </div>
          <div className="cc-timeline__title">{s.title}</div>
          {s.sub ? <div className="cc-timeline__sub">{s.sub}</div> : null}
        </div>
      ))}
    </div>
  );
}
