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

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
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

  const res = await fetch(path, {
    ...init,
    headers,
    credentials: "include",
  });

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

