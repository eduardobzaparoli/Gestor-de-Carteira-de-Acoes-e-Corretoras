import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ThemeProvider } from "../app/ThemeContext";
import { THEME_STORAGE_KEY } from "../app/theme";
import { ThemeToggle } from "./ThemeToggle";

test("alterna, anuncia e persiste o tema", async () => {
  document.documentElement.dataset.theme = "light";
  render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  );

  const toggle = screen.getByRole("button", { name: "Ativar modo escuro" });
  await userEvent.click(toggle);

  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(document.documentElement.style.colorScheme).toBe("dark");
  expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");
  expect(
    screen.getByRole("button", { name: "Ativar modo claro" }),
  ).toBeInTheDocument();
});

test("restaura a preferência salva", () => {
  localStorage.setItem(THEME_STORAGE_KEY, "dark");
  render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  );

  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(
    screen.getByRole("button", { name: "Ativar modo claro" }),
  ).toBeInTheDocument();
});
