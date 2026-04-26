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

