import { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import type { Role } from "../api/types";
import { useAuth } from "../state/auth/AuthProvider";

export function LoginPage() {
  const { login, register } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();

  const nextPath = useMemo(() => {
    const from = (loc.state as { from?: string } | null)?.from;
    return typeof from === "string" ? from : "/simulation";
  }, [loc.state]);

  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("SENDER");
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setNotice(null);
    setBusy(true);
    try {
      if (mode === "register") {
        const res = await register(username, password, role);
        setNotice(res.message || "Registered. You can now log in.");
        setMode("login");
      } else {
        await login(username, password);
        nav(nextPath, { replace: true });
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setNotice(err.details?.length ? `${err.message}: ${err.details.join("; ")}` : err.message);
      } else {
        setNotice("Login failed. Please try again.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="cc-surface" style={{ padding: 18, maxWidth: 520, margin: "18px auto" }}>
      <div style={{ padding: 14, borderBottom: "1px solid var(--cc-line)", position: "relative" }}>
        <div style={{ fontWeight: 950, letterSpacing: "-0.02em", fontSize: 18 }}>Access Control</div>
        <div style={{ color: "var(--cc-muted)", fontWeight: 650, marginTop: 6, fontSize: 13 }}>
          Sign in using the backend session-based auth (`/auth/login`).
        </div>
      </div>

      <form onSubmit={onSubmit} style={{ padding: 14, display: "grid", gap: 12, position: "relative" }}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button
            type="button"
            className="cc-btn"
            onClick={() => setMode("login")}
            disabled={busy}
            style={{ background: mode === "login" ? "rgba(100, 181, 255, 0.14)" : undefined }}
          >
            Login
          </button>
          <button
            type="button"
            className="cc-btn"
            onClick={() => setMode("register")}
            disabled={busy}
            style={{ background: mode === "register" ? "rgba(100, 181, 255, 0.14)" : undefined }}
          >
            Register
          </button>
        </div>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--cc-muted)", fontWeight: 850 }}>
            Username
          </span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoComplete="username"
            className="cc-input"
            style={inputStyle}
          />
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={{ fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--cc-muted)", fontWeight: 850 }}>
            Password
          </span>
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            type="password"
            autoComplete={mode === "register" ? "new-password" : "current-password"}
            className="cc-input"
            style={inputStyle}
          />
        </label>

        {mode === "register" ? (
          <label style={{ display: "grid", gap: 6 }}>
            <span style={{ fontSize: 12, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--cc-muted)", fontWeight: 850 }}>
              Role
            </span>
            <select value={role} onChange={(e) => setRole(e.target.value as Role)} className="cc-input" style={inputStyle}>
              <option value="SENDER">SENDER</option>
              <option value="RECEIVER">RECEIVER</option>
            </select>
          </label>
        ) : null}

        {notice ? (
          <div
            style={{
              border: "1px solid rgba(100, 181, 255, 0.22)",
              background: "rgba(100, 181, 255, 0.08)",
              borderRadius: 14,
              padding: "10px 12px",
              color: "rgba(230, 243, 255, 0.92)",
              fontWeight: 650,
              fontSize: 13,
              lineHeight: 1.35,
            }}
          >
            {notice}
          </div>
        ) : null}

        <button className="cc-btn" type="submit" disabled={busy}>
          {busy ? "Working…" : mode === "register" ? "Register" : "Login"}
        </button>
      </form>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  background: "rgba(7, 11, 18, 0.55)",
  border: "1px solid rgba(123, 145, 189, 0.26)",
  color: "rgba(234, 242, 255, 0.92)",
  borderRadius: 14,
  padding: "10px 12px",
  outline: "none",
};

