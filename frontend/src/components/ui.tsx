import {
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  useEffect,
  useId,
  useRef,
  useState,
} from "react";
import {
  AlertCircle,
  CalendarDays,
  CheckCircle2,
  LoaderCircle,
  X,
} from "lucide-react";
import {
  completePtBrDate,
  isoToPtBr,
  maskDate,
  ptBrToIso,
} from "../lib/format";

export function Button({
  className = "",
  variant = "primary",
  loading,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  loading?: boolean;
}) {
  return (
    <button
      className={`button button--${variant} ${className}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <LoaderCircle className="spin" size={17} />}
      {children}
    </button>
  );
}

export function Field({
  label,
  error,
  hint,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  hint?: string;
}) {
  const id = props.id ?? props.name;
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <input
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        {...props}
      />
      {hint && <small>{hint}</small>}
      {error && (
        <small className="field-error" id={`${id}-error`}>
          {error}
        </small>
      )}
    </label>
  );
}

export function SelectField({
  label,
  error,
  children,
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  error?: string;
}) {
  const id = props.id ?? props.name;
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <select id={id} aria-invalid={Boolean(error)} {...props}>
        {children}
      </select>
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}

export function DateField({
  label,
  value,
  onValueChange,
  error,
  required,
  name,
}: {
  label: string;
  value: string;
  onValueChange: (value: string) => void;
  error?: string;
  required?: boolean;
  name?: string;
}) {
  const generatedId = useId();
  const id = name ?? generatedId;
  const calendarRef = useRef<HTMLInputElement>(null);
  const openCalendar = () => {
    const picker = calendarRef.current;
    if (!picker) return;
    if (typeof picker.showPicker === "function") picker.showPicker();
    else picker.click();
  };
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <div className="date-field">
        <input
          id={id}
          name={name}
          inputMode="numeric"
          maxLength={10}
          placeholder="dd/mm/aaaa"
          value={value}
          required={required}
          aria-invalid={Boolean(error)}
          onChange={(event) => onValueChange(maskDate(event.target.value))}
          onBlur={(event) =>
            onValueChange(completePtBrDate(event.currentTarget.value))
          }
        />
        <button
          type="button"
          onClick={openCalendar}
          aria-label={`Abrir calendário de ${label.toLowerCase()}`}
        >
          <CalendarDays size={17} />
        </button>
        <input
          ref={calendarRef}
          className="date-field__picker"
          type="date"
          value={ptBrToIso(value) ?? ""}
          tabIndex={-1}
          aria-hidden="true"
          onChange={(event) => onValueChange(isoToPtBr(event.target.value))}
        />
      </div>
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}

export const Card = ({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) => <section className={`card ${className}`}>{children}</section>;
export const Badge = ({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: "success" | "danger" | "warning" | "info" | "neutral";
}) => <span className={`badge badge--${tone}`}>{children}</span>;
export const Skeleton = ({ height = 120 }: { height?: number }) => (
  <div className="skeleton" style={{ height }} aria-label="Carregando" />
);

export function EmptyState({
  icon,
  title,
  children,
  action,
}: {
  icon?: ReactNode;
  title: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      {icon}
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}

export function ErrorState({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div className="error-state" role="alert">
      <AlertCircle size={22} />
      <div>
        <strong>Não foi possível carregar</strong>
        <p>{message}</p>
        {retry && (
          <Button variant="secondary" onClick={retry}>
            Tentar novamente
          </Button>
        )}
      </div>
    </div>
  );
}

export function Dialog({
  open,
  title,
  children,
  onClose,
}: {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      className="dialog"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClose={onClose}
      aria-labelledby={titleId}
    >
      <div className="dialog__header">
        <div>
          <span className="eyebrow">Bom Investidor</span>
          <h2 id={titleId}>{title}</h2>
        </div>
        <button className="icon-button" onClick={onClose} aria-label="Fechar">
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}

type Toast = { id: number; message: string; tone: "success" | "error" };
let toastId = 0;
const toastListeners = new Set<(toast: Toast) => void>();
export const notify = (message: string, tone: Toast["tone"] = "success") =>
  toastListeners.forEach((listener) =>
    listener({ id: ++toastId, message, tone }),
  );

export function ToastRegion() {
  const [toasts, setToasts] = useState<Toast[]>([]);
  useEffect(() => {
    const listener = (toast: Toast) => {
      setToasts((current) => [...current, toast]);
      window.setTimeout(
        () =>
          setToasts((current) =>
            current.filter((item) => item.id !== toast.id),
          ),
        4500,
      );
    };
    toastListeners.add(listener);
    return () => {
      toastListeners.delete(listener);
    };
  }, []);
  const remove = (id: number) =>
    setToasts((current) => current.filter((item) => item.id !== id));
  return (
    <div className="toast-region" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast--${toast.tone}`}>
          {toast.tone === "success" ? <CheckCircle2 /> : <AlertCircle />}
          <span>{toast.message}</span>
          <button onClick={() => remove(toast.id)} aria-label="Fechar">
            <X size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}

export function PageHeading({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="page-heading">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}
