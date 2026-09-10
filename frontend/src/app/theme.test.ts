import { describe, expect, test, vi } from "vitest";
import {
  applyTheme,
  readStoredTheme,
  readSystemTheme,
  resolveInitialTheme,
  storeTheme,
  THEME_STORAGE_KEY,
} from "./theme";

describe("resolução do tema", () => {
  test("prioriza o tema já aplicado antes da montagem", () => {
    expect(
      resolveInitialTheme({
        rootTheme: "dark",
        storage: { getItem: () => "light" },
        matchMedia: () => ({ matches: false }),
      }),
    ).toBe("dark");
  });

  test("restaura uma preferência válida do navegador", () => {
    expect(
      resolveInitialTheme({
        storage: { getItem: () => "dark" },
        matchMedia: () => ({ matches: false }),
      }),
    ).toBe("dark");
  });

  test("usa a preferência do sistema e mantém claro como fallback", () => {
    expect(readSystemTheme(() => ({ matches: true }))).toBe("dark");
    expect(readSystemTheme(() => ({ matches: false }))).toBe("light");
    expect(
      readSystemTheme(() => {
        throw new Error("indisponível");
      }),
    ).toBe("light");
  });

  test("tolera armazenamento indisponível ou com valor inválido", () => {
    expect(readStoredTheme({ getItem: () => "azul" })).toBeNull();
    expect(
      readStoredTheme({
        getItem: () => {
          throw new Error("bloqueado");
        },
      }),
    ).toBeNull();

    const setItem = vi.fn(() => {
      throw new Error("bloqueado");
    });
    expect(() => storeTheme("dark", { setItem })).not.toThrow();
    expect(setItem).toHaveBeenCalledWith(THEME_STORAGE_KEY, "dark");
  });

  test("aplica o tema e o esquema de cores no elemento raiz", () => {
    const root = document.documentElement;
    applyTheme("dark", root);
    expect(root.dataset.theme).toBe("dark");
    expect(root.style.colorScheme).toBe("dark");
  });
});
