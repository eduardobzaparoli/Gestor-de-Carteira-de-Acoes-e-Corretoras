import {
  BarChart3,
  Building2,
  LogOut,
  Menu,
  TrendingUp,
  Users,
  WalletCards,
  X,
} from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Button } from "../components/ui";
import { ThemeToggle } from "../components/ThemeToggle";
import { useAuth } from "./AuthContext";

export function Shell() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const nav =
    user?.role === "ADMIN"
      ? [{ to: "/admin/usuarios", label: "Usuários", icon: Users }]
      : [
          { to: "/app/carteiras", label: "Carteiras", icon: WalletCards },
          { to: "/app/corretoras", label: "Corretoras", icon: Building2 },
        ];
  const links = (
    <nav className="nav" aria-label="Navegação principal">
      {nav.map(({ to, label, icon: Icon }) => (
        <NavLink key={to} to={to} onClick={() => setOpen(false)}>
          <Icon size={19} />
          {label}
        </NavLink>
      ))}
    </nav>
  );
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="brand">
            <span className="brand-mark">
              <TrendingUp size={21} />
            </span>
            Bom Investidor
          </div>
          <ThemeToggle className="theme-toggle--compact" />
        </div>
        <span className="nav-label">Visão geral</span>
        {links}
        <div className="sidebar-user">
          <strong>{user?.name}</strong>
          <span>{user?.email}</span>
          <Button variant="ghost" onClick={() => logout()}>
            <LogOut size={17} />
            Sair
          </Button>
        </div>
      </aside>
      <header className="mobile-header">
        <div className="brand">
          <span className="brand-mark">
            <BarChart3 size={19} />
          </span>
          Bom Investidor
        </div>
        <div className="mobile-header__actions">
          <ThemeToggle className="theme-toggle--compact" />
          <button
            className="icon-button"
            onClick={() => setOpen(!open)}
            aria-label={open ? "Fechar menu" : "Abrir menu"}
          >
            {open ? <X /> : <Menu />}
          </button>
        </div>
        {open && (
          <div className="mobile-menu">
            {links}
            <Button variant="ghost" onClick={() => logout()}>
              <LogOut size={17} />
              Sair
            </Button>
          </div>
        )}
      </header>
      <main className="main">
        <div className="content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
