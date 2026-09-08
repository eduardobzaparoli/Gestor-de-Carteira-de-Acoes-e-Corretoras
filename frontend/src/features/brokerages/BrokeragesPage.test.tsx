import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { BrokeragesPage } from "./BrokeragesPage";
import { investor, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";

test("consulta o CEP e cadastra uma corretora validada", async () => {
  let requestBody: Record<string, string> | undefined;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
    http.get("*/api/brokerages/cnpj", () =>
      HttpResponse.json({
        cnpj: "61384004000105",
        legalName: "Corretora Principal S.A.",
        tradeName: "Principal",
      }),
    ),
    http.get("*/api/brokerages/cep/01310100", () =>
      HttpResponse.json({
        cep: "01310100",
        street: "Avenida Paulista",
        neighborhood: "Bela Vista",
        city: "São Paulo",
        state: "SP",
      }),
    ),
    http.post("*/api/brokerages", async ({ request }) => {
      requestBody = (await request.json()) as Record<string, string>;
      return HttpResponse.json(
        {
          id: "b1",
          nickname: requestBody.nickname,
          cnpj: requestBody.cnpj,
          legalName: "Corretora Principal S.A.",
          tradeName: "Principal",
          registrationStatus: "ATIVA",
          cvmParticipantCategory: "Corretora",
          address: {
            cep: requestBody.cep,
            street: requestBody.street,
            neighborhood: requestBody.neighborhood,
            number: requestBody.number,
            city: "São Paulo",
            state: "SP",
          },
          createdAt: "2026-09-07T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
        { status: 201 },
      );
    }),
  );

  renderApp(<BrokeragesPage />, "/app/corretoras");
  await userEvent.click(
    await screen.findByRole("button", { name: /cadastrar corretora/i }),
  );
  await userEvent.type(screen.getByLabelText("Apelido"), "Principal");
  await userEvent.type(screen.getByLabelText("CNPJ"), "61384004000105");
  await userEvent.click(
    screen.getByRole("button", { name: /^consultar cnpj$/i }),
  );
  expect(
    await screen.findByDisplayValue("Corretora Principal S.A."),
  ).toHaveAttribute("readonly");
  await userEvent.type(screen.getByLabelText("CEP"), "01310100");
  await userEvent.click(screen.getByRole("button", { name: /^consultar$/i }));
  expect(await screen.findByDisplayValue("Avenida Paulista")).toHaveAttribute(
    "readonly",
  );
  expect(screen.getByText(/São Paulo\/SP/)).toBeInTheDocument();
  await userEvent.type(screen.getByLabelText("Número"), "1000");
  await userEvent.click(
    screen.getByRole("button", { name: /validar e cadastrar/i }),
  );

  await waitFor(() => expect(requestBody).toBeDefined());
  expect(requestBody).toMatchObject({
    nickname: "Principal",
    cnpj: "61384004000105",
    cep: "01310100",
    number: "1000",
  });
});

test("exclui uma corretora somente após confirmação", async () => {
  let deleted = false;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/brokerages", () =>
      HttpResponse.json([
        {
          id: "b1",
          nickname: "Principal",
          cnpj: "61384004000105",
          legalName: "Corretora Principal S.A.",
          registrationStatus: "ATIVA",
          cvmParticipantCategory: "Corretora",
          address: {
            cep: "01310100",
            street: "Avenida Paulista",
            neighborhood: "Bela Vista",
            number: "1000",
            city: "São Paulo",
            state: "SP",
          },
          createdAt: "2026-09-07T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
      ]),
    ),
    http.delete("*/api/brokerages/b1", () => {
      deleted = true;
      return new HttpResponse(null, { status: 204 });
    }),
  );

  renderApp(<BrokeragesPage />, "/app/corretoras");
  await userEvent.click(await screen.findByRole("button", { name: "Excluir" }));
  expect(deleted).toBe(false);
  await userEvent.click(
    screen.getByRole("button", { name: "Excluir corretora" }),
  );
  await waitFor(() => expect(deleted).toBe(true));
});

test("apresenta em português a ausência de campo obrigatório", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
    http.get("*/api/brokerages/cnpj", () =>
      HttpResponse.json({
        cnpj: "61384004000105",
        legalName: "Corretora Principal S.A.",
      }),
    ),
    http.get("*/api/brokerages/cep/01310100", () =>
      HttpResponse.json({
        cep: "01310100",
        street: "Avenida Paulista",
        neighborhood: "Bela Vista",
        city: "São Paulo",
        state: "SP",
      }),
    ),
  );
  renderApp(<BrokeragesPage />, "/app/corretoras");
  await userEvent.click(
    await screen.findByRole("button", { name: /cadastrar corretora/i }),
  );
  await userEvent.type(screen.getByLabelText("Apelido"), "Principal");
  await userEvent.type(screen.getByLabelText("CNPJ"), "61384004000105");
  await userEvent.click(
    screen.getByRole("button", { name: /^consultar cnpj$/i }),
  );
  await screen.findByDisplayValue("Corretora Principal S.A.");
  await userEvent.type(screen.getByLabelText("CEP"), "01310100");
  await userEvent.click(screen.getByRole("button", { name: /^consultar$/i }));
  await screen.findByDisplayValue("Avenida Paulista");
  await userEvent.click(
    screen.getByRole("button", { name: /validar e cadastrar/i }),
  );
  expect(await screen.findByText("Informe o número.")).toBeInTheDocument();
  expect(screen.queryByText(/please fill out/i)).not.toBeInTheDocument();
});
