import { expect, test } from "@playwright/test";
test("realiza a jornada de login responsiva", async ({ page }, testInfo) => {
  await page.addInitScript(() => {
    if (!localStorage.getItem("bom-investidor.theme")) {
      localStorage.setItem("bom-investidor.theme", "light");
    }
  });
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
  await page.getByRole("button", { name: "Ativar modo escuro" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Ativar modo claro" }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("login-dark.png"),
    fullPage: true,
  });
  await page.getByLabel("E-mail").fill("ana@example.com");
  await page.getByLabel("Senha").fill("password123");
  await page.getByRole("button", { name: /entrar/i }).click();
  await expect(page.getByRole("heading", { name: /Olá, Ana/ })).toBeVisible();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  const hasHorizontalOverflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
  expect(hasHorizontalOverflow).toBe(false);
});
