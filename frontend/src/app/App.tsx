import {
  Component,
  Suspense,
  lazy,
  type ErrorInfo,
  type ReactNode,
} from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { LoaderCircle } from "lucide-react";
import { useAuth } from "./AuthContext";
import { Shell } from "./Shell";

const LoginPage = lazy(() =>
  import("../features/auth/AuthPages").then((module) => ({
    default: module.LoginPage,
  })),
);
const RegisterPage = lazy(() =>
  import("../features/auth/AuthPages").then((module) => ({
    default: module.RegisterPage,
  })),
);
const NotFound = lazy(() =>
  import("../features/auth/AuthPages").then((module) => ({
    default: module.NotFound,
  })),
);
const BrokeragesPage = lazy(() =>
  import("../features/brokerages/BrokeragesPage").then((module) => ({
    default: module.BrokeragesPage,
  })),
);
const PortfoliosPage = lazy(() =>
  import("../features/portfolios/PortfoliosPage").then((module) => ({
    default: module.PortfoliosPage,
  })),
);
const PortfolioPage = lazy(() =>
  import("../features/portfolios/PortfolioPage").then((module) => ({
    default: module.PortfolioPage,
  })),
);
const AdminUsersPage = lazy(() =>
  import("../features/admin/AdminUsersPage").then((module) => ({
    default: module.AdminUsersPage,
  })),
);

const routeFallback = (
  <div className="app-loading">
    <LoaderCircle className="spin" />
    <span>Carregando interface…</span>
  </div>
);

class ErrorBoundary extends Component<
  { children: ReactNode },
  { failed: boolean }
> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error(error, info);
  }
  render() {
    return this.state.failed ? (
      <main className="app-loading">
        <h1>Algo saiu do esperado</h1>
        <p>Recarregue a página para continuar.</p>
        <button
          className="button button--primary"
          onClick={() => location.reload()}
        >
          Recarregar
        </button>
      </main>
    ) : (
      this.props.children
    );
  }
}

function Protected({
  role,
  children,
}: {
  role: "INVESTOR" | "ADMIN";
  children: ReactNode;
}) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading)
    return (
      <div className="app-loading">
        <LoaderCircle className="spin" />
        <span>Preparando sua experiência…</span>
      </div>
    );
  if (!user)
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (user.role !== role)
    return (
      <Navigate
        to={user.role === "ADMIN" ? "/admin/usuarios" : "/app/carteiras"}
        replace
      />
    );
  return children;
}

function Home() {
  const { user, loading } = useAuth();
  if (loading)
    return (
      <div className="app-loading">
        <LoaderCircle className="spin" />
      </div>
    );
  return (
    <Navigate
      to={
        !user
          ? "/login"
          : user.role === "ADMIN"
            ? "/admin/usuarios"
            : "/app/carteiras"
      }
      replace
    />
  );
}

export function App() {
  return (
    <ErrorBoundary>
      <Suspense fallback={routeFallback}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/cadastro" element={<RegisterPage />} />
          <Route
            element={
              <Protected role="INVESTOR">
                <Shell />
              </Protected>
            }
          >
            <Route path="/app/carteiras" element={<PortfoliosPage />} />
            <Route path="/app/corretoras" element={<BrokeragesPage />} />
            <Route
              path="/app/carteiras/:portfolioId"
              element={<PortfolioPage />}
            />
          </Route>
          <Route
            element={
              <Protected role="ADMIN">
                <Shell />
              </Protected>
            }
          >
            <Route path="/admin/usuarios" element={<AdminUsersPage />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}
