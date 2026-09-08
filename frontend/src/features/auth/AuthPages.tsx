import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, ShieldCheck, TrendingUp, WalletCards } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Field } from "../../components/ui";
import { ApiError, apiMessage, localizedFieldMessage } from "../../lib/http";
import { useAuth } from "../../app/AuthContext";

const loginSchema = z.object({
  email: z.string().email("Informe um e-mail válido."),
  password: z.string().min(1, "Informe sua senha.").max(72),
});
const registerSchema = loginSchema.extend({
  name: z.string().trim().min(1, "Informe seu nome.").max(100),
  password: z.string().min(8, "Use ao menos 8 caracteres.").max(72),
});
type LoginData = z.infer<typeof loginSchema>;
type RegisterData = z.infer<typeof registerSchema>;

function AuthFrame({ children }: { children: React.ReactNode }) {
  return (
    <main className="auth-layout">
      <section className="auth-hero">
        <div className="brand">
          <span className="brand-mark">
            <TrendingUp size={21} />
          </span>
          Bom Investidor
        </div>
        <div className="auth-copy">
          <span className="eyebrow" style={{ color: "#70dbb8" }}>
            Seu patrimônio, com clareza
          </span>
          <h1>Decisões melhores começam com uma visão completa.</h1>
          <p>
            Carteiras, posições, rentabilidade e proventos reunidos em uma
            experiência simples e inteligente.
          </p>
          <div className="hero-metrics">
            <div className="hero-metric">
              <strong>360º</strong>
              <span>da sua carteira</span>
            </div>
            <div className="hero-metric">
              <strong>BR + US</strong>
              <span>em uma visão</span>
            </div>
            <div className="hero-metric">
              <strong>BI</strong>
              <span>sem complicação</span>
            </div>
          </div>
        </div>
        <small>Dados organizados para você investir com contexto.</small>
      </section>
      <section className="auth-panel">{children}</section>
    </main>
  );
}

export function LoginPage() {
  const { user, login, reason } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginData>({ resolver: zodResolver(loginSchema) });
  if (user)
    return (
      <Navigate
        to={user.role === "ADMIN" ? "/admin/usuarios" : "/app/carteiras"}
        replace
      />
    );
  const submit = async (data: LoginData) => {
    try {
      const current = await login(data.email, data.password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(
        from ??
          (current.role === "ADMIN" ? "/admin/usuarios" : "/app/carteiras"),
        { replace: true },
      );
    } catch (error) {
      setError("root", { message: apiMessage(error) });
    }
  };
  return (
    <AuthFrame>
      <div className="auth-form">
        <span className="eyebrow">Bem-vindo de volta</span>
        <h2>Acesse sua conta</h2>
        <p>Entre para acompanhar seus investimentos.</p>
        {(reason || errors.root) && (
          <div className="error-state" role="alert">
            <ShieldCheck size={20} />
            <div>{reason || errors.root?.message}</div>
          </div>
        )}
        <form onSubmit={handleSubmit(submit)} noValidate>
          <Field
            label="E-mail"
            type="email"
            autoComplete="email"
            {...register("email")}
            error={errors.email?.message}
            placeholder="voce@exemplo.com"
          />
          <Field
            label="Senha"
            type="password"
            autoComplete="current-password"
            {...register("password")}
            error={errors.password?.message}
            placeholder="Sua senha"
          />
          <Button type="submit" loading={isSubmitting}>
            Entrar <ArrowRight size={18} />
          </Button>
        </form>
        <p className="auth-switch">
          Ainda não tem uma conta? <Link to="/cadastro">Cadastre-se</Link>
        </p>
      </div>
    </AuthFrame>
  );
}

export function RegisterPage() {
  const { user, register: createAccount } = useAuth();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterData>({ resolver: zodResolver(registerSchema) });
  if (user)
    return (
      <Navigate
        to={user.role === "ADMIN" ? "/admin/usuarios" : "/app/carteiras"}
        replace
      />
    );
  const submit = async (data: RegisterData) => {
    try {
      await createAccount(data.name, data.email, data.password);
      navigate("/login", { replace: true, state: { registered: true } });
    } catch (error) {
      if (error instanceof ApiError)
        error.body.fieldErrors.forEach((item) =>
          setError(item.field as keyof RegisterData, {
            message: localizedFieldMessage(item.field, item.message),
          }),
        );
      setError("root", { message: apiMessage(error) });
    }
  };
  return (
    <AuthFrame>
      <div className="auth-form">
        <span className="eyebrow">Comece agora</span>
        <h2>Crie sua conta</h2>
        <p>Organize seus investimentos em poucos passos.</p>
        {errors.root && (
          <div className="error-state" role="alert">
            <ShieldCheck size={20} />
            <div>{errors.root.message}</div>
          </div>
        )}
        <form onSubmit={handleSubmit(submit)} noValidate>
          <Field
            label="Nome"
            autoComplete="name"
            {...register("name")}
            error={errors.name?.message}
            placeholder="Como devemos chamar você?"
          />
          <Field
            label="E-mail"
            type="email"
            autoComplete="email"
            {...register("email")}
            error={errors.email?.message}
            placeholder="voce@exemplo.com"
          />
          <Field
            label="Senha"
            type="password"
            autoComplete="new-password"
            {...register("password")}
            error={errors.password?.message}
            hint="Entre 8 e 72 caracteres"
            placeholder="Crie uma senha segura"
          />
          <Button type="submit" loading={isSubmitting}>
            Criar conta <ArrowRight size={18} />
          </Button>
        </form>
        <p className="auth-switch">
          Já possui cadastro? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </AuthFrame>
  );
}

export function NotFound() {
  return (
    <main className="app-loading">
      <WalletCards size={48} />
      <h1>Página não encontrada</h1>
      <Link className="button button--primary" to="/">
        Voltar ao início
      </Link>
    </main>
  );
}
