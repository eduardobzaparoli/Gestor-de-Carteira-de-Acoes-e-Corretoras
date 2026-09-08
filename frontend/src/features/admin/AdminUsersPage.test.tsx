import { screen } from "@testing-library/react";
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
