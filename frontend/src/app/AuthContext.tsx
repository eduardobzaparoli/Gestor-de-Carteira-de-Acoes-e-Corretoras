import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, setAccessToken } from "../lib/http";
import type {
  AuthenticationResponse,
  ProfileUpdateRequest,
  PublicUser,
} from "../types/api";

const STORAGE_KEY = "bom-investidor.session";
type StoredSession = { token: string; expiresAt: string; user: PublicUser };
type AuthContextValue = {
  user: PublicUser | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<PublicUser>;
  logout: (reason?: string) => void;
  register: (name: string, email: string, password: string) => Promise<void>;
  updateProfile: (request: ProfileUpdateRequest) => Promise<PublicUser>;
  reason?: string;
};
const AuthContext = createContext<AuthContextValue | null>(null);

function readSession(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const value = JSON.parse(raw) as StoredSession;
    return new Date(value.expiresAt).getTime() > Date.now() ? value : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const initial = useMemo(readSession, []);
  const [token, setToken] = useState<string | null>(initial?.token ?? null);
  const [user, setUser] = useState<PublicUser | null>(initial?.user ?? null);
  const [loading, setLoading] = useState(Boolean(initial));
  const [reason, setReason] = useState<string>();

  const logout = useCallback(
    (nextReason?: string) => {
      sessionStorage.removeItem(STORAGE_KEY);
      setAccessToken(null);
      setToken(null);
      setUser(null);
      setReason(nextReason);
      queryClient.clear();
    },
    [queryClient],
  );

  useEffect(() => {
    const unauthorized = (event: Event) =>
      logout(
        (event as CustomEvent<string>).detail === "ACCOUNT_INACTIVE"
          ? "Sua conta está inativa."
          : "Sua sessão expirou. Entre novamente.",
      );
    window.addEventListener("auth:unauthorized", unauthorized);
    return () => window.removeEventListener("auth:unauthorized", unauthorized);
  }, [logout]);

  useEffect(() => {
    if (!initial) {
      setLoading(false);
      return;
    }
    setAccessToken(initial.token);
    api<PublicUser>("/api/auth/me")
      .then((current) => {
        const session = { ...initial, user: current };
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        setUser(current);
      })
      .catch(() => logout("Sua sessão expirou. Entre novamente."))
      .finally(() => setLoading(false));
  }, [initial, logout]);

  const login = async (email: string, password: string) => {
    const response = await api<AuthenticationResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    const session: StoredSession = {
      token: response.token,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    setAccessToken(response.token);
    setToken(response.token);
    setUser(response.user);
    setReason(undefined);
    return response.user;
  };
  const register = async (name: string, email: string, password: string) => {
    await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });
  };
  const updateProfile = async (request: ProfileUpdateRequest) => {
    const updated = await api<PublicUser>("/api/auth/me", {
      method: "PUT",
      body: JSON.stringify(request),
    });
    const stored = readSession();
    if (stored)
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ ...stored, user: updated }),
      );
    setUser(updated);
    queryClient.setQueryData(["current-user"], updated);
    return updated;
  };
  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        logout,
        register,
        updateProfile,
        reason,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used within AuthProvider");
  return value;
}
