import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { PortfoliosPage } from "./PortfoliosPage";
import { investor, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";

test("orienta o primeiro uso quando não há carteiras", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () => HttpResponse.json([])),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
  );
  renderApp(<PortfoliosPage />, "/app/carteiras");
  expect(
    await screen.findByText("Sua jornada começa com uma carteira"),
  ).toBeInTheDocument();
});

test("cria uma carteira vinculada à corretora selecionada", async () => {
  let body: Record<string, string> | undefined;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () => HttpResponse.json([])),
    http.get("*/api/brokerages", () =>
      HttpResponse.json([
        {
          id: "b1",
          nickname: "Principal",
          cnpj: "61384004000105",
          legalName: "Corretora",
          tradeName: "Principal",
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
    http.post("*/api/portfolios", async ({ request }) => {
      body = (await request.json()) as Record<string, string>;
      return HttpResponse.json({ id: "p1" }, { status: 201 });
    }),
  );
  renderApp(<PortfoliosPage />, "/app/carteiras");
  await userEvent.click(
    await screen.findByRole("button", { name: "Nova carteira" }),
  );
  expect(screen.getByRole("option", { name: "Principal" })).toBeInTheDocument();
  expect(screen.queryByRole("option", { name: /Corretora/ })).toBeNull();
  await userEvent.type(screen.getByLabelText("Nome da carteira"), "Reserva");
  await userEvent.click(screen.getByRole("button", { name: "Criar carteira" }));
  await waitFor(() => expect(body).toBeDefined());
  expect(body).toEqual({ name: "Reserva", brokerageId: "b1" });
});

test("exclui uma carteira somente após confirmação", async () => {
  let deleted = false;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () =>
      HttpResponse.json([
        {
          id: "p1",
          name: "Longo prazo",
          brokerage: {
            id: "b1",
            nickname: "Principal",
            cnpj: "61384004000105",
            legalName: "Corretora",
          },
          createdAt: "2026-09-01T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
      ]),
    ),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
    http.delete("*/api/portfolios/p1", () => {
      deleted = true;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderApp(<PortfoliosPage />, "/app/carteiras");
  await userEvent.click(
    await screen.findByRole("button", { name: "Excluir Longo prazo" }),
  );
  expect(deleted).toBe(false);
  await userEvent.click(
    screen.getByRole("button", { name: "Excluir definitivamente" }),
  );
  await waitFor(() => expect(deleted).toBe(true));
});

test("mostra uma carteira retornada pela API", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () =>
      HttpResponse.json([
        {
          id: "p1",
          name: "Longo prazo",
          brokerage: {
            id: "b1",
            nickname: "Principal",
            cnpj: "61384004000105",
            legalName: "Corretora",
          },
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      ]),
    ),
    http.get("*/api/brokerages", () => HttpResponse.json([])),
  );
  renderApp(<PortfoliosPage />, "/app/carteiras");
  expect(
    await screen.findByRole("heading", { name: "Longo prazo" }),
  ).toBeInTheDocument();
  expect(screen.getByText("Principal")).toBeInTheDocument();
});

test("edita nome e corretora e atualiza o cartão sem recarregar a página", async () => {
  setSession(investor);
  let requestBody: Record<string, string> | undefined;
  let portfolio = {
    id: "p1",
    name: "Longo prazo",
    brokerage: {
      id: "b1",
      nickname: "Principal",
      cnpj: "61384004000105",
      legalName: "Corretora Principal",
    },
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-07T10:00:00Z",
  };
  const brokerages = [
    { id: "b1", nickname: "Principal", cnpj: "61384004000105" },
    { id: "b2", nickname: "Internacional", cnpj: "04252011000110" },
  ].map((item) => ({
    ...item,
    legalName: `Corretora ${item.nickname}`,
    tradeName: item.nickname,
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
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
  }));
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () => HttpResponse.json([portfolio])),
    http.get("*/api/brokerages", () => HttpResponse.json(brokerages)),
    http.put("*/api/portfolios/p1", async ({ request }) => {
      requestBody = (await request.json()) as Record<string, string>;
      portfolio = {
        ...portfolio,
        name: requestBody.name,
        brokerage: {
          id: "b2",
          nickname: "Internacional",
          cnpj: "04252011000110",
          legalName: "Corretora Internacional",
        },
        updatedAt: "2026-09-12T12:00:00Z",
      };
      return HttpResponse.json(portfolio);
    }),
  );

  renderApp(<PortfoliosPage />, "/app/carteiras");
  await userEvent.click(
    await screen.findByRole("button", { name: "Editar Longo prazo" }),
  );
  const name = screen.getByLabelText("Nome da carteira");
  expect(name).toHaveValue("Longo prazo");
  expect(screen.getByLabelText("Corretora")).toHaveValue("b1");
  await userEvent.clear(name);
  await userEvent.type(name, "Reserva global");
  await userEvent.selectOptions(screen.getByLabelText("Corretora"), "b2");
  await userEvent.click(
    screen.getByRole("button", { name: "Salvar alterações" }),
  );

  await waitFor(() =>
    expect(requestBody).toEqual({
      name: "Reserva global",
      brokerageId: "b2",
    }),
  );
  expect(
    await screen.findByRole("heading", { name: "Reserva global" }),
  ).toBeInTheDocument();
  expect(screen.getByText("Internacional")).toBeInTheDocument();
});

test("mantém o formulário aberto e os valores quando a edição falha", async () => {
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios", () =>
      HttpResponse.json([
        {
          id: "p1",
          name: "Longo prazo",
          brokerage: {
            id: "b1",
            nickname: "Principal",
            cnpj: "1",
            legalName: "Principal",
          },
          createdAt: "2026-09-01T10:00:00Z",
          updatedAt: "2026-09-01T10:00:00Z",
        },
      ]),
    ),
    http.get("*/api/brokerages", () =>
      HttpResponse.json([{ id: "b1", nickname: "Principal" }]),
    ),
    http.put("*/api/portfolios/p1", () =>
      HttpResponse.json(
        {
          status: 409,
          code: "PORTFOLIO_NAME_ALREADY_REGISTERED",
          message: "Nome já cadastrado",
          fieldErrors: [],
        },
        { status: 409 },
      ),
    ),
  );

  renderApp(<PortfoliosPage />, "/app/carteiras");
  await userEvent.click(
    await screen.findByRole("button", { name: "Editar Longo prazo" }),
  );
  const name = screen.getByLabelText("Nome da carteira");
  await userEvent.clear(name);
  await userEvent.type(name, "Duplicada");
  await userEvent.click(
    screen.getByRole("button", { name: "Salvar alterações" }),
  );

  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Já existe uma carteira com esse nome.",
  );
  expect(name).toHaveValue("Duplicada");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});
