import { readFileSync } from "node:fs";
import { describe, expect, test } from "vitest";

const sourceFiles = [
  "./styles.css",
  "./features/auth/AuthPages.tsx",
  "./features/portfolios/PortfoliosPage.tsx",
  "./features/portfolios/PortfolioPage.tsx",
  "./app/theme.ts",
  "./app/ThemeContext.tsx",
  "./components/ThemeToggle.tsx",
  "../index.html",
];

const themeSource = sourceFiles
  .map((file) => readFileSync(new URL(file, import.meta.url), "utf8"))
  .join("\n")
  .toLowerCase();

describe("identidade cromática", () => {
  test("define a paleta principal laranja por tokens semânticos", () => {
    expect(themeSource).toContain("--primary: #c2410c");
    expect(themeSource).toContain("--primary-dark: #9a3412");
    expect(themeSource).toContain("--primary-soft: #ffedd5");
  });

  test("define temas claro e escuro com superfícies semânticas", () => {
    expect(themeSource).toContain(':root[data-theme="dark"]');
    expect(themeSource).toContain("--page-bg: #fffaf5");
    expect(themeSource).toContain("--page-bg: #11100f");
    expect(themeSource).toContain("--surface-solid: #1d1b1a");
    expect(themeSource).toContain("dataset.theme = theme");
    expect(themeSource).toContain("bom-investidor.theme");
  });

  test("não mantém tokens ou cores da antiga identidade verde", () => {
    const legacyGreenPatterns = [
      "--emerald",
      "--mint",
      "#21a179",
      "#08785a",
      "#dff5ec",
      "#e3f3ec",
      "#78d9bb",
      "#47c99e",
      "#70dbb8",
      "#79ddbc",
      "#74b49b",
      "#93aaa4",
      "#8ff0ce",
      "rgba(33, 161, 121",
    ];

    legacyGreenPatterns.forEach((pattern) => {
      expect(themeSource).not.toContain(pattern);
    });
  });
});
