import { expect, test } from "@playwright/test";
test("realiza a jornada de login responsiva", async ({ page }) => {
  await page.route("**/api/auth/login", async (route) =>
    route.fulfill({
      json: {
        token: "token",
        tokenType: "Bearer",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: {
          id: "1",
          name: "Ana",
          email: "ana@example.com",
          role: "INVESTOR",
        },
      },
    }),
  );
  await page.route("**/api/portfolios", async (route) =>
    route.fulfill({ json: [] }),
  );
  await page.route("**/api/brokerages", async (route) =>
    route.fulfill({ json: [] }),
  );
  await page.goto("/login");
  await page.getByLabel("E-mail").fill("ana@example.com");
  await page.getByLabel("Senha").fill("password123");
  await page.getByRole("button", { name: /entrar/i }).click();
  await expect(page.getByRole("heading", { name: /Olá, Ana/ })).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
  expect(hasHorizontalOverflow).toBe(false);
});
