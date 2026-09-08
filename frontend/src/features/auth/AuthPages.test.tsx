import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { Route, Routes } from "react-router-dom";
import { LoginPage, RegisterPage } from "./AuthPages";
import { investor, renderApp } from "../../test/render";
import { server } from "../../test/server";

test("autentica e direciona o investidor", async () => {
  server.use(
    http.post("*/api/auth/login", () =>
      HttpResponse.json({
        token: "token",
        tokenType: "Bearer",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: investor,
      }),
    ),
  );
  renderApp(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/app/carteiras" element={<h1>Minhas carteiras</h1>} />
    </Routes>,
    "/login",
  );
  await userEvent.type(screen.getByLabelText("E-mail"), "ana@example.com");
  await userEvent.type(screen.getByLabelText("Senha"), "password123");
  await userEvent.click(screen.getByRole("button", { name: /entrar/i }));
  expect(
    await screen.findByRole("heading", { name: "Minhas carteiras" }),
  ).toBeInTheDocument();
});

test("apresenta erros de campos no cadastro", async () => {
  renderApp(<RegisterPage />, "/cadastro");
  await userEvent.click(screen.getByRole("button", { name: /criar conta/i }));
  expect(await screen.findByText("Informe seu nome.")).toBeInTheDocument();
  expect(screen.getByText("Informe um e-mail válido.")).toBeInTheDocument();
});
