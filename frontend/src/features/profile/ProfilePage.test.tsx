import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { App } from "../../app/App";
import { investor, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";

const openProfile = () => {
  setSession(investor);
  server.use(http.get("*/api/auth/me", () => HttpResponse.json(investor)));
  renderApp(<App />, "/app/perfil");
};

test("abre o perfil preenchido sem expor senha", async () => {
  openProfile();

  expect(
    await screen.findByRole("heading", { name: "Perfil do investidor" }),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Nome")).toHaveValue(investor.name);
  expect(screen.getByLabelText("E-mail")).toHaveValue(investor.email);
  expect(screen.getByLabelText("Senha atual")).toHaveValue("");
  expect(screen.getByLabelText("Nova senha")).toHaveValue("");
});

test("mantém o perfil acessível no tema escuro e em tela pequena", async () => {
  localStorage.setItem("bom-investidor.theme", "dark");
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    value: 480,
  });
  openProfile();

  await screen.findByRole("heading", { name: "Perfil do investidor" });
  expect(document.documentElement).toHaveAttribute("data-theme", "dark");
  await userEvent.click(screen.getByRole("button", { name: "Abrir menu" }));
  expect(screen.getByRole("link", { name: "Meu perfil" })).toBeInTheDocument();
  expect(screen.getByLabelText("Nome")).toBeVisible();
  expect(screen.getByLabelText("Confirmar nova senha")).toBeVisible();
});

test("atualiza o perfil e a identidade da navegação sem recarregar", async () => {
  const updated = {
    ...investor,
    name: "Ana Atualizada",
    email: "ana.nova@example.com",
  };
  openProfile();
  let submitted: Record<string, unknown> | undefined;
  server.use(
    http.put("*/api/auth/me", async ({ request }) => {
      submitted = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(updated);
    }),
  );
  const user = userEvent.setup();

  await user.clear(await screen.findByLabelText("Nome"));
  await user.type(screen.getByLabelText("Nome"), updated.name);
  await user.clear(screen.getByLabelText("E-mail"));
  await user.type(screen.getByLabelText("E-mail"), updated.email);
  await user.click(screen.getByRole("button", { name: "Salvar alterações" }));

  expect(
    await screen.findByText("Perfil atualizado com sucesso."),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: `Abrir perfil de ${updated.name}` }),
  ).toBeInTheDocument();
  expect(submitted).toEqual({ name: updated.name, email: updated.email });
  expect(sessionStorage.getItem("bom-investidor.session")).toContain(
    updated.email,
  );
});

test("mantém os dados e identifica e-mail já utilizado", async () => {
  openProfile();
  server.use(
    http.put("*/api/auth/me", () =>
      HttpResponse.json(
        {
          status: 409,
          code: "EMAIL_ALREADY_REGISTERED",
          message: "Email is already registered",
          fieldErrors: [],
        },
        { status: 409 },
      ),
    ),
  );
  const user = userEvent.setup();

  await user.clear(await screen.findByLabelText("E-mail"));
  await user.type(screen.getByLabelText("E-mail"), "usado@example.com");
  await user.click(screen.getByRole("button", { name: "Salvar alterações" }));

  expect(
    await screen.findByText("Este e-mail já está sendo utilizado."),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("E-mail")).toHaveValue("usado@example.com");
});

test("valida a confirmação local e limpa os campos sensíveis", async () => {
  openProfile();
  const user = userEvent.setup();

  await user.type(await screen.findByLabelText("Senha atual"), "password123");
  await user.type(screen.getByLabelText("Nova senha"), "new-password");
  await user.type(
    screen.getByLabelText("Confirmar nova senha"),
    "different-password",
  );
  await user.click(screen.getByRole("button", { name: "Salvar alterações" }));

  expect(
    screen.getByText("A confirmação não corresponde à nova senha."),
  ).toBeInTheDocument();
  await waitFor(() =>
    expect(screen.getByLabelText("Senha atual")).toHaveValue(""),
  );
  expect(screen.getByLabelText("Nova senha")).toHaveValue("");
  expect(screen.getByLabelText("Confirmar nova senha")).toHaveValue("");
  expect(screen.getByLabelText("Nome")).toHaveValue(investor.name);
});

test("traduz senha atual inválida e remove valores sensíveis após a API", async () => {
  openProfile();
  server.use(
    http.put("*/api/auth/me", () =>
      HttpResponse.json(
        {
          status: 400,
          code: "VALIDATION_ERROR",
          message: "Request validation failed",
          fieldErrors: [
            {
              field: "currentPassword",
              message: "Current password is invalid",
            },
          ],
        },
        { status: 400 },
      ),
    ),
  );
  const user = userEvent.setup();

  await user.type(
    await screen.findByLabelText("Senha atual"),
    "wrong-password",
  );
  await user.type(screen.getByLabelText("Nova senha"), "new-password");
  await user.type(
    screen.getByLabelText("Confirmar nova senha"),
    "new-password",
  );
  await user.click(screen.getByRole("button", { name: "Salvar alterações" }));

  expect(
    await screen.findByText("A senha atual está incorreta."),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Senha atual")).toHaveValue("");
  expect(screen.getByLabelText("Nova senha")).toHaveValue("");
});
