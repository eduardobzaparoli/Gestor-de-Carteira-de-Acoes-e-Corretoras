import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { App } from "./App";
import { admin, investor, renderApp, setSession } from "../test/render";
import { server } from "../test/server";

test("restaura a sessão validada do investidor", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () => HttpResponse.json([])),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
  );
  renderApp(<App />, "/app/carteiras");
  expect(
    await screen.findByRole("heading", { name: "Olá, Ana" }),
  ).toBeInTheDocument();
});

test("abre o catálogo de ativos pela rota protegida do investidor", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/assets", () => HttpResponse.json([])),
  );
  renderApp(<App />, "/app/ativos");
  expect(
    await screen.findByRole("heading", { name: "Seus ativos" }),
  ).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Ativos" })).toBeInTheDocument();
});

test("a marca leva o investidor de volta à visão geral", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/assets", () => HttpResponse.json([])),
    http.get("*/api/portfolios", () => HttpResponse.json([])),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
  );
  renderApp(<App />, "/app/ativos");
  await screen.findByRole("heading", { name: "Seus ativos" });
  await userEvent.click(
    screen.getAllByRole("link", { name: "Voltar à visão geral" })[0],
  );
  expect(
    await screen.findByRole("heading", { name: /Olá,/i }),
  ).toBeInTheDocument();
});

test("descarta uma sessão expirada antes de acessar uma rota privada", async () => {
  sessionStorage.setItem(
    "bom-investidor.session",
    JSON.stringify({
      token: "expired",
      expiresAt: new Date(Date.now() - 1_000).toISOString(),
      user: investor,
    }),
  );
  renderApp(<App />, "/app/carteiras");
  expect(
    await screen.findByRole("heading", { name: "Acesse sua conta" }),
  ).toBeInTheDocument();
});

test("redireciona administrador para sua área sem expor finanças", async () => {
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () => HttpResponse.json([])),
  );
  renderApp(<App />, "/app/carteiras");
  expect(
    await screen.findByRole("heading", { name: "Gestão de usuários" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Carteiras")).not.toBeInTheDocument();
});
