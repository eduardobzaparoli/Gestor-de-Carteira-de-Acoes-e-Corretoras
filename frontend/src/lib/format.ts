import type { DecimalValue } from "../types/api";

export const asNumber = (value: DecimalValue | null | undefined) =>
  value == null ? 0 : Number(value);
export const money = (
  value: DecimalValue | null | undefined,
  currency = "BRL",
) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(asNumber(value));
export const number = (value: DecimalValue | null | undefined, digits = 2) =>
  new Intl.NumberFormat("pt-BR", { maximumFractionDigits: digits }).format(
    asNumber(value),
  );
export const percent = (value: DecimalValue | null | undefined) =>
  `${number(value, 2)}%`;
export const date = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" }).format(
        new Date(`${value.slice(0, 10)}T12:00:00Z`),
      )
    : "—";
export const dateTime = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short",
      }).format(new Date(value))
    : "—";
export const digits = (value: string) => value.replace(/\D/g, "");
export const maskCep = (value: string) =>
  digits(value)
    .slice(0, 8)
    .replace(/(\d{5})(\d)/, "$1-$2");
export const maskCnpj = (value: string) =>
  digits(value)
    .slice(0, 14)
    .replace(/(\d{2})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");

export const maskDate = (value: string) =>
  digits(value)
    .slice(0, 8)
    .replace(/(\d{2})(\d)/, "$1/$2")
    .replace(/(\d{2})(\d)/, "$1/$2");

export const isoToPtBr = (value?: string) => {
  if (!value) return "";
  const [year, month, day] = value.slice(0, 10).split("-");
  return year && month && day ? `${day}/${month}/${year}` : "";
};

export const ptBrToIso = (value: string) => {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (!match) return null;
  const [, day, month, year] = match;
  const iso = `${year}-${month}-${day}`;
  const parsed = new Date(`${iso}T12:00:00Z`);
  return Number.isNaN(parsed.getTime()) ||
    parsed.getUTCFullYear() !== Number(year) ||
    parsed.getUTCMonth() + 1 !== Number(month) ||
    parsed.getUTCDate() !== Number(day)
    ? null
    : iso;
};

export const todayPtBr = () => {
  const today = new Date();
  return `${String(today.getDate()).padStart(2, "0")}/${String(today.getMonth() + 1).padStart(2, "0")}/${today.getFullYear()}`;
};

export const completePtBrDate = (
  value: string,
  year = new Date().getFullYear(),
) => {
  const masked = maskDate(value);
  if (/^\d{2}\/\d{2}$/.test(masked)) return `${masked}/${year}`;
  return masked;
};

export const decimalInput = (value: string) => {
  const cleaned = value.trim().replace(/[^\d,.-]/g, "");
  if (cleaned.includes(","))
    return cleaned.replace(/\./g, "").replace(",", ".");
  return cleaned;
};

export const formattedMoneyInput = (value: string, currency = "BRL") => {
  const decimal = decimalInput(value);
  if (!decimal || Number.isNaN(Number(decimal))) return value;
  return money(decimal, currency);
};

export const convertedAmount = (
  value: DecimalValue | null | undefined,
  rate: DecimalValue | null | undefined,
) => asNumber(value) * asNumber(rate ?? 1);

export const nativeAmount = (
  valueInBrl: DecimalValue | null | undefined,
  rate: DecimalValue | null | undefined,
) => {
  const divisor = asNumber(rate ?? 1);
  return divisor > 0 ? asNumber(valueInBrl) / divisor : asNumber(valueInBrl);
};

export const decimalForApi = (
  value: DecimalValue | null | undefined,
  maximumFractionDigits = 8,
) => {
  const numeric = asNumber(value);
  if (!Number.isFinite(numeric)) return "";
  return numeric
    .toFixed(maximumFractionDigits)
    .replace(/(\.\d*?[1-9])0+$/, "$1")
    .replace(/\.0+$/, "");
};
export const labels: Record<string, string> = {
  BUY: "Compra",
  SELL: "Venda",
  PENDING: "Pendente",
  EFFECTIVE: "Efetivado",
  CANCELLED: "Cancelado",
  ACTIVE: "Ativo",
  INACTIVE: "Inativo",
  INVESTOR: "Investidor",
  ADMIN: "Administrador",
  BR: "Brasil",
  US: "Estados Unidos",
  STOCK: "Ação",
  ETF: "ETF",
  DIVIDEND: "Dividendo",
  INTEREST_ON_EQUITY: "Juros sobre capital",
  DISTRIBUTION: "Distribuição",
  PROVIDER: "Integração",
  MANUAL: "Manual",
};
export const label = (value: string) => labels[value] ?? value;
