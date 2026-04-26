import { useEffect, useState } from "react";
import { adminApi } from "../../api/admin";
import { ApiError, adminStepUp } from "../../api/client";

interface Props {
  open: boolean;
  reason?: string;
  onClose: () => void;
  onConfirmed: () => void;
}

/**
 * Lightweight admin step-up dialog. The admin re-enters their password; the
 * backend verifies it and mints a short-lived confirmation token. After that
 * the originating sensitive action can be retried.
 */
export function AdminStepUpModal({ open, reason, onClose, onConfirmed }: Props) {
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setPassword("");
      setError(null);
      setBusy(false);
    }
  }, [open]);

  if (!open) return null;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const res = await adminApi.confirmAction(password);
      adminStepUp.set(res.token);
      onConfirmed();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Confirmation failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="cc-modal__backdrop" role="dialog" aria-modal="true" aria-labelledby="step-up-title">
      <div className="cc-modal">
        <div className="cc-modal__head">
          <span id="step-up-title" className="cc-modal__title">
            Admin step-up required
          </span>
          <button type="button" className="cc-btn cc-btn--ghost" onClick={onClose} disabled={busy}>
            Cancel
          </button>
        </div>
        <div className="cc-modal__body">
          <p className="cc-modal__hint">
            {reason ?? "Re-confirm your admin password to authorise this sensitive action."}
            {" "}This token stays valid for 5 minutes and is never echoed back in plaintext.
          </p>
          <form onSubmit={submit} className="cc-modal__form">
            <label className="cc-field">
              <span className="cc-field__label">Admin password</span>
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={busy}
                required
                autoFocus
              />
            </label>
            {error ? <div className="cc-notice cc-notice--error">{error}</div> : null}
            <div className="cc-modal__actions">
              <button type="submit" className="cc-btn cc-btn--primary" disabled={busy || password.length === 0}>
                {busy ? "Confirming…" : "Confirm and retry"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
