import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../app/AuthContext";
import type { PublicUser } from "../types/api";

export const investor: PublicUser = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Ana Investidora",
  email: "ana@example.com",
  role: "INVESTOR",
};
export const admin: PublicUser = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Admin",
  email: "admin@example.com",
  role: "ADMIN",
};
export function setSession(user: PublicUser) {
  sessionStorage.setItem(
    "bom-investidor.session",
    JSON.stringify({
      token: "valid-token",
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      user,
    }),
  );
}
export function renderApp(children: React.ReactNode, route = "/") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  window.history.pushState({}, "", route);
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>{children}</AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
