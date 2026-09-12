import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { investor, renderApp, setSession } from "../../test/render";
import { server } from "../../test/server";
import { AssetsPage } from "./AssetsPage";

test("cadastra, filtra e atualiza a cotação de um ativo", async () => {
  let createBody: { assetSelectionId: string } | undefined;
  let registered = false;
  let quote = 35.1;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/assets", ({ request }) => {
      if (!registered) return HttpResponse.json([]);
      const market = new URL(request.url).searchParams.get("market");
      if (market === "US") return HttpResponse.json([]);
      return HttpResponse.json([
        {
          id: "asset-1",
          ticker: "PETR4",
          name: "Petrobras PN",
          market: "BR",
          assetType: "STOCK",
          currency: "BRL",
          lastQuote: quote,
          quotedAt: "2026-09-11T15:30:00Z",
          createdAt: "2026-09-11T15:30:00Z",
          updatedAt: "2026-09-11T15:30:00Z",
        },
      ]);
    }),
    http.get("*/api/assets/search", () =>
      HttpResponse.json([
        {
          selectionId: "selection-1",
          ticker: "PETR4",
          name: "Petrobras PN",
          market: "BR",
          assetType: "STOCK",
          currency: "BRL",
          price: 35.1,
        },
      ]),
    ),
    http.post("*/api/assets", async ({ request }) => {
      createBody = (await request.json()) as { assetSelectionId: string };
      registered = true;
      return HttpResponse.json({ id: "asset-1" }, { status: 201 });
    }),
    http.post("*/api/assets/asset-1/quote-refresh", () => {
      quote = 36.25;
      return HttpResponse.json({ id: "asset-1", lastQuote: quote });
    }),
  );

  renderApp(<AssetsPage />, "/app/ativos");
  await userEvent.click(
    await screen.findByRole("button", { name: /cadastrar ativo/i }),
  );
  await userEvent.type(screen.getByLabelText("Pesquisar ativo"), "PETR");
  const result = await screen.findByRole("button", {
    name: /PETR4.*Petrobras/i,
  });
  await userEvent.click(result);
  await userEvent.click(
    screen.getAllByRole("button", { name: /^Cadastrar ativo$/i }).at(-1)!,
  );

  await waitFor(() =>
    expect(createBody).toEqual({ assetSelectionId: "selection-1" }),
  );
  expect(await screen.findByRole("heading", { name: "PETR4" })).toBeVisible();
  await userEvent.click(
    screen.getByRole("button", { name: /Atualizar cotação/i }),
  );
  await waitFor(() => expect(screen.getByText(/36,25/)).toBeVisible());
  await userEvent.click(screen.getByRole("button", { name: "Americanos" }));
  expect(await screen.findByText("Nenhum ativo neste mercado")).toBeVisible();
});

test("exibe cotação americana em reais e permite excluir o ativo", async () => {
  let deleted = false;
  setSession(investor);
  server.use(
    http.get("*/api/auth/me", () => HttpResponse.json(investor)),
    http.get("*/api/assets", () =>
      HttpResponse.json(
        deleted
          ? []
          : [
              {
                id: "us-1",
                ticker: "MSFT",
                name: "Microsoft Corporation",
                market: "US",
                assetType: "STOCK",
                currency: "USD",
                lastQuote: 100,
                quotedAt: "2026-09-12T03:17:00Z",
                createdAt: "2026-09-12T03:17:00Z",
                updatedAt: "2026-09-12T03:17:00Z",
              },
            ],
      ),
    ),
    http.get("*/api/assets/exchange-rate", () =>
      HttpResponse.json({
        sourceCurrency: "USD",
        targetCurrency: "BRL",
        rate: 5.1,
        referenceDate: "2026-09-11",
      }),
    ),
    http.delete("*/api/assets/us-1", () => {
      deleted = true;
      return new HttpResponse(null, { status: 204 });
    }),
  );

  renderApp(<AssetsPage />, "/app/ativos");
  expect(await screen.findByText(/R\$\s*510,00/)).toBeVisible();
  expect(
    screen.queryByText(/Logos americanos fornecidos por/i),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: /^Excluir$/i }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar exclusão" }),
  );
  expect(await screen.findByText("Cadastre seu primeiro ativo")).toBeVisible();
});
