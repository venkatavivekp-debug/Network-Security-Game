import { useEffect, useState } from "react";
import type { EvaluationRunResponse } from "../api/types";
import { ApiError } from "../api/client";
import { simulationApi } from "../api/simulation";

export function EvaluationPage() {
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [items, setItems] = useState<EvaluationRunResponse[]>([]);

  async function load() {
    setNotice(null);
    setBusy(true);
    try {
      const data = await simulationApi.evaluations();
      setItems(data);
    } catch (err) {
      if (err instanceof ApiError) setNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else setNotice("Failed to load evaluations.");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <div className="cc-page">
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Evaluation Dashboard</div>
          <button className="cc-btn cc-btn--ghost" type="button" onClick={() => void load()} disabled={busy}>
            Refresh
          </button>
        </div>
        <div className="cc-panel-body">
          {notice ? <div className="cc-notice">{notice}</div> : null}
          {!items.length ? (
            <div className="cc-empty">No evaluation runs found yet.</div>
          ) : (
            <div className="cc-eval-grid">
              {items.map((e) => (
                <div key={e.evaluationRunId} className="cc-surface" style={{ padding: 12 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "baseline" }}>
                    <div style={{ fontWeight: 950, letterSpacing: "-0.02em" }}>{e.scenarioName}</div>
                    <div className="cc-userchip">#{e.evaluationRunId}</div>
                  </div>
                  <div style={{ marginTop: 10, display: "grid", gap: 8 }}>
                    <div className="cc-result-row">
                      <span>Algorithm</span>
                      <span>{e.algorithmType}</span>
                    </div>
                    <div className="cc-result-row">
                      <span>Rounds</span>
                      <span>{e.rounds}</span>
                    </div>
                    <div className="cc-result-row">
                      <span>Repetitions</span>
                      <span>{e.repetitions}</span>
                    </div>
                    <div className="cc-result-row">
                      <span>MTD / Deception</span>
                      <span>
                        {e.enableMTD ? "on" : "off"} / {e.enableDeception ? "on" : "off"}
                      </span>
                    </div>
                    <div style={{ color: "var(--cc-muted)", fontWeight: 900, fontSize: 12, letterSpacing: "0.12em", textTransform: "uppercase", marginTop: 6 }}>
                      Aggregate metrics (raw)
                    </div>
                    <pre
                      style={{
                        margin: 0,
                        padding: 12,
                        borderRadius: 14,
                        border: "1px solid rgba(123, 145, 189, 0.18)",
                        background: "rgba(7, 11, 18, 0.50)",
                        color: "rgba(234, 242, 255, 0.90)",
                        overflow: "auto",
                        maxHeight: 220,
                      }}
                    >
                      {JSON.stringify(e.aggregateMetrics ?? {}, null, 2)}
                    </pre>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

