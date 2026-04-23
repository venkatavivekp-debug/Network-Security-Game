import { useEffect, useMemo, useState } from "react";
import type { AlgorithmType, MessageSummaryResponse } from "../api/types";
import { ApiError } from "../api/client";
import { messageApi } from "../api/message";
import { useAuth } from "../state/auth/AuthProvider";

export function MessagingPage() {
  const { user } = useAuth();
  const [mode, setMode] = useState<"send" | "inbox">("send");
  const [notice, setNotice] = useState<string | null>(null);

  const canSend = user?.role === "SENDER";
  const canReceive = user?.role === "RECEIVER";

  useEffect(() => {
    if (canReceive) setMode("inbox");
    else setMode("send");
  }, [canReceive]);

  return (
    <div className="cc-page">
      <section className="cc-surface" style={{ padding: 0 }}>
        <div className="cc-panel-header">
          <div className="cc-panel-title">Secure Messaging</div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button className="cc-btn" type="button" onClick={() => setMode("send")} disabled={!canSend}>
              Send
            </button>
            <button className="cc-btn" type="button" onClick={() => setMode("inbox")} disabled={!canReceive}>
              Receive
            </button>
          </div>
        </div>
        <div className="cc-panel-body">
          {notice ? <div className="cc-notice">{notice}</div> : null}
          {mode === "send" ? (
            <SendPanel enabled={canSend} onNotice={setNotice} />
          ) : (
            <InboxPanel enabled={canReceive} onNotice={setNotice} />
          )}
        </div>
      </section>
    </div>
  );
}

function SendPanel({ enabled, onNotice }: { enabled: boolean; onNotice: (s: string | null) => void }) {
  const [receiverUsername, setReceiverUsername] = useState("");
  const [content, setContent] = useState("");
  const [algorithmType, setAlgorithmType] = useState<AlgorithmType>("SHCS");
  const [busy, setBusy] = useState(false);

  async function onSend() {
    onNotice(null);
    setBusy(true);
    try {
      await messageApi.send({ receiverUsername, content, algorithmType });
      onNotice("Message sent successfully.");
      setContent("");
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Send failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 12 }}>
      {!enabled ? (
        <div className="cc-notice">This account is not allowed to send messages. Login as `SENDER`.</div>
      ) : null}

      <label className="cc-label">
        Receiver username
        <input className="cc-input" value={receiverUsername} onChange={(e) => setReceiverUsername(e.target.value)} disabled={!enabled || busy} />
      </label>

      <label className="cc-label">
        Algorithm
        <select className="cc-select" value={algorithmType} onChange={(e) => setAlgorithmType(e.target.value as AlgorithmType)} disabled={!enabled || busy}>
          <option value="NORMAL">NORMAL</option>
          <option value="SHCS">SHCS</option>
          <option value="CPHS">CPHS</option>
        </select>
      </label>

      <label className="cc-label" style={{ textTransform: "none", letterSpacing: "0.02em" }}>
        Message content
        <textarea
          className="cc-input"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={5}
          disabled={!enabled || busy}
          style={{ resize: "vertical" }}
        />
      </label>

      <button className="cc-btn" type="button" onClick={() => void onSend()} disabled={!enabled || busy || !receiverUsername || !content}>
        {busy ? "Sending…" : "Send message"}
      </button>
    </div>
  );
}

function InboxPanel({ enabled, onNotice }: { enabled: boolean; onNotice: (s: string | null) => void }) {
  const [busy, setBusy] = useState(false);
  const [items, setItems] = useState<MessageSummaryResponse[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [decrypted, setDecrypted] = useState<string | null>(null);

  const selected = useMemo(() => items.find((m) => m.id === selectedId) ?? null, [items, selectedId]);

  async function load() {
    onNotice(null);
    setBusy(true);
    try {
      const data = await messageApi.received();
      setItems(data);
      if (data.length && selectedId == null) setSelectedId(data[0].id);
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Failed to load inbox.");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    if (enabled) void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]);

  async function decrypt() {
    if (!selectedId) return;
    onNotice(null);
    setBusy(true);
    try {
      const res = await messageApi.decrypt(selectedId);
      setDecrypted(res.decryptedContent);
      onNotice("Message decrypted.");
    } catch (err) {
      if (err instanceof ApiError) onNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      else onNotice("Decrypt failed.");
    } finally {
      setBusy(false);
    }
  }

  if (!enabled) {
    return <div className="cc-notice">This account is not allowed to receive messages. Login as `RECEIVER`.</div>;
  }

  return (
    <div style={{ display: "grid", gridTemplateColumns: "380px minmax(0,1fr)", gap: 12 }}>
      <div className="cc-surface" style={{ padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
          <div style={{ fontWeight: 900, letterSpacing: "-0.01em" }}>Inbox</div>
          <button className="cc-btn cc-btn--ghost" type="button" onClick={() => void load()} disabled={busy}>
            Refresh
          </button>
        </div>
        <div style={{ marginTop: 10, display: "grid", gap: 8, maxHeight: 420, overflow: "auto" }}>
          {items.length ? (
            items.map((m) => (
              <button
                key={m.id}
                type="button"
                className="cc-btn cc-btn--ghost"
                onClick={() => {
                  setSelectedId(m.id);
                  setDecrypted(null);
                }}
                style={{
                  textAlign: "left",
                  borderColor: selectedId === m.id ? "rgba(100, 181, 255, 0.28)" : "rgba(123, 145, 189, 0.18)",
                }}
              >
                <div style={{ fontWeight: 900 }}>{m.senderUsername}</div>
                <div style={{ color: "var(--cc-muted)", fontSize: 12, fontWeight: 650 }}>Algo: {m.algorithmType}</div>
              </button>
            ))
          ) : (
            <div className="cc-empty" style={{ padding: 10 }}>
              No messages received.
            </div>
          )}
        </div>
      </div>

      <div className="cc-surface" style={{ padding: 12 }}>
        {selected ? (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
              <div style={{ fontWeight: 950, letterSpacing: "-0.02em" }}>Message #{selected.id}</div>
              <button className="cc-btn" type="button" onClick={() => void decrypt()} disabled={busy}>
                {busy ? "Working…" : "Decrypt"}
              </button>
            </div>
            <div style={{ marginTop: 10, display: "grid", gap: 8 }}>
              <div className="cc-result-row">
                <span>From</span>
                <span>{selected.senderUsername}</span>
              </div>
              <div className="cc-result-row">
                <span>Algorithm</span>
                <span>{selected.algorithmType}</span>
              </div>
              <div style={{ marginTop: 8, color: "var(--cc-muted)", fontWeight: 650, fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase" }}>
                Encrypted payload
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
                  maxHeight: 180,
                }}
              >
                {selected.encryptedContent}
              </pre>

              {decrypted ? (
                <>
                  <div style={{ marginTop: 8, color: "rgba(103, 232, 179, 0.90)", fontWeight: 850, fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase" }}>
                    Decrypted content
                  </div>
                  <pre
                    style={{
                      margin: 0,
                      padding: 12,
                      borderRadius: 14,
                      border: "1px solid rgba(103, 232, 179, 0.22)",
                      background: "rgba(7, 11, 18, 0.50)",
                      color: "rgba(234, 242, 255, 0.92)",
                      overflow: "auto",
                      maxHeight: 220,
                    }}
                  >
                    {decrypted}
                  </pre>
                </>
              ) : null}
            </div>
          </>
        ) : (
          <div className="cc-empty">Select a message to inspect.</div>
        )}
      </div>
    </div>
  );
}

