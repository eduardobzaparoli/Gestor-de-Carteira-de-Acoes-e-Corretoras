import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { Route, Routes } from "react-router-dom";
import { vi } from "vitest";
import { PortfolioPage } from "./PortfolioPage";
import { investor, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";

vi.mock("recharts", () => {
  const Container = ({ children }: { children?: React.ReactNode }) => (
    <div>{children}</div>
  );
  const Empty = () => null;
  return {
    ResponsiveContainer: Container,
    LineChart: Container,
    PieChart: Container,
    Pie: Container,
    Line: Empty,
    Cell: Empty,
    Tooltip: Empty,
    XAxis: Empty,
    YAxis: Empty,
  };
});

const portfolio = {
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
const position = {
  ticker: "PETR4",
  assetName: "Petrobras",
  market: "BR",
  assetType: "STOCK",
  currency: "BRL",
  quantity: 15,
  averagePrice: 13.53,
  custodyCost: 203,
};
const incomeSummary = {
  currencySummaries: [{ currency: "BRL", receivedAmount: 25 }],
  baseCurrency: "BRL",
  consolidatedReceivedAmount: 25,
  exchangeRates: [],
};

function baseHandlers() {
  return [
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/portfolios/p1", () => HttpResponse.json(portfolio)),
    http.get("*/api/portfolios/p1/positions", () =>
      HttpResponse.json([position]),
    ),
    http.get("*/api/portfolios/p1/valuation", () =>
      HttpResponse.json({
        positions: [
          {
            ...position,
            currentPrice: 32.1,
            marketValue: 481.5,
            unrealizedGain: 278.5,
            returnPercentage: 137.19,
            allocationPercentage: 100,
          },
        ],
        currencySummaries: [
          {
            currency: "BRL",
            investedValue: 203,
            marketValue: 481.5,
            unrealizedGain: 278.5,
            returnPercentage: 137.19,
          },
        ],
        consolidatedSummary: {
          baseCurrency: "BRL",
          investedValue: 203,
          marketValue: 481.5,
          totalGain: 278.5,
          returnPercentage: 137.19,
          exchangeRates: [],
          historicalExchangeRates: [],
        },
      }),
    ),
    http.get("*/api/portfolios/p1/value-evolution", () =>
      HttpResponse.json([
        { date: "2026-09-01", investedValue: 203, marketValue: 450 },
        { date: "2026-09-07", investedValue: 203, marketValue: 481.5 },
      ]),
    ),
    http.get("*/api/portfolios/p1/transactions", () => HttpResponse.json([])),
    http.get("*/api/portfolios/p1/income-events/summary", () =>
      HttpResponse.json(incomeSummary),
    ),
    http.get("*/api/assets", () => HttpResponse.json([])),
  ];
}

function renderPortfolio() {
  setSession(investor);
  renderApp(
    <Routes>
      <Route path="/app/carteiras/:portfolioId" element={<PortfolioPage />} />
    </Routes>,
    "/app/carteiras/p1",
  );
}

test("não exibe atribuição americana quando a carteira contém apenas ativos brasileiros", async () => {
  server.use(...baseHandlers());
  renderPortfolio();

  await screen.findByRole("heading", { name: "Longo prazo" });
  expect(
    screen.queryByText(/Logos americanos fornecidos por/i),
  ).not.toBeInTheDocument();
});

test("pesquisa um ativo e registra uma compra", async () => {
  let body: Record<string, string> | undefined;
  server.use(
    http.get("*/api/assets", () =>
      HttpResponse.json([
        {
          id: "a1",
          ticker: "PETR4",
          name: "Petrobras",
          market: "BR",
          assetType: "STOCK",
          currency: "BRL",
          lastQuote: 31.9,
          quotedAt: "2026-09-07T09:00:00Z",
          createdAt: "2026-09-07T09:00:00Z",
          updatedAt: "2026-09-07T09:00:00Z",
        },
      ]),
    ),
    http.get("*/api/assets/a1/quote", () =>
      HttpResponse.json({
        assetId: "a1",
        ticker: "PETR4",
        market: "BR",
        currency: "BRL",
        price: 32.1,
        quotedAt: "2026-09-07T10:00:00Z",
      }),
    ),
    http.post("*/api/portfolios/p1/transactions", async ({ request }) => {
      body = (await request.json()) as Record<string, string>;
      return HttpResponse.json(
        {
          id: "t1",
          ...position,
          type: "BUY",
          status: "EFFECTIVE",
          transactionDate: body.transactionDate,
          quantity: body.quantity,
          unitPrice: body.unitPrice,
          costs: body.costs,
          createdAt: "2026-09-07T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
        { status: 201 },
      );
    }),
    ...baseHandlers(),
  );
  renderPortfolio();

  await userEvent.click(
    await screen.findByRole("button", { name: "Novo lançamento" }),
  );
  await userEvent.type(screen.getByLabelText("Pesquisar ativo"), "PETR");
  await waitFor(() =>
    expect(
      screen
        .getAllByRole("button")
        .some(
          (button) =>
            button.classList.contains("asset-result") &&
            button.textContent?.includes("PETR4"),
        ),
    ).toBe(true),
  );
  const assetButton = screen
    .getAllByRole("button")
    .find(
      (button) =>
        button.classList.contains("asset-result") &&
        button.textContent?.includes("PETR4"),
    )!;
  await userEvent.click(assetButton);
  await userEvent.type(screen.getByLabelText("Quantidade"), "2");
  await userEvent.click(
    screen.getByRole("button", { name: "Registrar lançamento" }),
  );

  await waitFor(() => expect(body).toBeDefined());
  expect(body).toMatchObject({
    registeredAssetId: "a1",
    type: "BUY",
    quantity: "2",
    unitPrice: "32.10",
    costs: "0.00",
  });
});

test("limpa o formulário ao reabrir um novo lançamento", async () => {
  server.use(...baseHandlers());
  renderPortfolio();

  await userEvent.click(
    await screen.findByRole("button", { name: "Novo lançamento" }),
  );
  await userEvent.type(screen.getByLabelText("Pesquisar ativo"), "PETR");
  await userEvent.type(screen.getByLabelText("Quantidade"), "5");
  await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Novo lançamento" }),
  );

  expect(screen.getByLabelText("Pesquisar ativo")).toHaveValue("");
  expect(screen.getByLabelText("Quantidade")).toHaveValue("");
  expect((screen.getByLabelText("Data") as HTMLInputElement).value).toMatch(
    /^\d{2}\/\d{2}\/\d{4}$/,
  );
});

test("bloqueia o lançamento quando a cotação fresca falha", async () => {
  server.use(
    http.get("*/api/assets", () =>
      HttpResponse.json([
        {
          id: "a1",
          ticker: "PETR4",
          name: "Petrobras",
          market: "BR",
          assetType: "STOCK",
          currency: "BRL",
          lastQuote: 31.9,
          quotedAt: "2026-09-07T09:00:00Z",
          createdAt: "2026-09-07T09:00:00Z",
          updatedAt: "2026-09-07T09:00:00Z",
        },
      ]),
    ),
    http.get("*/api/assets/a1/quote", () =>
      HttpResponse.json(
        {
          status: 503,
          code: "ASSET_QUOTE_UNAVAILABLE",
          message: "Asset quote is unavailable",
          fieldErrors: [],
        },
        { status: 503 },
      ),
    ),
    ...baseHandlers(),
  );
  renderPortfolio();

  await userEvent.click(
    await screen.findByRole("button", { name: "Novo lançamento" }),
  );
  await userEvent.type(screen.getByLabelText("Pesquisar ativo"), "PETR");
  const assetButton = screen
    .getAllByRole("button")
    .find(
      (button) =>
        button.classList.contains("asset-result") &&
        button.textContent?.includes("PETR4"),
    )!;
  await userEvent.click(assetButton);

  expect(
    await screen.findByText(
      "Não foi possível atualizar a cotação. Tente novamente mais tarde.",
    ),
  ).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Registrar lançamento" }),
  ).toBeDisabled();
});

test("edita lançamento pendente com data e moeda em formato brasileiro", async () => {
  let body: Record<string, string> | undefined;
  server.use(
    http.get("*/api/portfolios/p1/transactions", () =>
      HttpResponse.json([
        {
          id: "t1",
          ...position,
          type: "BUY",
          status: "PENDING",
          transactionDate: "2026-09-10",
          quantity: 5,
          unitPrice: 22.8,
          costs: 0,
          createdAt: "2026-09-07T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
      ]),
    ),
    http.put("*/api/portfolios/p1/transactions/t1", async ({ request }) => {
      body = (await request.json()) as Record<string, string>;
      return HttpResponse.json({ id: "t1", ...body, status: "PENDING" });
    }),
    ...baseHandlers(),
  );
  renderPortfolio();

  await userEvent.click(
    await screen.findByRole("button", { name: /Lançamentos/ }),
  );
  await userEvent.click(await screen.findByRole("button", { name: "Editar" }));
  expect(screen.getByLabelText("Data")).toHaveValue("10/09/2026");
  expect(
    screen.getByRole("button", { name: "Abrir calendário de data" }),
  ).toBeInTheDocument();
  const price = screen.getByLabelText("Preço unitário (BRL)");
  await userEvent.clear(price);
  await userEvent.type(price, "25,50");
  await userEvent.tab();
  expect((price as HTMLInputElement).value).toMatch(/R\$\s25,50/);
  const costs = screen.getByLabelText("Custos (BRL)");
  await userEvent.clear(costs);
  await userEvent.type(costs, "1,25");
  await userEvent.tab();
  expect((costs as HTMLInputElement).value).toMatch(/R\$\s1,25/);
  await userEvent.click(
    screen.getByRole("button", { name: "Salvar alterações" }),
  );

  await waitFor(() => expect(body).toBeDefined());
  expect(body).toMatchObject({
    transactionDate: "2026-09-10",
    quantity: "5",
    unitPrice: "25.50",
    costs: "1.25",
  });
});

test("completa o ano de uma data parcial ao sair do campo", async () => {
  server.use(
    http.get("*/api/portfolios/p1/transactions", () =>
      HttpResponse.json([
        {
          id: "t1",
          ...position,
          type: "BUY",
          status: "PENDING",
          transactionDate: "2026-09-10",
          quantity: 5,
          unitPrice: 22.8,
          costs: 0,
          createdAt: "2026-09-07T10:00:00Z",
          updatedAt: "2026-09-07T10:00:00Z",
        },
      ]),
    ),
    ...baseHandlers(),
  );
  renderPortfolio();
  await userEvent.click(
    await screen.findByRole("button", { name: /Lançamentos/ }),
  );
  await userEvent.click(await screen.findByRole("button", { name: "Editar" }));
  const input = screen.getByLabelText("Data");
  await userEvent.clear(input);
  await userEvent.type(input, "0809");
  await userEvent.tab();
  expect(input).toHaveValue(`08/09/${new Date().getFullYear()}`);
});

test("exibe e edita um ativo americano em reais sem alterar a moeda nativa da API", async () => {
  let body: Record<string, string> | undefined;
  server.use(
    http.get("*/api/portfolios/p1/exchange-rates", () =>
      HttpResponse.json({
        sourceCurrency: "USD",
        targetCurrency: "BRL",
        rate: 5.12,
        referenceDate: "2026-09-08",
      }),
    ),
    http.get("*/api/assets", () =>
      HttpResponse.json([
        {
          id: "us1",
          ticker: "AAPL",
          name: "Apple Inc.",
          market: "US",
          assetType: "STOCK",
          currency: "USD",
          lastQuote: 316.51,
          quotedAt: "2026-09-07T09:00:00Z",
          createdAt: "2026-09-07T09:00:00Z",
          updatedAt: "2026-09-07T09:00:00Z",
        },
      ]),
    ),
    http.get("*/api/assets/us1/quote", () =>
      HttpResponse.json({
        assetId: "us1",
        ticker: "AAPL",
        market: "US",
        currency: "USD",
        price: 316.51,
        quotedAt: "2026-09-07T10:00:00Z",
      }),
    ),
    http.post("*/api/portfolios/p1/transactions", async ({ request }) => {
      body = (await request.json()) as Record<string, string>;
      return HttpResponse.json(
        { id: "us-t1", ...body, type: "BUY", status: "EFFECTIVE" },
        { status: 201 },
      );
    }),
    ...baseHandlers(),
  );
  renderPortfolio();
  await userEvent.click(
    await screen.findByRole("button", { name: "Novo lançamento" }),
  );
  await userEvent.selectOptions(screen.getByLabelText("Mercado"), "US");
  await userEvent.type(screen.getByLabelText("Pesquisar ativo"), "AAPL");
  expect(
    screen.queryByText(/Logos americanos fornecidos por/i),
  ).not.toBeInTheDocument();
  const asset = await screen.findByRole("button", {
    name: /AAPL.*R\$\s*1\.620,53/i,
  });
  await userEvent.click(asset);
  expect(
    (screen.getByLabelText("Preço unitário (BRL)") as HTMLInputElement).value,
  ).toMatch(/R\$\s*1\.620,53/);
  await userEvent.type(screen.getByLabelText("Quantidade"), "1");
  await userEvent.click(
    screen.getByRole("button", { name: "Registrar lançamento" }),
  );
  await waitFor(() => expect(body).toBeDefined());
  expect(body?.unitPrice).toBe("316.51");
});

test("oferece no provento manual apenas os ativos em custódia", async () => {
  server.use(
    ...baseHandlers(),
    http.get("*/api/portfolios/p1/income-events", () => HttpResponse.json([])),
    http.get("*/api/portfolios/p1/income-events/candidates", () =>
      HttpResponse.json([]),
    ),
  );
  renderPortfolio();
  await userEvent.click(
    await screen.findByRole("button", { name: "Proventos" }),
  );
  await userEvent.click(await screen.findByRole("button", { name: "Manual" }));
  expect(
    screen.getByRole("option", { name: /PETR4 — Petrobras/ }),
  ).toBeInTheDocument();
  expect(
    (screen.getByLabelText("Data de pagamento") as HTMLInputElement).value,
  ).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
  expect(
    (
      screen
        .getAllByLabelText("Valor recebido (BRL)")
        .find((item) =>
          (item as HTMLInputElement).value.includes("R$"),
        ) as HTMLInputElement
    ).value,
  ).toMatch(/R\$\s*0,00/);
});

test("confirma um candidato de provento retornado pela API", async () => {
  let body: Record<string, string> | undefined;
  server.use(
    ...baseHandlers(),
    http.get("*/api/portfolios/p1/income-events", () => HttpResponse.json([])),
    http.get("*/api/portfolios/p1/income-events/candidates", () =>
      HttpResponse.json([
        {
          candidateId: "c1",
          ticker: "PETR4",
          assetName: "Petrobras",
          market: "BR",
          assetType: "STOCK",
          currency: "BRL",
          type: "DIVIDEND",
          source: "BRAPI",
          unitAmount: 1.5,
          eligibilityDate: "2026-09-01",
          paymentDate: "2026-09-07",
          eligibleQuantity: 15,
          expectedAmount: 22.5,
          confirmable: true,
          alreadyRecorded: false,
        },
      ]),
    ),
    http.post(
      "*/api/portfolios/p1/income-events/confirmations",
      async ({ request }) => {
        body = (await request.json()) as Record<string, string>;
        return HttpResponse.json({ id: "i1" }, { status: 201 });
      },
    ),
  );
  renderPortfolio();

  await userEvent.click(
    await screen.findByRole("button", { name: "Proventos" }),
  );
  await userEvent.click(
    await screen.findByRole("button", { name: "Confirmar" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar recebimento" }),
  );

  await waitFor(() => expect(body).toBeDefined());
  expect(body).toMatchObject({ candidateId: "c1", receivedAmount: "22.50" });
});
