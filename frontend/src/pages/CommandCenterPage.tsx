import { useEffect, useMemo, useRef, useState } from "react";
import type {
  AlgorithmType,
  SimulationRunRequest,
  SimulationRunResponse,
  SystemPressureResponse,
} from "../api/types";
import { ApiError } from "../api/client";
import { simulationApi } from "../api/simulation";
import { adminApi } from "../api/admin";
import { ThreatBanner } from "../components/cyber/ThreatBanner";

type Phase = "setup" | "attack" | "defense" | "recovery" | "results";
type NodeState = "neutral" | "stable" | "attacked" | "recovering";
type EdgeState = "active" | "disrupted";

type Node = { id: number; x: number; y: number; state: NodeState; pulse: number };
type Edge = { from: number; to: number; state: EdgeState };

type RunConfig = {
  seed: number;
  numNodes: number;
  numEdges: number;
  afterAttackConnectivity: number;
  afterRecoveryConnectivity: number;
  attackEfficiency: number;
  onPhase: (p: Phase) => void;
  onKpis: (compromise: number, resilience: number, efficiency: number) => void;
  onFeed: (msg: string, tone: "attack" | "defense" | "recovery" | "info") => void;
};

export function CommandCenterPage() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [phase, setPhase] = useState<Phase>("setup");
  const [status, setStatus] = useState<"READY" | "ENGAGED" | "COMPLETE">("READY");
  const [feed, setFeed] = useState<Array<{ t: string; msg: string; tone: "attack" | "defense" | "recovery" | "info" }>>([]);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const [numNodes, setNumNodes] = useState(10);
  const [numEdges, setNumEdges] = useState(15);
  const [attackBudget, setAttackBudget] = useState(3);
  const [defenseBudget, setDefenseBudget] = useState(3);
  const [recoveryBudget, setRecoveryBudget] = useState(2);
  const [algorithmType, setAlgorithmType] = useState<AlgorithmType>("SHCS");

  const [kpiCompromise, setKpiCompromise] = useState(0);
  const [kpiResilience, setKpiResilience] = useState(1);
  const [kpiEfficiency, setKpiEfficiency] = useState(0);
  const [lastResult, setLastResult] = useState<SimulationRunResponse | null>(null);
  const [pressure, setPressure] = useState<SystemPressureResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function tick() {
      try {
        const snap = await adminApi.systemPressure();
        if (!cancelled) setPressure(snap);
      } catch {
        // best-effort; ignore
      }
    }
    void tick();
    const id = setInterval(tick, 8000);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  const derivedSeed = useMemo(() => {
    return (((numNodes * 73856093) ^ (numEdges * 19349663) ^ (attackBudget * 83492791)) >>> 0) || 1;
  }, [numNodes, numEdges, attackBudget]);

  function pushFeed(msg: string, tone: "attack" | "defense" | "recovery" | "info") {
    setFeed((prev) => [{ t: nowClock(), msg, tone }, ...prev].slice(0, 40));
  }

  // Canvas setup + initial render
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const dpr = window.devicePixelRatio || 1;
    const parent = canvas.parentElement;
    const logicalW = parent ? Math.floor(parent.clientWidth) : 1120;
    const logicalH = 560;
    const w = Math.max(860, logicalW);

    canvas.width = Math.floor(w * dpr);
    canvas.height = Math.floor(logicalH * dpr);
    canvas.style.width = "100%";
    canvas.style.height = `${logicalH}px`;

    const ctx = canvas.getContext("2d");
    if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    const random = seededRandom(derivedSeed);
    const nodes = buildRingLayout(Math.max(2, Math.min(60, numNodes)), w, logicalH);
    const edges = buildEdges(random, nodes.length, Math.max(0, Math.min(400, numEdges)));
    drawNetwork(canvas, nodes, edges);
  }, [derivedSeed, numEdges, numNodes]);

  async function launch() {
    setNotice(null);
    setBusy(true);
    setStatus("ENGAGED");

    const request: SimulationRunRequest = { numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget, algorithmType };

    try {
      const response = await simulationApi.run(request);
      setLastResult(response);

      const canvas = canvasRef.current;
      if (!canvas) return;

      await runPhases(canvas, {
        seed: derivedSeed,
        numNodes,
        numEdges,
        afterAttackConnectivity: response.afterAttackConnectivity,
        afterRecoveryConnectivity: response.afterRecoveryConnectivity,
        attackEfficiency: response.effectiveAttackSuccessProbability,
        onPhase: setPhase,
        onKpis: (c, r, e) => {
          setKpiCompromise(c);
          setKpiResilience(r);
          setKpiEfficiency(e);
        },
        onFeed: pushFeed,
      });

      setPhase("results");
      setStatus("COMPLETE");
      pushFeed("Mission complete. Results captured and ready for persistence.", "info");
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      } else {
        setNotice("Simulation failed. Please try again.");
      }
      setPhase("setup");
      setStatus("READY");
    } finally {
      setBusy(false);
    }
  }

  async function replay() {
    setNotice(null);
    const canvas = canvasRef.current;
    if (!canvas) return;

    setBusy(true);
    setStatus("ENGAGED");
    pushFeed("Replaying last scenario locally (no persistence).", "info");

    const afterAttack = lastResult?.afterAttackConnectivity ?? 0.75;
    const afterRecovery = lastResult?.afterRecoveryConnectivity ?? 0.6;
    const eff = lastResult?.effectiveAttackSuccessProbability ?? 0.65;

    try {
      await runPhases(canvas, {
        seed: derivedSeed,
        numNodes,
        numEdges,
        afterAttackConnectivity: afterAttack,
        afterRecoveryConnectivity: afterRecovery,
        attackEfficiency: eff,
        onPhase: setPhase,
        onKpis: (c, r, e) => {
          setKpiCompromise(c);
          setKpiResilience(r);
          setKpiEfficiency(e);
        },
        onFeed: pushFeed,
      });
      setPhase("results");
      setStatus("COMPLETE");
    } finally {
      setBusy(false);
    }
  }

  function reset() {
    setNotice(null);
    setBusy(false);
    setPhase("setup");
    setStatus("READY");
    setFeed([]);
    setKpiCompromise(0);
    setKpiResilience(1);
    setKpiEfficiency(0);
    pushFeed("Mission reset. Configure parameters to begin.", "info");
  }

  return (
    <div className="cc-page">
      <ThreatBanner snapshot={pressure} />
      <section className="cc-surface cc-mission">
        <div className="cc-mission-left">
          <div className="cc-title-row">
            <div className="cc-title">Cyber Defense Command Center</div>
            <div className="cc-chip cc-chip--mode">BASE SIMULATION</div>
          </div>
          <div className="cc-subtitle">Attack → Defense → Recovery. Persistent runs use the unchanged backend REST APIs.</div>
        </div>
        <div className="cc-mission-right">
          <div className="cc-chip cc-chip--status">STATUS · {status}</div>
          <div className="cc-chip cc-chip--phase">PHASE · {phase.toUpperCase()}</div>
          <div className="cc-chip cc-chip--scenario">ALGO · {algorithmType}</div>
        </div>
      </section>

      <div className="cc-layout">
        <section className="cc-surface cc-panel cc-panel--left">
          <div className="cc-panel-header">
            <div className="cc-panel-title">Mission Control</div>
          </div>
          <div className="cc-panel-body">
            <div className="cc-section-title">Network Setup</div>
            <div className="cc-form">
              <Field label="Nodes" value={numNodes} onChange={setNumNodes} min={2} max={300} disabled={busy} />
              <Field label="Edges" value={numEdges} onChange={setNumEdges} min={0} max={50000} disabled={busy} />
            </div>

            <div className="cc-section-title">Budgets</div>
            <div className="cc-form">
              <Field label="Attack" value={attackBudget} onChange={setAttackBudget} min={0} max={10000} disabled={busy} />
              <Field label="Defense" value={defenseBudget} onChange={setDefenseBudget} min={0} max={10000} disabled={busy} />
              <Field label="Recovery" value={recoveryBudget} onChange={setRecoveryBudget} min={0} max={10000} disabled={busy} />
            </div>

            <div className="cc-section-title">Security Posture</div>
            <label className="cc-label">
              Algorithm
              <select
                className="cc-select"
                value={algorithmType}
                onChange={(e) => setAlgorithmType(e.target.value as AlgorithmType)}
                disabled={busy}
              >
                <option value="NORMAL">NORMAL</option>
                <option value="SHCS">SHCS</option>
                <option value="CPHS">CPHS</option>
              </select>
            </label>

            <div className="cc-actions">
              <button className="cc-btn" type="button" onClick={() => void launch()} disabled={busy}>
                {busy ? "Running…" : "Launch Simulation"}
              </button>
              <button className="cc-btn cc-btn--ghost" type="button" onClick={() => void replay()} disabled={busy}>
                Replay Scenario
              </button>
              <button className="cc-btn cc-btn--ghost" type="button" onClick={reset} disabled={busy}>
                Reset Mission
              </button>
            </div>

            {notice ? <div className="cc-notice">{notice}</div> : null}
          </div>
        </section>

        <section className="cc-surface cc-panel cc-panel--center">
          <div className="cc-panel-header">
            <div className="cc-panel-title">Battlefield Network</div>
          </div>
          <div className="cc-battlefield">
            <canvas ref={canvasRef} />
          </div>
        </section>

        <section className="cc-surface cc-panel cc-panel--right">
          <div className="cc-panel-header">
            <div className="cc-panel-title">Telemetry</div>
          </div>
          <div className="cc-panel-body">
            <div className="cc-kpis">
              <Kpi label="Compromise" value={format3(kpiCompromise)} hint="Approx. fraction compromised" />
              <Kpi label="Resilience" value={format3(kpiResilience)} hint="Connectivity ratio proxy" />
              <Kpi label="Attack Eff." value={format3(kpiEfficiency)} hint="Effective success probability" />
              <Kpi
                label="Pressure"
                value={pressure ? `${(pressure.pressure * 100).toFixed(0)}%` : "—"}
                hint={pressure ? `Adaptive load · ${pressure.level}` : "Live system pressure"}
              />
            </div>

            <div className="cc-section-title">Last Persisted Run</div>
            <div className="cc-results">
              {lastResult ? (
                <>
                  <ResultRow k="Run ID" v={String(lastResult.simulationRunId)} />
                  <ResultRow k="After attack" v={format3(lastResult.afterAttackConnectivity)} />
                  <ResultRow k="After recovery" v={format3(lastResult.afterRecoveryConnectivity)} />
                  <ResultRow k="Nodes lost" v={String(lastResult.nodesLost)} />
                  <ResultRow k="Edges lost" v={String(lastResult.edgesLost)} />
                </>
              ) : (
                <div className="cc-empty">No run persisted yet.</div>
              )}
            </div>
          </div>
        </section>

        <section className="cc-surface cc-panel cc-panel--bottom">
          <div className="cc-panel-header">
            <div className="cc-panel-title">Timeline / Event Feed</div>
            <div className="cc-stepper" aria-label="phase indicator">
              <span className={phase === "setup" ? "cc-step is-active" : "cc-step"}>Setup</span>
              <span className={phase === "attack" ? "cc-step is-active" : "cc-step"}>Attack</span>
              <span className={phase === "defense" ? "cc-step is-active" : "cc-step"}>Defense</span>
              <span className={phase === "recovery" ? "cc-step is-active" : "cc-step"}>Recovery</span>
              <span className={phase === "results" ? "cc-step is-active" : "cc-step"}>Results</span>
            </div>
          </div>
          <div className="cc-feed">
            <ul className="cc-feed-list">
              {feed.length ? (
                feed.map((e, idx) => (
                  <li key={idx} className="cc-feed-item">
                    <span className="cc-feed-time">{e.t}</span>
                    <span className={`cc-feed-msg tone-${e.tone}`}>{e.msg}</span>
                  </li>
                ))
              ) : (
                <li className="cc-feed-empty">No events yet. Launch a simulation to begin.</li>
              )}
            </ul>
          </div>
        </section>
      </div>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  min,
  max,
  disabled,
}: {
  label: string;
  value: number;
  onChange: (v: number) => void;
  min: number;
  max: number;
  disabled?: boolean;
}) {
  return (
    <label className="cc-label">
      {label}
      <input
        className="cc-input"
        type="number"
        value={value}
        min={min}
        max={max}
        disabled={disabled}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    </label>
  );
}

function Kpi({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="cc-kpi">
      <div className="cc-kpi-label">{label}</div>
      <div className="cc-kpi-value">{value}</div>
      <div className="cc-kpi-hint">{hint}</div>
    </div>
  );
}

function ResultRow({ k, v }: { k: string; v: string }) {
  return (
    <div className="cc-result-row">
      <span>{k}</span>
      <span>{v}</span>
    </div>
  );
}

function nowClock() {
  const d = new Date();
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${hh}:${mm}:${ss}`;
}

function clamp01(v: number) {
  if (typeof v !== "number" || !Number.isFinite(v)) return 0;
  return Math.max(0, Math.min(1, v));
}

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t;
}

function format3(value: number) {
  if (typeof value !== "number" || !Number.isFinite(value)) return "—";
  return value.toFixed(3);
}

function seededRandom(seed: number) {
  let state = (seed >>> 0) || 1;
  return function next() {
    state = (1664525 * state + 1013904223) >>> 0;
    return state / 0xffffffff;
  };
}

function buildRingLayout(count: number, width: number, height: number): Node[] {
  const cx = width / 2;
  const cy = height / 2;
  const radius = Math.max(90, Math.min(width, height) * 0.32);
  const nodes: Node[] = [];
  for (let i = 0; i < count; i++) {
    const angle = (Math.PI * 2 * i) / Math.max(1, count);
    nodes.push({ id: i + 1, x: cx + radius * Math.cos(angle), y: cy + radius * Math.sin(angle), state: "neutral", pulse: 0 });
  }
  return nodes;
}

function buildEdges(random: () => number, numNodes: number, target: number): Edge[] {
  const edges: Edge[] = [];
  const seen = new Set<string>();

  const maxEdges = (numNodes * (numNodes - 1)) / 2;
  const count = Math.min(target, maxEdges);

  for (let i = 1; i <= numNodes; i++) {
    const from = i;
    const to = i === numNodes ? 1 : i + 1;
    const key = from < to ? `${from}-${to}` : `${to}-${from}`;
    if (!seen.has(key)) {
      seen.add(key);
      edges.push({ from, to, state: "active" });
    }
  }

  while (edges.length < count) {
    const from = 1 + Math.floor(random() * numNodes);
    const to = 1 + Math.floor(random() * numNodes);
    if (from === to) continue;
    const key = from < to ? `${from}-${to}` : `${to}-${from}`;
    if (seen.has(key)) continue;
    seen.add(key);
    edges.push({ from, to, state: "active" });
  }

  return edges;
}

function drawNetwork(canvas: HTMLCanvasElement, nodes: Node[], edges: Edge[]) {
  const ctx = canvas.getContext("2d");
  if (!ctx) return;
  const w = canvas.width;
  const h = canvas.height;

  ctx.clearRect(0, 0, w, h);

  ctx.save();
  ctx.globalAlpha = 0.1;
  ctx.strokeStyle = "rgba(140,170,220,0.40)";
  ctx.lineWidth = 1;
  for (let x = 0; x <= w; x += 28) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }
  for (let y = 0; y <= h; y += 28) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }
  ctx.restore();

  ctx.save();
  ctx.lineWidth = 1.7;
  ctx.globalAlpha = 0.8;
  edges.forEach((e) => {
    const a = nodes[e.from - 1];
    const b = nodes[e.to - 1];
    if (!a || !b) return;
    const disrupted = e.state === "disrupted";
    ctx.strokeStyle = disrupted ? "rgba(255,93,108,0.52)" : "rgba(100,181,255,0.33)";
    ctx.setLineDash(disrupted ? [6, 6] : []);
    ctx.beginPath();
    ctx.moveTo(a.x, a.y);
    ctx.lineTo(b.x, b.y);
    ctx.stroke();
  });
  ctx.restore();

  nodes.forEach((n) => {
    const color =
      n.state === "attacked" ? "#ff5d6c" : n.state === "recovering" ? "#f6c445" : n.state === "stable" ? "#3ddc97" : "#3b82f6";

    ctx.save();
    ctx.fillStyle = color;
    ctx.strokeStyle = "rgba(234,242,255,0.12)";
    ctx.lineWidth = 2.2;
    ctx.shadowColor = color;
    ctx.shadowBlur = n.state === "attacked" ? 20 : n.state === "recovering" ? 15 : 12;

    if (n.pulse > 0.01) {
      ctx.globalAlpha = 0.16;
      ctx.beginPath();
      ctx.arc(n.x, n.y, 16 + n.pulse * 14, 0, Math.PI * 2);
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.globalAlpha = 1;
    }

    ctx.beginPath();
    ctx.arc(n.x, n.y, 11.5, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = "rgba(234,242,255,0.92)";
    ctx.globalAlpha = 0.92;
    ctx.font = "850 10px Segoe UI, Tahoma, sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "top";
    ctx.fillText(String(n.id), n.x, n.y + 14);
    ctx.restore();
  });
}

async function runPhases(canvas: HTMLCanvasElement, cfg: RunConfig) {
  const random = seededRandom(cfg.seed);
  const numNodes = Math.max(2, Math.min(60, cfg.numNodes));
  const numEdges = Math.max(0, Math.min(400, cfg.numEdges));
  const nodes = buildRingLayout(numNodes, canvas.width, canvas.height);
  const edges = buildEdges(random, numNodes, numEdges);

  const afterAttack = typeof cfg.afterAttackConnectivity === "number" ? cfg.afterAttackConnectivity : 0.75;
  const afterRecovery = typeof cfg.afterRecoveryConnectivity === "number" ? cfg.afterRecoveryConnectivity : 0.6;
  const efficiency = typeof cfg.attackEfficiency === "number" ? cfg.attackEfficiency : 0.65;
  const compromiseTarget = clamp01(1 - afterAttack);

  cfg.onPhase("attack");
  cfg.onFeed("Attack detected. Telemetry online.", "attack");

  const attackedCount = Math.max(1, Math.round(numNodes * compromiseTarget));
  const attackedIds = pickUnique(random, numNodes, attackedCount);
  const disruptedEdges = pickUnique(random, edges.length, Math.max(1, Math.round(edges.length * compromiseTarget * 0.35))).map((i) => i - 1);

  for (let step = 0; step <= 20; step++) {
    const t = step / 20;
    const currentCompromise = lerp(0.02, compromiseTarget, t);
    cfg.onKpis(currentCompromise, lerp(1, afterAttack, t), efficiency);

    attackedIds.slice(0, Math.max(1, Math.round(attackedCount * t))).forEach((id) => {
      nodes[id - 1].state = "attacked";
      nodes[id - 1].pulse = Math.max(nodes[id - 1].pulse, 1 - t);
    });
    disruptedEdges.slice(0, Math.max(1, Math.round(disruptedEdges.length * t))).forEach((idx) => {
      if (edges[idx]) edges[idx].state = "disrupted";
    });
    if (step === 6) cfg.onFeed("Selective disruption escalating across high-connectivity links.", "attack");
    drawNetwork(canvas, nodes, edges);
    nodes.forEach((n) => (n.pulse = Math.max(0, n.pulse * 0.84)));
    await sleep(55);
  }

  cfg.onPhase("defense");
  cfg.onFeed("Defense response engaged. Prioritizing critical nodes.", "defense");
  for (let step = 0; step <= 16; step++) {
    const t = step / 16;
    const containCount = Math.round(attackedCount * lerp(0.15, 0.35, t));
    attackedIds.slice(0, containCount).forEach((id) => {
      nodes[id - 1].state = "stable";
      nodes[id - 1].pulse = Math.max(nodes[id - 1].pulse, 0.65);
    });
    disruptedEdges.slice(0, Math.max(1, Math.round(disruptedEdges.length * lerp(0.1, 0.5, t)))).forEach((idx) => {
      if (edges[idx]) edges[idx].state = "active";
    });
    drawNetwork(canvas, nodes, edges);
    nodes.forEach((n) => (n.pulse = Math.max(0, n.pulse * 0.86)));
    await sleep(60);
  }

  cfg.onPhase("recovery");
  cfg.onFeed("System recovering. Restoring connectivity after disruption.", "recovery");
  const compromiseAfterRecovery = clamp01(1 - afterRecovery);
  for (let step = 0; step <= 22; step++) {
    const t = step / 22;
    const recoveredCompromise = lerp(compromiseTarget, compromiseAfterRecovery, t);
    cfg.onKpis(recoveredCompromise, lerp(afterAttack, afterRecovery, t), efficiency);

    attackedIds.forEach((id, idx) => {
      const node = nodes[id - 1];
      const pivot = (idx / Math.max(1, attackedIds.length)) * 0.65;
      if (t < pivot) node.state = "attacked";
      else if (t < pivot + 0.25) {
        node.state = "recovering";
        node.pulse = Math.max(node.pulse, 0.55);
      } else node.state = "stable";
    });
    drawNetwork(canvas, nodes, edges);
    nodes.forEach((n) => (n.pulse = Math.max(0, n.pulse * 0.88)));
    await sleep(50);
  }
}

function pickUnique(random: () => number, maxId: number, count: number) {
  const picked = new Set<number>();
  const target = Math.min(count, maxId);
  while (picked.size < target) {
    const id = 1 + Math.floor(random() * maxId);
    picked.add(id);
  }
  return Array.from(picked);
}

function sleep(ms: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, ms));
}

