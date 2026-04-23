import React, { createContext, useCallback, useContext, useMemo, useState } from "react";
import type { AuthResponse, Role } from "../../api/types";
import { authApi } from "../../api/auth";

type AuthState = {
  user: { username: string; role: Role } | null;
  login: (username: string, password: string) => Promise<AuthResponse>;
  register: (username: string, password: string, role: Role) => Promise<AuthResponse>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

const STORAGE_KEY = "ns_game_auth_v1";

function readStoredUser(): { username: string; role: Role } | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as { username: string; role: Role };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<{ username: string; role: Role } | null>(() => readStoredUser());

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login({ username, password });
    const next = { username: res.username, role: res.role };
    setUser(next);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    return res;
  }, []);

  const register = useCallback(async (username: string, password: string, role: Role) => {
    const res = await authApi.register({ username, password, role });
    return res;
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setUser(null);
    window.localStorage.removeItem(STORAGE_KEY);
  }, []);

  const value = useMemo<AuthState>(() => ({ user, login, register, logout }), [user, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

