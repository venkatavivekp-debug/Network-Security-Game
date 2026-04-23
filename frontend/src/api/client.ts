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

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
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

