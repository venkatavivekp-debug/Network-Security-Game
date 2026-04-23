import { apiRequest } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "./types";

export const authApi = {
  login: (body: LoginRequest) => apiRequest<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  register: (body: RegisterRequest) =>
    apiRequest<AuthResponse>("/auth/register", { method: "POST", body: JSON.stringify(body) }),
  logout: async () => {
    // Spring Security default logout endpoint uses POST /logout.
    await fetch("/logout", { method: "POST", credentials: "include" });
  },
};

