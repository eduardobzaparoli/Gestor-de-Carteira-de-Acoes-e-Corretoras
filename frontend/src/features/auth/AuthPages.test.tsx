import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { Route, Routes } from "react-router-dom";
import { LoginPage, RegisterPage } from "./AuthPages";
import { investor, renderApp } from "../../test/render";
import { server } from "../../test/server";

test("autentica e direciona o investidor", async () => {
  let submitted: Record<string, unknown> | undefined;
  server.use(
    http.post("*/api/auth/login", async ({ request }) => {
      submitted = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json({
        token: "token",
        tokenType: "Bearer",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: investor,
      });
    }),
  );
  renderApp(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/app/carteiras" element={<h1>Minhas carteiras</h1>} />
    </Routes>,
    "/login",
  );
  const user = userEvent.setup();
  const password = screen.getByLabelText("Senha");
  expect(password).toHaveAttribute("type", "password");
  await user.type(screen.getByLabelText("E-mail"), "ana@example.com");
  await user.type(password, "password123");
  await user.click(screen.getByRole("button", { name: "Mostrar senha" }));
  expect(password).toHaveAttribute("type", "text");
  await user.click(screen.getByRole("button", { name: /entrar/i }));
  expect(
    await screen.findByRole("heading", { name: "Minhas carteiras" }),
  ).toBeInTheDocument();
  expect(submitted).toEqual({
    email: "ana@example.com",
    password: "password123",
  });
});

test("apresenta erros de campos no cadastro", async () => {
  renderApp(<RegisterPage />, "/cadastro");
  const password = screen.getByLabelText("Senha");
  expect(password).toHaveAttribute("type", "password");
  await userEvent.click(screen.getByRole("button", { name: "Mostrar senha" }));
  expect(password).toHaveAttribute("type", "text");
  await userEvent.click(screen.getByRole("button", { name: /criar conta/i }));
  expect(await screen.findByText("Informe seu nome.")).toBeInTheDocument();
  expect(screen.getByText("Informe um e-mail válido.")).toBeInTheDocument();
});
