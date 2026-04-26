import { useMemo } from "react";

export interface NetworkNode {
  id: string;
  state: "stable" | "compromised" | "recovered";
  label?: string;
}

export interface NetworkEdge {
  a: string;
  b: string;
  cut?: boolean;
}

interface PositionedNode extends NetworkNode {
  x: number;
  y: number;
}

const COLORS: Record<NetworkNode["state"], string> = {
  stable: "rgba(86, 204, 242, 0.95)",
  compromised: "rgba(255, 93, 108, 0.95)",
  recovered: "rgba(61, 220, 151, 0.95)",
};

const HALO: Record<NetworkNode["state"], string> = {
  stable: "rgba(86, 204, 242, 0.18)",
  compromised: "rgba(255, 93, 108, 0.22)",
  recovered: "rgba(61, 220, 151, 0.18)",
};

export function NetworkViz({
  nodes,
  edges,
  height = 260,
}: {
  nodes: NetworkNode[];
  edges: NetworkEdge[];
  height?: number;
}) {
  const positioned = useMemo<PositionedNode[]>(() => layoutCircle(nodes), [nodes]);

  const edgeLines = useMemo(() => {
    const map = new Map(positioned.map((n) => [n.id, n] as const));
    return edges
      .map((e) => {
        const a = map.get(e.a);
        const b = map.get(e.b);
        if (!a || !b) return null;
        return { a, b, cut: !!e.cut };
      })
      .filter((x): x is { a: PositionedNode; b: PositionedNode; cut: boolean } => x != null);
  }, [edges, positioned]);

  if (nodes.length === 0) {
    return (
      <div className="cc-netviz cc-netviz--empty" style={{ height }}>
        <span>No active sessions in the SOC view.</span>
      </div>
    );
  }

  return (
    <div className="cc-netviz" style={{ height }}>
      <svg viewBox="0 0 200 120" preserveAspectRatio="xMidYMid meet">
        <defs>
          <radialGradient id="cc-netviz-bg" cx="50%" cy="50%" r="60%">
            <stop offset="0%" stopColor="rgba(100, 181, 255, 0.08)" />
            <stop offset="100%" stopColor="rgba(0,0,0,0)" />
          </radialGradient>
        </defs>
        <rect x="0" y="0" width="200" height="120" fill="url(#cc-netviz-bg)" />

        {edgeLines.map((e, i) => (
          <line
            key={`e-${i}`}
            className={`cc-netviz__edge ${e.cut ? "is-cut" : ""}`}
            x1={e.a.x}
            y1={e.a.y}
            x2={e.b.x}
            y2={e.b.y}
          />
        ))}

        {positioned.map((n) => (
          <g key={n.id} className={`cc-netviz__node is-${n.state}`}>
            <circle className="cc-netviz__halo" cx={n.x} cy={n.y} r={6.6} fill={HALO[n.state]} />
            <circle
              className="cc-netviz__core"
              cx={n.x}
              cy={n.y}
              r={3.2}
              fill={COLORS[n.state]}
              stroke="rgba(7, 11, 18, 0.85)"
              strokeWidth={0.6}
            />
            <text
              x={n.x}
              y={n.y + 9.4}
              textAnchor="middle"
              fontSize="3.3"
              fill="rgba(214, 237, 255, 0.85)"
              fontWeight={700}
            >
              {n.label ?? n.id}
            </text>
          </g>
        ))}
      </svg>
      <div className="cc-netviz__legend">
        <span><i style={{ background: COLORS.stable }} /> stable</span>
        <span><i style={{ background: COLORS.compromised }} /> compromised</span>
        <span><i style={{ background: COLORS.recovered }} /> recovered</span>
      </div>
    </div>
  );
}

function layoutCircle(nodes: NetworkNode[]): PositionedNode[] {
  const cx = 100;
  const cy = 60;
  const r = 42;
  if (nodes.length === 0) return [];
  if (nodes.length === 1) return [{ ...nodes[0], x: cx, y: cy }];
  return nodes.map((n, i) => {
    const angle = (i / nodes.length) * Math.PI * 2 - Math.PI / 2;
    return { ...n, x: cx + Math.cos(angle) * r, y: cy + Math.sin(angle) * r };
  });
}
