import type { ApiErrorBody } from "../types/api";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");
let accessToken: string | null = null;

export class ApiError extends Error {
  constructor(public readonly body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
  }
  get code() {
    return this.body.code;
  }
  get status() {
    return this.body.status;
  }
  field(field: string) {
    const message = this.body.fieldErrors?.find(
      (item) => item.field === field,
    )?.message;
    return message ? localizedFieldMessage(field, message) : undefined;
  }
}

export const localizedFieldMessage = (field: string, message: string) => {
  const labels: Record<string, string> = {
    name: "nome",
    email: "e-mail",
    password: "senha",
    role: "papel",
    nickname: "apelido",
    cnpj: "CNPJ",
    cep: "CEP",
    street: "rua",
    neighborhood: "bairro",
    number: "número",
    brokerageId: "corretora",
    transactionDate: "data",
    quantity: "quantidade",
    unitPrice: "preço unitário",
    costs: "custos",
    paymentDate: "data de pagamento",
    receivedAmount: "valor recebido",
  };
  const label = labels[field] ?? "campo";
  const normalized = message.toLowerCase();
  if (
    normalized.includes("required") ||
    normalized.includes("must not be blank") ||
    normalized.includes("must not be null")
  )
    return `Informe ${label}.`;
  if (
    normalized.includes("must be valid") ||
    normalized.includes("well-formed")
  )
    return `Informe um ${label} válido.`;
  if (
    normalized.includes("at most") ||
    normalized.includes("size must be between")
  )
    return `Revise o tamanho do campo ${label}.`;
  if (
    normalized.includes("greater than") ||
    normalized.includes("must be greater")
  )
    return `Informe um valor válido para ${label}.`;
  return `Revise o valor informado para ${label}.`;
};

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message: "Não foi possível conectar à API.",
      fieldErrors: [],
    });
  }
  if (response.status === 204) return undefined as T;
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new ApiError(
      body ?? {
        status: response.status,
        code: "UNEXPECTED_ERROR",
        message: "Ocorreu um erro inesperado.",
        fieldErrors: [],
      },
    );
    if (response.status === 401)
      window.dispatchEvent(
        new CustomEvent("auth:unauthorized", { detail: error.code }),
      );
    throw error;
  }
  return body as T;
}

export const apiMessage = (error: unknown) => {
  if (!(error instanceof ApiError))
    return "Não foi possível concluir a operação.";
  if (error.code === "VALIDATION_ERROR" && error.body.fieldErrors?.length) {
    return [
      ...new Set(
        error.body.fieldErrors.map(({ field, message }) =>
          localizedFieldMessage(field, message),
        ),
      ),
    ].join(" ");
  }
  const messages: Record<string, string> = {
    NETWORK_ERROR:
      "A API não está acessível. Verifique se o backend está em execução.",
    INVALID_CREDENTIALS: "E-mail ou senha incorretos.",
    ACCOUNT_INACTIVE: "Esta conta está inativa.",
    EMAIL_ALREADY_REGISTERED: "Este e-mail já está cadastrado.",
    INSUFFICIENT_POSITION: "A quantidade de venda supera o saldo disponível.",
    INSUFFICIENT_ASSET_QUANTITY:
      "A quantidade de venda supera o saldo disponível.",
    LAST_ACTIVE_ADMIN:
      "O último administrador ativo não pode perder esse papel.",
    CANNOT_DEACTIVATE_SELF: "Você não pode desativar sua própria conta.",
    ALPHAVANTAGE_RATE_LIMITED:
      "O limite do provedor de mercado foi atingido. Tente novamente mais tarde.",
    TWELVE_DATA_RATE_LIMITED:
      "O limite do provedor de mercado foi atingido. Tente novamente mais tarde.",
    TWELVE_DATA_PROVIDER_UNAVAILABLE:
      "Os dados do mercado americano estão indisponíveis no momento. Tente novamente.",
    EXCHANGE_RATE_UNAVAILABLE:
      "O câmbio necessário está indisponível no momento.",
    CNPJ_NOT_FOUND: "O CNPJ informado não foi encontrado.",
    CNPJ_NOT_ACTIVE_AT_CVM:
      "O CNPJ informado não pertence a uma instituição ativa na CVM.",
    CNPJ_PROVIDER_UNAVAILABLE:
      "A consulta de CNPJ está indisponível no momento. Tente novamente.",
    CVM_PROVIDER_UNAVAILABLE:
      "A consulta da CVM está indisponível no momento. Tente novamente.",
    ADDRESS_PROVIDER_UNAVAILABLE:
      "A consulta de endereço está indisponível no momento. Tente novamente.",
    CEP_NOT_FOUND: "O CEP informado não foi encontrado.",
    CEP_CNPJ_MISMATCH:
      "O CEP informado não corresponde ao endereço cadastrado para este CNPJ.",
    BROKERAGE_CNPJ_ALREADY_REGISTERED: "Este CNPJ já está cadastrado.",
    BROKERAGE_NICKNAME_ALREADY_REGISTERED:
      "Já existe uma corretora com esse apelido.",
    BROKERAGE_HAS_PORTFOLIOS:
      "Esta corretora está vinculada a uma carteira. Exclua a carteira primeiro.",
    TRANSACTION_CANNOT_BE_EDITED:
      "Somente lançamentos pendentes podem ser editados.",
    TRANSACTION_CANNOT_BE_CANCELLED:
      "Somente lançamentos pendentes podem ser cancelados.",
  };
  return messages[error.code] ?? "Não foi possível concluir a operação.";
};
