import { Moon, Sun } from "lucide-react";
import { useTheme } from "../app/ThemeContext";

export function ThemeToggle({ className = "" }: { className?: string }) {
  const { theme, toggleTheme } = useTheme();
  const dark = theme === "dark";
  const label = dark ? "Ativar modo claro" : "Ativar modo escuro";

  return (
    <button
      type="button"
      className={`theme-toggle ${className}`}
      onClick={toggleTheme}
      aria-label={label}
      title={label}
    >
      {dark ? <Sun size={18} /> : <Moon size={18} />}
      <span className="theme-toggle__label">{dark ? "Claro" : "Escuro"}</span>
    </button>
  );
}
