import { apiRequest } from "./client";
import type {
  AuditEventView,
  HeldMessageView,
  RecoveryPolicyEntry,
  SystemPressureResponse,
  UserAtRiskView,
} from "./types";

export const adminApi = {
  heldMessages: () => apiRequest<HeldMessageView[]>("/admin/held-messages", { method: "GET" }),
  usersAtRisk: () => apiRequest<UserAtRiskView[]>("/admin/users-at-risk", { method: "GET" }),
  recentAudit: () => apiRequest<AuditEventView[]>("/admin/audit/recent", { method: "GET" }),
  suspiciousSessions: () => apiRequest<AuditEventView[]>("/admin/audit/suspicious-sessions", { method: "GET" }),
  recoveryPolicy: () => apiRequest<RecoveryPolicyEntry[]>("/admin/recovery-policy", { method: "GET" }),
  threatLevel: () => apiRequest<{ attackIntensity01: number }>("/admin/threat-level", { method: "GET" }),
  systemPressure: () => apiRequest<SystemPressureResponse>("/admin/system-pressure", { method: "GET" }),
  setThreatLevel: (intensity: number) => {
    const params = new URLSearchParams({ attackIntensity01: String(intensity) });
    return apiRequest<{ attackIntensity01: number }>(`/admin/threat-level?${params.toString()}`, { method: "POST" });
  },
  hold: (messageId: number, reason: string) => {
    const params = new URLSearchParams({ messageId: String(messageId), reason });
    return apiRequest<{ messageId: number; status: string }>(`/admin/hold-message?${params.toString()}`, { method: "POST" });
  },
  release: (messageId: number) => {
    const params = new URLSearchParams({ messageId: String(messageId) });
    return apiRequest<{ messageId: number; status: string }>(`/admin/release-message?${params.toString()}`, { method: "POST" });
  },
  resetFailures: (username: string) => {
    const params = new URLSearchParams({ username });
    return apiRequest<{ username: string; consecutiveFailures: number }>(`/admin/reset-failures?${params.toString()}`, { method: "POST" });
  },
};
