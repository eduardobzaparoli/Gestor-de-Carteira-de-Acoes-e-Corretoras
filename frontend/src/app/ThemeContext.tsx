import {
  createContext,
  type ReactNode,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
} from "react";
import {
  applyTheme,
  resolveInitialTheme,
  storeTheme,
  type Theme,
} from "./theme";

type ThemeContextValue = {
  theme: Theme;
  toggleTheme: () => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function browserStorage() {
  try {
    return window.localStorage;
  } catch {
    return undefined;
  }
}

function browserInitialTheme(): Theme {
  if (typeof document === "undefined" || typeof window === "undefined") {
    return "light";
  }
  return resolveInitialTheme({
    rootTheme: document.documentElement.dataset.theme,
    storage: browserStorage(),
    matchMedia: window.matchMedia?.bind(window),
  });
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(browserInitialTheme);

  useLayoutEffect(() => {
    applyTheme(theme, document.documentElement);
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", theme === "dark" ? "#11100f" : "#fffaf5");
  }, [theme]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      toggleTheme: () =>
        setTheme((current) => {
          const next = current === "light" ? "dark" : "light";
          storeTheme(next, browserStorage());
          return next;
        }),
    }),
    [theme],
  );

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context)
    throw new Error("useTheme deve ser usado dentro de ThemeProvider");
  return context;
}
