import { expect, test } from "@playwright/test";

const investor = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Ana Investidora",
  email: "ana@example.com",
  role: "INVESTOR",
};

test("apresenta o dashboard financeiro com dados representativos", async ({
  page,
}, testInfo) => {
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path === "/api/auth/login") {
      return route.fulfill({
        json: {
          token: "token",
          tokenType: "Bearer",
          expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
          user: investor,
        },
      });
    }
    if (path === "/api/portfolios") {
      return route.fulfill({
        json: [
          {
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
          },
        ],
      });
    }
    if (path === "/api/brokerages") return route.fulfill({ json: [] });
    if (path === "/api/portfolios/p1") {
      return route.fulfill({
        json: {
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
        },
      });
    }
    if (path.endsWith("/positions")) {
      return route.fulfill({
        json: [
          {
            ticker: "PETR4",
            assetName: "Petrobras",
            market: "BR",
            assetType: "STOCK",
            currency: "BRL",
            quantity: 15,
            averagePrice: 13.53,
            custodyCost: 203,
          },
        ],
      });
    }
    if (path.endsWith("/valuation")) {
      return route.fulfill({
        json: {
          positions: [
            {
              ticker: "PETR4",
              assetName: "Petrobras",
              market: "BR",
              assetType: "STOCK",
              currency: "BRL",
              quantity: 15,
              averagePrice: 13.53,
              custodyCost: 203,
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
        },
      });
    }
    if (path.endsWith("/value-evolution")) {
      return route.fulfill({
        json: [
          { date: "2026-09-01", investedValue: 203, marketValue: 450 },
          { date: "2026-09-07", investedValue: 203, marketValue: 481.5 },
        ],
      });
    }
    if (path.endsWith("/transactions")) return route.fulfill({ json: [] });
    if (path.endsWith("/income-events/summary")) {
      return route.fulfill({
        json: {
          currencySummaries: [{ currency: "BRL", receivedAmount: 25 }],
          baseCurrency: "BRL",
          consolidatedReceivedAmount: 25,
          exchangeRates: [],
        },
      });
    }
    return route.fulfill({ status: 404, json: { message: "Not mocked" } });
  });

  await page.goto("/login");
  await page.getByLabel("E-mail").fill("ana@example.com");
  await page.getByLabel("Senha").fill("password123");
  await page.getByRole("button", { name: /entrar/i }).click();
  await page.getByRole("link", { name: /ver dashboard/i }).click();

  await expect(
    page.getByRole("heading", { name: "Longo prazo" }),
  ).toBeVisible();
  await expect(page.getByText("R$ 481,50").first()).toBeVisible();
  await expect(page.getByText("137,19%").first()).toBeVisible();
  await expect(page.getByRole("heading", { name: "Composição" })).toBeVisible();
  await expect(page.getByText("PETR4", { exact: true }).first()).toBeVisible();

  const hasHorizontalOverflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
  expect(hasHorizontalOverflow).toBe(false);
  await page.screenshot({
    path: testInfo.outputPath("dashboard.png"),
    fullPage: true,
  });
});
