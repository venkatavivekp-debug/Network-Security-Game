import type { ApiErrorResponse, ApiSuccessResponse } from "./types";

export class ApiError extends Error {
  status: number;
  details?: string[];
  path?: string;

  constructor(message: string, status: number, details?: string[], path?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
    this.path = path;
  }

  /**
   * Heuristic: a 403 with the step-up message (or path under /admin/) means
   * the admin needs to confirm their password before the action will succeed.
   * The frontend uses this to show the step-up modal and retry.
   */
  isAdminStepUpRequired(): boolean {
    if (this.status !== 403) return false;
    const msg = (this.message || "").toLowerCase();
    return msg.includes("step-up") || msg.includes("admin step");
  }

  /**
   * User-facing safe message: turns server status codes into short,
   * non-technical text that we can show in a toast/banner without
   * leaking server internals.
   */
  userFacingMessage(): string {
    if (this.status === 401) return "Session ended. Sign in again to continue.";
    if (this.status === 403) {
      if (this.isAdminStepUpRequired()) return "Admin verification required.";
      const msg = (this.message || "").toLowerCase();
      if (msg.includes("replay_blocked")) {
        return "Replay blocked. Refresh and try again.";
      }
      if (msg.includes("request_integrity_failed")) {
        return "Request integrity check failed. Refresh and retry.";
      }
      if (msg.includes("security header")) {
        return "Request blocked for safety. Reload the page and try again.";
      }
      return "Action not allowed for this account.";
    }
    if (this.status === 404) return "Item not found or no longer available.";
    if (this.status === 408 || this.status === 410) return "Challenge expired. Reload to start over.";
    if (this.status === 413) return "Request too large. Shorten it and try again.";
    if (this.status === 429) return "Too many requests. Slow down and retry shortly.";
    if (this.status >= 500) return "Server is having trouble. Please try again.";
    return this.message || "Request could not be completed.";
  }
}

async function parseJsonSafe(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return { raw: text };
  }
}

let stepUpToken: string | null = null;
let integritySecretB64: string | null = null;
let lastThrottleMs: number = 0;

export const adminStepUp = {
  set(token: string | null) {
    stepUpToken = token && token.length > 0 ? token : null;
  },
  get(): string | null {
    return stepUpToken;
  },
  clear() {
    stepUpToken = null;
  },
};

async function ensureIntegritySecret(): Promise<string | null> {
  if (integritySecretB64) return integritySecretB64;
  try {
    const res = await fetch("/security/integrity-key", {
      method: "GET",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
    });
    if (!res.ok) return null;
    const payload = (await parseJsonSafe(res)) as unknown as ApiSuccessResponse<{ secretB64: string }>;
    const secret = payload?.data?.secretB64;
    integritySecretB64 = secret && secret.length > 0 ? secret : null;
    return integritySecretB64;
  } catch {
    return null;
  }
}

export function consumeThrottleNotice(): string | null {
  if (!lastThrottleMs || lastThrottleMs <= 0) return null;
  const ms = lastThrottleMs;
  lastThrottleMs = 0;
  return `Temporary throttle applied (${ms}ms).`;
}

function isSensitiveMutating(path: string, method: string): boolean {
  const m = method.toUpperCase();
  if (m === "GET" || m === "HEAD" || m === "OPTIONS") return false;
  if (path.startsWith("/admin/")) return true;
  if (path.startsWith("/message/decrypt/")) return true;
  if (path.startsWith("/puzzle/solve/")) return true;
  // backward-compatible path shape (some clients use /puzzle/{id}/solve)
  if (path.startsWith("/puzzle/") && path.includes("/solve")) return true;
  return false;
}

function base64ToArrayBuffer(b64: string): ArrayBuffer {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

function bytesToBase64(bytes: ArrayBuffer): string {
  const u8 = new Uint8Array(bytes);
  let bin = "";
  for (let i = 0; i < u8.length; i++) bin += String.fromCharCode(u8[i]);
  return btoa(bin);
}

async function signCanonical(secretB64: string, canonical: string): Promise<string> {
  const keyBytes = base64ToArrayBuffer(secretB64);
  const key = await crypto.subtle.importKey(
    "raw",
    keyBytes,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(canonical));
  return bytesToBase64(sig);
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method || "GET").toUpperCase();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    // Compensating CSRF control: the backend requires this header on every
    // mutating request. Browsers refuse to set it for cross-origin HTML forms
    // without a preflight, so this gates simple cross-site POSTs.
    "X-Requested-With": "XMLHttpRequest",
    ...((init?.headers as Record<string, string>) ?? {}),
  };
  if (stepUpToken && path.startsWith("/admin/")) {
    headers["X-Admin-Confirm"] = stepUpToken;
  }

  if (isSensitiveMutating(path, method)) {
    const secret = await ensureIntegritySecret();
    if (secret) {
      const nonce = crypto.randomUUID();
      const ts = String(Date.now());
      const body = typeof init?.body === "string" ? init?.body : "";
      const canonical = `${method}\n${path}\n${body}\n${ts}\n${nonce}`;
      const sig = await signCanonical(secret, canonical);
      headers["X-Req-Nonce"] = nonce;
      headers["X-Req-Ts"] = ts;
      headers["X-Req-Sig"] = sig;
    }
  }

  const res = await fetch(path, {
    ...init,
    headers,
    credentials: "include",
  });

  const throttle = res.headers.get("X-NSG-Throttle-Ms");
  if (throttle) {
    const v = Number(throttle);
    if (Number.isFinite(v) && v > 0) lastThrottleMs = Math.floor(v);
  }

  const payload = (await parseJsonSafe(res)) as unknown;

  if (!res.ok) {
    const err = payload as Partial<ApiErrorResponse>;
    throw new ApiError(
      err?.message || `Request failed (${res.status})`,
      res.status,
      err?.details,
      err?.path
    );
  }

  const ok = payload as ApiSuccessResponse<T>;
  return ok.data;
}

