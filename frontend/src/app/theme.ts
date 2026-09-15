export type Theme = "light" | "dark";

export const THEME_STORAGE_KEY = "bom-investidor.theme";

export function isTheme(value: unknown): value is Theme {
  return value === "light" || value === "dark";
}

export function readStoredTheme(
  storage?: Pick<Storage, "getItem">,
): Theme | null {
  try {
    const value = storage?.getItem(THEME_STORAGE_KEY);
    return isTheme(value) ? value : null;
  } catch {
    return null;
  }
}

export function readSystemTheme(
  matchMedia?: (query: string) => Pick<MediaQueryList, "matches">,
): Theme {
  try {
    return matchMedia?.("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  } catch {
    return "light";
  }
}

export function resolveInitialTheme({
  rootTheme,
  storage,
  matchMedia,
}: {
  rootTheme?: unknown;
  storage?: Pick<Storage, "getItem">;
  matchMedia?: (query: string) => Pick<MediaQueryList, "matches">;
} = {}): Theme {
  if (isTheme(rootTheme)) return rootTheme;
  return readStoredTheme(storage) ?? readSystemTheme(matchMedia);
}

export function applyTheme(
  theme: Theme,
  root?: Pick<HTMLElement, "dataset" | "style">,
) {
  if (!root) return;
  root.dataset.theme = theme;
  root.style.colorScheme = theme;
}

export function storeTheme(theme: Theme, storage?: Pick<Storage, "setItem">) {
  try {
    storage?.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // O tema continua funcional mesmo quando o armazenamento está indisponível.
  }
}
