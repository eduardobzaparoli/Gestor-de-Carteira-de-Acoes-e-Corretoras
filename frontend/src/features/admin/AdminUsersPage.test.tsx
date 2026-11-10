import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { AdminUsersPage } from "./AdminUsersPage";
import { admin, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";

test("lista somente dados administrativos dos usuários", async () => {
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () =>
      HttpResponse.json([
        {
          ...admin,
          status: "ACTIVE",
          createdAt: "2026-09-01T10:00:00Z",
          updatedAt: "2026-09-01T10:00:00Z",
        },
      ]),
    ),
  );
  renderApp(<AdminUsersPage />, "/admin/usuarios");
  expect(await screen.findByText("admin@example.com")).toBeInTheDocument();
  expect(screen.getAllByText("Administrador").length).toBeGreaterThan(0);
  expect(screen.queryByText(/carteira/i)).not.toBeInTheDocument();
});

test("reinicia a senha oculta ao reabrir o formulário de cadastro", async () => {
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () =>
      HttpResponse.json([
        {
          ...admin,
          status: "ACTIVE",
          createdAt: "2026-09-01T10:00:00Z",
          updatedAt: "2026-09-01T10:00:00Z",
        },
      ]),
    ),
  );
  renderApp(<AdminUsersPage />, "/admin/usuarios");
  const user = userEvent.setup();

  await screen.findByText("admin@example.com");
  await user.click(screen.getByRole("button", { name: "Novo usuário" }));
  let password = screen.getByLabelText("Senha");
  await user.type(password, "password123");
  await user.click(screen.getByRole("button", { name: "Mostrar senha" }));
  expect(password).toHaveAttribute("type", "text");

  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  await user.click(screen.getByRole("button", { name: "Novo usuário" }));
  password = screen.getByLabelText("Senha");
  expect(password).toHaveAttribute("type", "password");
  expect(password).toHaveValue("");

  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  await user.click(screen.getByRole("button", { name: "Editar" }));
  expect(screen.queryByLabelText("Senha")).not.toBeInTheDocument();
});

test("separa a edição cadastral da redefinição de senha", async () => {
  const current = {
    ...admin,
    status: "ACTIVE" as const,
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
  };
  let editBody: unknown;
  let passwordBody: unknown;
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () => HttpResponse.json([current])),
    http.put("*/api/admin/users/:id", async ({ request }) => {
      editBody = await request.json();
      return HttpResponse.json(current);
    }),
    http.put("*/api/admin/users/:id/password", async ({ request }) => {
      passwordBody = await request.json();
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderApp(<AdminUsersPage />, "/admin/usuarios");
  const user = userEvent.setup();

  await screen.findByText("admin@example.com");
  await user.click(screen.getByRole("button", { name: "Editar" }));
  expect(screen.queryByLabelText("Senha")).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Salvar alterações" }));
  await waitFor(() =>
    expect(editBody).toEqual({
      name: admin.name,
      email: admin.email,
      role: admin.role,
    }),
  );

  await user.click(screen.getByRole("button", { name: "Senha" }));
  await user.type(screen.getByLabelText("Nova senha"), "new-password");
  await user.click(screen.getByRole("button", { name: "Redefinir senha" }));
  await waitFor(() =>
    expect(passwordBody).toEqual({ password: "new-password" }),
  );
  await waitFor(() =>
    expect(
      screen
        .getByRole("heading", {
          name: "Redefinir senha",
          level: 2,
          hidden: true,
        })
        .closest("dialog"),
    ).not.toHaveAttribute("open"),
  );
  expect(screen.getByLabelText("Nova senha")).toHaveValue("");
});

test("limpa a nova senha quando a redefinição falha", async () => {
  const current = {
    ...admin,
    status: "ACTIVE" as const,
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
  };
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () => HttpResponse.json([current])),
    http.put("*/api/admin/users/:id/password", () =>
      HttpResponse.json(
        {
          status: 500,
          code: "UNEXPECTED_ERROR",
          message: "Unexpected error",
          fieldErrors: [],
        },
        { status: 500 },
      ),
    ),
  );
  renderApp(<AdminUsersPage />, "/admin/usuarios");
  const user = userEvent.setup();

  await screen.findByText("admin@example.com");
  await user.click(screen.getByRole("button", { name: "Senha" }));
  const password = screen.getByLabelText("Nova senha");
  await user.type(password, "new-password");
  await user.click(screen.getByRole("button", { name: "Redefinir senha" }));

  await waitFor(() => expect(password).toHaveValue(""));
  expect(
    screen.getByText("Não foi possível concluir a operação."),
  ).toBeInTheDocument();
});

test("explica a proteção do último administrador ativo", async () => {
  const current = {
    ...admin,
    status: "ACTIVE" as const,
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
  };
  setSession(admin);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(admin)),
    http.get("*/api/admin/users", () => HttpResponse.json([current])),
    http.delete("*/api/admin/users/:id", () =>
      HttpResponse.json(
        {
          status: 409,
          code: "LAST_ACTIVE_ADMIN",
          message: "Last active administrator",
          fieldErrors: [],
        },
        { status: 409 },
      ),
    ),
  );

  renderApp(<AdminUsersPage />, "/admin/usuarios");
  await userEvent.click(
    await screen.findByRole("button", { name: "Desativar" }),
  );
  await userEvent.click(screen.getByRole("button", { name: "Confirmar" }));
  expect(
    await screen.findByText(
      "O último administrador ativo não pode perder esse papel.",
    ),
  ).toBeInTheDocument();
});
