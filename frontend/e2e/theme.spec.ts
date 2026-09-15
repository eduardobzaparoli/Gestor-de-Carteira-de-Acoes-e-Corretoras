import { expect, test } from "@playwright/test";

const admin = {
  id: "22222222-2222-2222-2222-222222222222",
  name: "Admin",
  email: "admin@example.com",
  role: "ADMIN",
};

test("mantém o tema escuro na área administrativa", async ({
  page,
}, testInfo) => {
  await page.addInitScript(() =>
    localStorage.setItem("bom-investidor.theme", "dark"),
  );
  await page.route("**/api/auth/login", async (route) =>
    route.fulfill({
      json: {
        token: "token-admin",
        tokenType: "Bearer",
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
        user: admin,
      },
    }),
  );
  await page.route("**/api/admin/users", async (route) =>
    route.fulfill({ json: [] }),
  );

  await page.goto("/login");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.getByLabel("E-mail").fill("admin@example.com");
  await page.getByLabel("Senha").fill("password123");
  await page.getByRole("button", { name: /entrar/i }).click();

  await expect(
    page.getByRole("heading", { name: "Gestão de usuários" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Ativar modo claro" }),
  ).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth,
  );
  expect(hasHorizontalOverflow).toBe(false);
  await page.screenshot({
    path: testInfo.outputPath("admin-dark.png"),
    fullPage: true,
  });
});
