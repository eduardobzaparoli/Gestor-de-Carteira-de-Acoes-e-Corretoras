import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseQueryResult,
} from "@tanstack/react-query";
import {
  Activity,
  ArrowLeft,
  Banknote,
  CalendarClock,
  CircleDollarSign,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  TrendingDown,
  TrendingUp,
  WalletCards,
} from "lucide-react";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import {
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Badge,
  Button,
  Card,
  Dialog,
  EmptyState,
  ErrorState,
  DateField,
  Field,
  PageHeading,
  SelectField,
  Skeleton,
  notify,
} from "../../components/ui";
import { AssetLogo, AssetLogoAttribution } from "../../components/AssetLogo";
import { api, apiMessage } from "../../lib/http";
import {
  asNumber,
  convertedAmount,
  decimalForApi,
  decimalInput,
  date,
  formattedMoneyInput,
  isoToPtBr,
  label,
  money,
  nativeAmount,
  number,
  percent,
  ptBrToIso,
  todayPtBr,
} from "../../lib/format";
import type {
  AssetMarket,
  AssetSearchResult,
  AssetType,
  ExchangeRate,
  EvolutionPoint,
  IncomeCandidate,
  IncomeEvent,
  IncomeEventType,
  IncomeSummary,
  Portfolio,
  Position,
  Transaction,
  TransactionUpdateInput,
  TransactionStatus,
  TransactionType,
  Valuation,
  ValuationPosition,
} from "../../types/api";

const colors = [
  "#c2410c",
  "#316b83",
  "#d99b3e",
  "#7256a8",
  "#d76060",
  "#9a5b3c",
  "#557a95",
];
const queryKeys = (id: string) => [
  ["portfolio", id],
  ["positions", id],
  ["valuation", id],
  ["evolution", id],
  ["transactions", id],
  ["income-events", id],
  ["income-summary", id],
];

export function PortfolioPage() {
  const { portfolioId = "" } = useParams();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<"overview" | "transactions" | "income">(
    "overview",
  );
  const [transactionOpen, setTransactionOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] =
    useState<Transaction | null>(null);
  const openNewTransaction = () => {
    setEditingTransaction(null);
    setTransactionOpen(true);
  };
  const openEditTransaction = (transaction: Transaction) => {
    setEditingTransaction(transaction);
    setTransactionOpen(true);
  };
  const detail = useQuery({
    queryKey: ["portfolio", portfolioId],
    queryFn: () => api<Portfolio>(`/api/portfolios/${portfolioId}`),
  });
  const positions = useQuery({
    queryKey: ["positions", portfolioId],
    queryFn: () => api<Position[]>(`/api/portfolios/${portfolioId}/positions`),
  });
  const valuation = useQuery({
    queryKey: ["valuation", portfolioId],
    queryFn: () => api<Valuation>(`/api/portfolios/${portfolioId}/valuation`),
    retry: false,
  });
  const evolution = useQuery({
    queryKey: ["evolution", portfolioId],
    queryFn: () =>
      api<EvolutionPoint[]>(`/api/portfolios/${portfolioId}/value-evolution`),
    retry: false,
  });
  const transactions = useQuery({
    queryKey: ["transactions", portfolioId],
    queryFn: () =>
      api<Transaction[]>(`/api/portfolios/${portfolioId}/transactions`),
  });
  const income = useQuery({
    queryKey: ["income-summary", portfolioId],
    queryFn: () =>
      api<IncomeSummary>(
        `/api/portfolios/${portfolioId}/income-events/summary`,
      ),
    retry: false,
  });
  const showsAmericanAsset = [
    ...(positions.data ?? []),
    ...(transactions.data ?? []),
  ].some((item) => item.market === "US");
  const refresh = () => {
    queryKeys(portfolioId).forEach((key) =>
      queryClient.invalidateQueries({ queryKey: key }),
    );
    notify("Atualizando os indicadores da carteira.");
  };
  if (detail.isLoading) return <Skeleton height={620} />;
  if (detail.isError || !detail.data)
    return (
      <ErrorState
        message={apiMessage(detail.error)}
        retry={() => detail.refetch()}
      />
    );
  return (
    <>
      <Link
        to="/app/carteiras"
        className="muted"
        style={{
          display: "inline-flex",
          gap: ".4rem",
          alignItems: "center",
          textDecoration: "none",
          marginBottom: "1rem",
        }}
      >
        <ArrowLeft size={16} />
        Voltar às carteiras
      </Link>
      <PageHeading
        eyebrow={detail.data.brokerage.nickname}
        title={detail.data.name}
        description="Visão consolidada da sua estratégia, atualizada com dados de mercado."
        actions={
          <>
            <Button variant="secondary" onClick={refresh}>
              <RefreshCw size={17} />
              Atualizar
            </Button>
            <Button onClick={openNewTransaction}>
              <Plus size={17} />
              Novo lançamento
            </Button>
          </>
        }
      />
      <div className="tabs" role="tablist">
        <button
          className={tab === "overview" ? "active" : ""}
          onClick={() => setTab("overview")}
        >
          Visão geral
        </button>
        <button
          className={tab === "transactions" ? "active" : ""}
          onClick={() => setTab("transactions")}
        >
          Lançamentos <Badge>{transactions.data?.length ?? 0}</Badge>
        </button>
        <button
          className={tab === "income" ? "active" : ""}
          onClick={() => setTab("income")}
        >
          Proventos
        </button>
      </div>
      {tab === "overview" && (
        <Dashboard
          valuation={valuation}
          evolution={evolution}
          positions={positions}
          income={income}
          onNew={openNewTransaction}
        />
      )}
      {tab === "transactions" && (
        <TransactionsPanel
          portfolioId={portfolioId}
          query={transactions}
          onNew={openNewTransaction}
          onEdit={openEditTransaction}
        />
      )}
      {tab === "income" && <IncomePanel portfolioId={portfolioId} />}
      <TransactionDialog
        open={transactionOpen}
        onClose={() => setTransactionOpen(false)}
        portfolioId={portfolioId}
        editing={editingTransaction}
      />
      {showsAmericanAsset && <AssetLogoAttribution />}
    </>
  );
}

function Dashboard({
  valuation,
  evolution,
  positions,
  income,
  onNew,
}: {
  valuation: UseQueryResult<Valuation>;
  evolution: UseQueryResult<EvolutionPoint[]>;
  positions: UseQueryResult<Position[]>;
  income: UseQueryResult<IncomeSummary>;
  onNew: () => void;
}) {
  const summary = valuation.data?.consolidatedSummary;
  const gain = asNumber(summary?.totalGain);
  const pieData =
    valuation.data?.positions.map((item) => ({
      name: item.ticker,
      value: asNumber(item.allocationPercentage),
    })) ?? [];
  const currentRates = summary?.exchangeRates ?? [];
  const amountInBrl = (
    value: string | number,
    currency: string,
    rates: ExchangeRate[] = currentRates,
  ) => {
    if (currency === "BRL") return money(value, "BRL");
    const rate = rates.find((item) => item.sourceCurrency === currency)?.rate;
    return rate ? money(convertedAmount(value, rate), "BRL") : "—";
  };
  return (
    <>
      <div className="grid grid-4">
        <Kpi
          label="Valor investido"
          value={
            summary ? money(summary.investedValue, summary.baseCurrency) : "—"
          }
          icon={<WalletCards />}
          detail="Custo das posições em custódia"
        />
        <Kpi
          label="Patrimônio"
          value={
            summary ? money(summary.marketValue, summary.baseCurrency) : "—"
          }
          icon={<Activity />}
          detail="Cotações atuais consolidadas"
        />
        <Kpi
          label="Ganho total"
          value={summary ? money(summary.totalGain, summary.baseCurrency) : "—"}
          icon={gain >= 0 ? <TrendingUp /> : <TrendingDown />}
          tone={gain >= 0 ? "positive" : "negative"}
          detail="Patrimônio menos valor investido"
        />
        <Kpi
          label="Rentabilidade"
          value={summary ? percent(summary.returnPercentage) : "—"}
          icon={<CircleDollarSign />}
          tone={gain >= 0 ? "positive" : "negative"}
          detail="Retorno não realizado"
        />
      </div>
      {!positions.isLoading && !positions.data?.length ? (
        <Card className="dashboard-empty">
          <EmptyState
            icon={<TrendingUp size={42} />}
            title="Carteira pronta para receber ativos"
            action={
              <Button onClick={onNew}>
                <Plus size={17} />
                Cadastrar primeiro lançamento
              </Button>
            }
          >
            Registre uma compra para ativar posições, indicadores e gráficos.
          </EmptyState>
        </Card>
      ) : (
        <div className="dashboard-grid">
          <ChartCard
            title="Evolução patrimonial"
            subtitle="Valor investido × patrimônio"
            query={evolution}
          >
            {evolution.data && (
              <>
                <div className="chart">
                  <ResponsiveContainer>
                    <LineChart data={evolution.data}>
                      <XAxis
                        dataKey="date"
                        tickFormatter={(v) => date(v).slice(0, 5)}
                        tick={{ fontSize: 11 }}
                      />
                      <YAxis
                        tickFormatter={(v) => number(v, 0)}
                        tick={{ fontSize: 11 }}
                        width={54}
                      />
                      <Tooltip
                        labelFormatter={(v) => date(String(v))}
                        formatter={(v, name) => [
                          money(Number(v)),
                          name === "marketValue" ? "Patrimônio" : "Investido",
                        ]}
                      />
                      <Line
                        type="monotone"
                        dataKey="investedValue"
                        stroke="#9a8c83"
                        strokeWidth={2}
                        dot={false}
                        isAnimationActive={false}
                      />
                      <Line
                        type="monotone"
                        dataKey="marketValue"
                        stroke="#c2410c"
                        strokeWidth={3}
                        dot={false}
                        isAnimationActive={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
                <div className="chart-legend">
                  <span>
                    <i
                      className="legend-dot"
                      style={{ background: "#c2410c" }}
                    />
                    Patrimônio
                  </span>
                  <span>
                    <i
                      className="legend-dot"
                      style={{ background: "#9a8c83" }}
                    />
                    Valor investido
                  </span>
                </div>
                <details>
                  <summary className="muted">Ver dados do gráfico</summary>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Data</th>
                          <th>Investido</th>
                          <th>Patrimônio</th>
                        </tr>
                      </thead>
                      <tbody>
                        {evolution.data.map((point) => (
                          <tr key={point.date}>
                            <td>{date(point.date)}</td>
                            <td>{money(point.investedValue)}</td>
                            <td>{money(point.marketValue)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </details>
              </>
            )}
          </ChartCard>
          <ChartCard
            title="Composição"
            subtitle="Participação por ativo"
            query={valuation}
          >
            {valuation.data && (
              <>
                {pieData.length ? (
                  <div className="chart">
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie
                          data={pieData}
                          dataKey="value"
                          nameKey="name"
                          innerRadius="58%"
                          outerRadius="82%"
                          paddingAngle={3}
                          isAnimationActive={false}
                        >
                          {pieData.map((_, index) => (
                            <Cell
                              key={index}
                              fill={colors[index % colors.length]}
                            />
                          ))}
                        </Pie>
                        <Tooltip formatter={(v) => percent(Number(v))} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <EmptyState title="Sem composição">
                    A alocação aparecerá quando houver cotação disponível.
                  </EmptyState>
                )}
                <div className="chart-legend">
                  {pieData.map((item, index) => (
                    <span key={item.name}>
                      <i
                        className="legend-dot"
                        style={{ background: colors[index % colors.length] }}
                      />
                      {item.name} · {percent(item.value)}
                    </span>
                  ))}
                </div>
              </>
            )}
          </ChartCard>
        </div>
      )}
      <div className="dashboard-grid">
        <PositionsCard
          query={valuation}
          fallback={positions.data ?? []}
          exchangeRates={currentRates}
        />
        <Card>
          <div className="card-header">
            <div>
              <h3>Moedas e proventos</h3>
              <p>Consolidação informada pela API</p>
            </div>
            <Banknote color="#c2410c" />
          </div>
          {valuation.data?.currencySummaries.map((item) => (
            <div className="currency-row" key={item.currency}>
              <div>
                <strong>{item.currency}</strong>
                <span>
                  {amountInBrl(item.investedValue, item.currency)} investidos
                </span>
              </div>
              <div style={{ textAlign: "right" }}>
                <strong>{amountInBrl(item.marketValue, item.currency)}</strong>
                <span
                  className={
                    asNumber(item.unrealizedGain) >= 0 ? "positive" : "negative"
                  }
                >
                  {percent(item.returnPercentage)}
                </span>
              </div>
            </div>
          ))}
          {income.data && (
            <div className="currency-row">
              <div>
                <strong>Proventos consolidados</strong>
                <span>Convertidos pela API</span>
              </div>
              <strong>
                {money(income.data.consolidatedReceivedAmount, "BRL")}
              </strong>
            </div>
          )}
          {summary?.exchangeRates.map((rate) => (
            <p
              className="muted"
              key={rate.sourceCurrency}
              style={{ fontSize: ".75rem" }}
            >
              1 {rate.sourceCurrency} = {number(rate.rate, 4)}{" "}
              {rate.targetCurrency} · {date(rate.referenceDate)}
            </p>
          ))}
        </Card>
      </div>
    </>
  );
}

function Kpi({
  label: text,
  value,
  icon,
  detail,
  tone = "",
}: {
  label: string;
  value: string;
  icon: ReactNode;
  detail: string;
  tone?: string;
}) {
  return (
    <Card className="kpi">
      <div className="kpi__top">
        <span>{text}</span>
        <span className="kpi__icon">{icon}</span>
      </div>
      <div className={`kpi__value ${tone}`}>{value}</div>
      <div className="kpi__detail">{detail}</div>
    </Card>
  );
}
function ChartCard<T>({
  title,
  subtitle,
  query,
  children,
}: {
  title: string;
  subtitle: string;
  query: UseQueryResult<T>;
  children: ReactNode;
}) {
  return (
    <Card>
      <div className="card-header">
        <div>
          <h3>{title}</h3>
          <p>{subtitle}</p>
        </div>
      </div>
      {query.isLoading ? (
        <Skeleton height={310} />
      ) : query.isError ? (
        <ErrorState
          message={apiMessage(query.error)}
          retry={() => query.refetch()}
        />
      ) : (
        children
      )}
    </Card>
  );
}
function PositionsCard({
  query,
  fallback,
  exchangeRates,
}: {
  query: UseQueryResult<Valuation>;
  fallback: Position[];
  exchangeRates: ExchangeRate[];
}) {
  const rows = query.data?.positions ?? fallback;
  const inBrl = (value: string | number, currency: string) => {
    const rate = exchangeRates.find(
      (item) => item.sourceCurrency === currency,
    )?.rate;
    if (currency !== "BRL" && !rate) return "—";
    return money(convertedAmount(value, currency === "BRL" ? 1 : rate), "BRL");
  };
  return (
    <Card>
      <div className="card-header">
        <div>
          <h3>Posições em custódia</h3>
          <p>Preço médio, valor atual e participação</p>
        </div>
      </div>
      {query.isLoading ? (
        <Skeleton height={220} />
      ) : !rows.length ? (
        <EmptyState title="Sem posições">
          Cadastre uma compra para começar.
        </EmptyState>
      ) : (
        <div className="table-wrap">
          <table className="data-table mobile-cards">
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Qtd.</th>
                <th>Preço médio</th>
                <th>Patrimônio</th>
                <th>Ganho</th>
                <th>Alocação</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => {
                const current =
                  "currentPrice" in item
                    ? (item as ValuationPosition)
                    : undefined;
                return (
                  <tr key={`${item.market}-${item.ticker}`}>
                    <td data-label="Ativo">
                      <Ticker item={item} />
                    </td>
                    <td data-label="Quantidade">{number(item.quantity, 8)}</td>
                    <td data-label="Preço médio">
                      {inBrl(item.averagePrice, item.currency)}
                    </td>
                    <td data-label="Patrimônio">
                      {current
                        ? inBrl(current.marketValue, item.currency)
                        : inBrl(item.custodyCost, item.currency)}
                    </td>
                    <td
                      data-label="Ganho"
                      className={
                        current && asNumber(current.unrealizedGain) >= 0
                          ? "positive"
                          : "negative"
                      }
                    >
                      {current
                        ? inBrl(current.unrealizedGain, item.currency)
                        : "—"}
                    </td>
                    <td data-label="Alocação">
                      {current ? percent(current.allocationPercentage) : "—"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
function Ticker({
  item,
}: {
  item: { ticker: string; assetName: string; market: string };
}) {
  return (
    <div className="ticker">
      <AssetLogo ticker={item.ticker} market={item.market} />
      <div>
        <strong>{item.ticker}</strong>
        <span>{item.assetName}</span>
      </div>
    </div>
  );
}

function BrlAmount({
  portfolioId,
  value,
  currency,
  referenceDate,
}: {
  portfolioId: string;
  value: string | number;
  currency: string;
  referenceDate: string;
}) {
  const exchange = useQuery({
    queryKey: ["exchange-rate", portfolioId, currency, referenceDate],
    queryFn: () =>
      api<ExchangeRate>(
        `/api/portfolios/${portfolioId}/exchange-rates?sourceCurrency=${currency}&date=${referenceDate}`,
      ),
    enabled: currency !== "BRL" && Boolean(referenceDate),
    retry: false,
  });
  if (currency === "BRL") return <>{money(value, "BRL")}</>;
  if (exchange.isLoading) return <>Convertendo…</>;
  if (!exchange.data) return <>Indisponível</>;
  return <>{money(convertedAmount(value, exchange.data.rate), "BRL")}</>;
}

function newTransactionForm(
  editing?: Transaction | null,
  exchangeRate?: number,
): TransactionUpdateInput {
  const displayRate = editing?.currency === "BRL" ? 1 : exchangeRate;
  return {
    type: editing?.type ?? "BUY",
    transactionDate: editing ? isoToPtBr(editing.transactionDate) : todayPtBr(),
    quantity: editing ? String(editing.quantity) : "",
    unitPrice:
      editing && displayRate
        ? formattedMoneyInput(
            String(convertedAmount(editing.unitPrice, displayRate)),
            "BRL",
          )
        : editing
          ? ""
          : formattedMoneyInput("0", "BRL"),
    costs:
      editing && !displayRate
        ? ""
        : formattedMoneyInput(
            String(convertedAmount(editing?.costs ?? 0, displayRate ?? 1)),
            "BRL",
          ),
  };
}

function TransactionDialog({
  open,
  onClose,
  portfolioId,
  editing,
}: {
  open: boolean;
  onClose: () => void;
  portfolioId: string;
  editing: Transaction | null;
}) {
  const queryClient = useQueryClient();
  const [market, setMarket] = useState<AssetMarket>("BR");
  const [assetType, setAssetType] = useState<AssetType>("STOCK");
  const [term, setTerm] = useState("");
  const [debounced, setDebounced] = useState("");
  const [selected, setSelected] = useState<AssetSearchResult | null>(null);
  const [form, setForm] = useState<TransactionUpdateInput>(() =>
    newTransactionForm(),
  );
  const [initializedRate, setInitializedRate] = useState(false);
  const [priceEdited, setPriceEdited] = useState(false);
  const [costsEdited, setCostsEdited] = useState(false);

  useEffect(() => {
    if (!open) return;
    setMarket(editing?.market ?? "BR");
    setAssetType(editing?.assetType ?? "STOCK");
    setTerm("");
    setDebounced("");
    setSelected(null);
    setForm(newTransactionForm(editing));
    setInitializedRate(editing?.currency === "BRL" || !editing);
    setPriceEdited(false);
    setCostsEdited(false);
  }, [open, editing]);

  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(term.trim()), 350);
    return () => clearTimeout(id);
  }, [term]);

  const search = useQuery({
    queryKey: ["assets", portfolioId, market, assetType, debounced],
    queryFn: () =>
      api<AssetSearchResult[]>(
        `/api/portfolios/${portfolioId}/assets?market=${market}&assetType=${assetType}&query=${encodeURIComponent(debounced)}`,
      ),
    enabled: open && !editing && debounced.length >= 2,
  });
  const transactionDate = ptBrToIso(form.transactionDate);
  const asset = editing
    ? {
        ticker: editing.ticker,
        name: editing.assetName,
        assetName: editing.assetName,
        market: editing.market,
        currency: editing.currency,
      }
    : selected;
  const sourceCurrency = editing?.currency ?? (market === "US" ? "USD" : "BRL");
  const rateQuery = useQuery({
    queryKey: ["exchange-rate", portfolioId, sourceCurrency, transactionDate],
    queryFn: () =>
      api<ExchangeRate>(
        `/api/portfolios/${portfolioId}/exchange-rates?sourceCurrency=${sourceCurrency}&date=${transactionDate}`,
      ),
    enabled: open && sourceCurrency !== "BRL" && Boolean(transactionDate),
    retry: false,
  });
  const exchangeRate =
    sourceCurrency === "BRL" ? 1 : asNumber(rateQuery.data?.rate);

  useEffect(() => {
    if (
      !open ||
      !editing ||
      editing.currency === "BRL" ||
      !exchangeRate ||
      initializedRate
    )
      return;
    setForm(newTransactionForm(editing, exchangeRate));
    setInitializedRate(true);
  }, [editing, exchangeRate, initializedRate, open]);
  useEffect(() => {
    if (!selected || selected.currency === "BRL" || !exchangeRate) return;
    setForm((current) => ({
      ...current,
      unitPrice: formattedMoneyInput(
        String(convertedAmount(selected.price, exchangeRate)),
        "BRL",
      ),
    }));
  }, [exchangeRate, selected]);
  const save = useMutation({
    mutationFn: () => {
      const body = {
        type: form.type,
        transactionDate,
        quantity: decimalInput(form.quantity),
        unitPrice:
          sourceCurrency === "BRL"
            ? decimalInput(form.unitPrice)
            : !priceEdited && (editing || selected)
              ? String(editing?.unitPrice ?? selected?.price)
              : String(
                  decimalForApi(
                    nativeAmount(
                      decimalInput(form.unitPrice),
                      exchangeRate || 1,
                    ),
                  ),
                ),
        costs:
          sourceCurrency === "BRL"
            ? decimalInput(form.costs) || "0"
            : !costsEdited
              ? String(editing?.costs ?? 0)
              : decimalForApi(
                  nativeAmount(
                    decimalInput(form.costs) || "0",
                    exchangeRate || 1,
                  ),
                ),
      };
      return editing
        ? api<Transaction>(
            `/api/portfolios/${portfolioId}/transactions/${editing.id}`,
            { method: "PUT", body: JSON.stringify(body) },
          )
        : api<Transaction>(`/api/portfolios/${portfolioId}/transactions`, {
            method: "POST",
            body: JSON.stringify({
              ...body,
              assetSelectionId: selected!.selectionId,
            }),
          });
    },
    onSuccess: (result) => {
      queryKeys(portfolioId).forEach((key) =>
        queryClient.invalidateQueries({ queryKey: key }),
      );
      notify(
        editing
          ? "Lançamento atualizado com sucesso."
          : `${label(result.type)} registrada como ${label(result.status).toLowerCase()}.`,
      );
      onClose();
    },
  });
  const future = Boolean(
    transactionDate && transactionDate > (ptBrToIso(todayPtBr()) ?? ""),
  );
  const gross =
    asNumber(decimalInput(form.quantity)) *
    asNumber(decimalInput(form.unitPrice));
  const valid = Boolean(
    asset &&
    transactionDate &&
    (sourceCurrency === "BRL" || exchangeRate > 0) &&
    asNumber(decimalInput(form.quantity)) > 0 &&
    asNumber(decimalInput(form.unitPrice)) > 0,
  );
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={editing ? "Editar lançamento" : "Novo lançamento"}
    >
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          if (valid) save.mutate();
        }}
      >
        <div className="form-grid">
          {editing ? (
            <Card className="span-2">
              <Ticker item={editing} />
              <p className="muted" style={{ margin: ".65rem 0 0" }}>
                Somente lançamentos pendentes podem ser corrigidos.
              </p>
            </Card>
          ) : (
            <>
              <SelectField
                label="Mercado"
                value={market}
                onChange={(event) => {
                  setMarket(event.target.value as AssetMarket);
                  setSelected(null);
                }}
              >
                <option value="BR">Brasil</option>
                <option value="US">Estados Unidos</option>
              </SelectField>
              <SelectField
                label="Tipo de ativo"
                value={assetType}
                onChange={(event) => {
                  setAssetType(event.target.value as AssetType);
                  setSelected(null);
                }}
              >
                <option value="STOCK">Ação</option>
                <option value="ETF">ETF</option>
              </SelectField>
              <Field
                className="span-2"
                label="Pesquisar ativo"
                value={term}
                onChange={(event) => {
                  setTerm(event.target.value);
                  setSelected(null);
                }}
                placeholder="Ticker ou nome do ativo"
              />
              {search.isFetching && (
                <div className="span-2 muted">Buscando ativos…</div>
              )}
              {search.isError && (
                <div className="span-2 error-state">
                  {apiMessage(search.error)}
                </div>
              )}
              {search.data && (
                <div className="span-2 search-results">
                  {search.data.map((item) => (
                    <button
                      type="button"
                      className={`asset-result ${selected?.selectionId === item.selectionId ? "selected" : ""}`}
                      key={item.selectionId}
                      onClick={() => {
                        setSelected(item);
                        setPriceEdited(false);
                        setCostsEdited(false);
                        setForm((current) => ({
                          ...current,
                          unitPrice: formattedMoneyInput(
                            String(
                              convertedAmount(item.price, exchangeRate || 1),
                            ),
                            "BRL",
                          ),
                          costs: formattedMoneyInput(
                            decimalInput(current.costs) || "0",
                            "BRL",
                          ),
                        }));
                      }}
                    >
                      <div className="asset-result__identity">
                        <AssetLogo ticker={item.ticker} market={item.market} />
                        <div>
                          <strong>{item.ticker}</strong>
                          <span>
                            {item.name} · {label(item.assetType)}
                          </span>
                        </div>
                      </div>
                      <strong>
                        {item.currency === "BRL"
                          ? money(item.price, "BRL")
                          : exchangeRate
                            ? money(
                                convertedAmount(item.price, exchangeRate),
                                "BRL",
                              )
                            : "Câmbio indisponível"}
                      </strong>
                    </button>
                  ))}
                </div>
              )}
              {market === "US" && search.data?.length ? (
                <div className="span-2">
                  <AssetLogoAttribution />
                </div>
              ) : null}
            </>
          )}
          <SelectField
            label="Operação"
            value={form.type}
            onChange={(event) =>
              setForm({ ...form, type: event.target.value as TransactionType })
            }
          >
            <option value="BUY">Compra</option>
            <option value="SELL">Venda</option>
          </SelectField>
          <DateField
            label="Data"
            value={form.transactionDate}
            onValueChange={(value) =>
              setForm({
                ...form,
                transactionDate: value,
              })
            }
            error={
              form.transactionDate.length === 10 && !transactionDate
                ? "Informe uma data válida."
                : undefined
            }
            required
          />
          <Field
            label="Quantidade"
            inputMode="decimal"
            value={form.quantity}
            onChange={(event) =>
              setForm({
                ...form,
                quantity: event.target.value.replace(",", "."),
              })
            }
            required
          />
          <Field
            label="Preço unitário (BRL)"
            inputMode="decimal"
            value={form.unitPrice}
            onFocus={() =>
              setForm((current) => ({
                ...current,
                unitPrice: decimalInput(current.unitPrice),
              }))
            }
            onChange={(event) => {
              setPriceEdited(true);
              setForm((current) => ({
                ...current,
                unitPrice: event.target.value,
              }));
            }}
            onBlur={(event) => {
              const value = event.currentTarget.value;
              setForm((current) => ({
                ...current,
                unitPrice: formattedMoneyInput(value || "0", "BRL"),
              }));
            }}
            required
          />
          <Field
            label="Custos (BRL)"
            inputMode="decimal"
            value={form.costs}
            onFocus={() =>
              setForm((current) => ({
                ...current,
                costs: decimalInput(current.costs),
              }))
            }
            onChange={(event) => {
              setCostsEdited(true);
              setForm((current) => ({
                ...current,
                costs: event.target.value,
              }));
            }}
            onBlur={(event) => {
              const value = event.currentTarget.value;
              setForm((current) => ({
                ...current,
                costs: formattedMoneyInput(value || "0", "BRL"),
              }));
            }}
          />
          <Card className="span-2">
            <strong>Resumo da operação</strong>
            <p className="muted" style={{ margin: ".4rem 0 0" }}>
              {asset
                ? `${form.type === "BUY" ? "Compra" : "Venda"} de ${form.quantity || 0} ${asset.ticker} · bruto ${money(gross, "BRL")}`
                : "Selecione um ativo para continuar."}
            </p>
            {future && (
              <p style={{ color: "#92500b", margin: ".5rem 0 0" }}>
                <CalendarClock size={15} /> Data futura: o lançamento ficará
                pendente.
              </p>
            )}
          </Card>
          {sourceCurrency !== "BRL" && (
            <div className="span-2 muted">
              {rateQuery.isFetching
                ? "Consultando câmbio para reais…"
                : rateQuery.isError
                  ? apiMessage(rateQuery.error)
                  : exchangeRate
                    ? `Valores exibidos em reais · 1 ${sourceCurrency} = ${money(exchangeRate, "BRL")} · referência ${date(rateQuery.data?.referenceDate)}`
                    : "Selecione uma data válida para consultar o câmbio."}
            </div>
          )}
        </div>
        {save.isError && (
          <div className="error-state">{apiMessage(save.error)}</div>
        )}
        <div className="form-actions">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={save.isPending} disabled={!valid}>
            {editing ? "Salvar alterações" : "Registrar lançamento"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function TransactionsPanel({
  portfolioId,
  query,
  onNew,
  onEdit,
}: {
  portfolioId: string;
  query: UseQueryResult<Transaction[]>;
  onNew: () => void;
  onEdit: (transaction: Transaction) => void;
}) {
  const queryClient = useQueryClient();
  const [text, setText] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [cancel, setCancel] = useState<Transaction | null>(null);
  const remove = useMutation({
    mutationFn: (id: string) =>
      api<void>(`/api/portfolios/${portfolioId}/transactions/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      queryKeys(portfolioId).forEach((key) =>
        queryClient.invalidateQueries({ queryKey: key }),
      );
      setCancel(null);
      notify("Lançamento cancelado.");
    },
  });
  const rows = useMemo(
    () =>
      query.data
        ?.filter(
          (item) =>
            (!text ||
              `${item.ticker} ${item.assetName}`
                .toLowerCase()
                .includes(text.toLowerCase())) &&
            (!type || item.type === type) &&
            (!status || item.status === status),
        )
        .sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)) ??
      [],
    [query.data, text, type, status],
  );
  return (
    <>
      <Card>
        <div className="card-header">
          <div>
            <h2>Histórico de lançamentos</h2>
            <p>Seu log completo de compras e vendas.</p>
          </div>
          <Button onClick={onNew}>
            <Plus size={17} />
            Novo
          </Button>
        </div>
        <div className="filter-bar">
          <input
            aria-label="Buscar lançamento"
            placeholder="Buscar ativo"
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <select
            aria-label="Filtrar por tipo"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            <option value="">Todos os tipos</option>
            <option value="BUY">Compras</option>
            <option value="SELL">Vendas</option>
          </select>
          <select
            aria-label="Filtrar por estado"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">Todos os estados</option>
            <option value="EFFECTIVE">Efetivados</option>
            <option value="PENDING">Pendentes</option>
            <option value="CANCELLED">Cancelados</option>
          </select>
        </div>
        {query.isLoading ? (
          <Skeleton height={260} />
        ) : query.isError ? (
          <ErrorState
            message={apiMessage(query.error)}
            retry={() => query.refetch()}
          />
        ) : !query.data?.length ? (
          <EmptyState title="Nenhum lançamento">
            Registre uma compra ou venda para formar seu histórico.
          </EmptyState>
        ) : !rows.length ? (
          <EmptyState icon={<Search />} title="Nenhum resultado">
            Revise os filtros utilizados.
          </EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="data-table mobile-cards">
              <thead>
                <tr>
                  <th>Ativo</th>
                  <th>Operação</th>
                  <th>Data</th>
                  <th>Quantidade</th>
                  <th>Preço</th>
                  <th>Custos</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((item) => (
                  <tr key={item.id}>
                    <td data-label="Ativo">
                      <Ticker item={item} />
                    </td>
                    <td data-label="Operação">
                      <Badge tone={item.type === "BUY" ? "success" : "info"}>
                        {label(item.type)}
                      </Badge>
                    </td>
                    <td data-label="Data">{date(item.transactionDate)}</td>
                    <td data-label="Quantidade">{number(item.quantity, 8)}</td>
                    <td data-label="Preço">
                      <BrlAmount
                        portfolioId={portfolioId}
                        value={item.unitPrice}
                        currency={item.currency}
                        referenceDate={item.transactionDate}
                      />
                    </td>
                    <td data-label="Custos">
                      <BrlAmount
                        portfolioId={portfolioId}
                        value={item.costs}
                        currency={item.currency}
                        referenceDate={item.transactionDate}
                      />
                    </td>
                    <td data-label="Estado">
                      <StatusBadge value={item.status} />
                    </td>
                    <td data-label="Ação">
                      {item.status === "PENDING" && (
                        <div className="table-actions">
                          <Button
                            variant="secondary"
                            onClick={() => onEdit(item)}
                          >
                            <Pencil size={15} />
                            Editar
                          </Button>
                          <Button
                            variant="danger"
                            onClick={() => setCancel(item)}
                          >
                            Cancelar
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      <Dialog
        open={Boolean(cancel)}
        onClose={() => setCancel(null)}
        title="Cancelar lançamento?"
      >
        <p>
          O lançamento pendente de <strong>{cancel?.ticker}</strong> será
          cancelado e não afetará a carteira.
        </p>
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setCancel(null)}>
            Voltar
          </Button>
          <Button
            variant="danger"
            loading={remove.isPending}
            onClick={() => cancel && remove.mutate(cancel.id)}
          >
            Confirmar cancelamento
          </Button>
        </div>
      </Dialog>
    </>
  );
}

function IncomePanel({ portfolioId }: { portfolioId: string }) {
  const queryClient = useQueryClient();
  const [market, setMarket] = useState<AssetMarket>("BR");
  const [manualOpen, setManualOpen] = useState(false);
  const [candidate, setCandidate] = useState<IncomeCandidate | null>(null);
  const [received, setReceived] = useState("");
  const [reason, setReason] = useState("");
  const [filter, setFilter] = useState("");
  const [cancelling, setCancelling] = useState<IncomeEvent | null>(null);
  const events = useQuery({
    queryKey: ["income-events", portfolioId],
    queryFn: () =>
      api<IncomeEvent[]>(`/api/portfolios/${portfolioId}/income-events`),
  });
  const summary = useQuery({
    queryKey: ["income-summary", portfolioId],
    queryFn: () =>
      api<IncomeSummary>(
        `/api/portfolios/${portfolioId}/income-events/summary`,
      ),
    retry: false,
  });
  const candidates = useQuery({
    queryKey: ["income-candidates", portfolioId, market],
    queryFn: () =>
      api<IncomeCandidate[]>(
        `/api/portfolios/${portfolioId}/income-events/candidates?market=${market}`,
      ),
    retry: false,
  });
  const candidateRate = useQuery({
    queryKey: [
      "exchange-rate",
      portfolioId,
      candidate?.currency,
      candidate?.paymentDate,
    ],
    queryFn: () =>
      api<ExchangeRate>(
        `/api/portfolios/${portfolioId}/exchange-rates?sourceCurrency=${candidate!.currency}&date=${candidate!.paymentDate}`,
      ),
    enabled: Boolean(candidate && candidate.currency !== "BRL"),
    retry: false,
  });
  const candidateExchangeRate =
    candidate?.currency === "BRL" ? 1 : asNumber(candidateRate.data?.rate);
  useEffect(() => {
    if (!candidate) return;
    if (candidate.currency === "BRL") {
      setReceived(formattedMoneyInput(String(candidate.expectedAmount), "BRL"));
      return;
    }
    if (candidateExchangeRate > 0)
      setReceived(
        formattedMoneyInput(
          String(
            convertedAmount(candidate.expectedAmount, candidateExchangeRate),
          ),
          "BRL",
        ),
      );
  }, [candidate, candidateExchangeRate]);
  const refresh = () => {
    [
      ["income-events", portfolioId],
      ["income-summary", portfolioId],
      ["income-candidates", portfolioId, market],
    ].forEach((key) => queryClient.invalidateQueries({ queryKey: key }));
  };
  const confirm = useMutation({
    mutationFn: () =>
      api<IncomeEvent>(
        `/api/portfolios/${portfolioId}/income-events/confirmations`,
        {
          method: "POST",
          body: JSON.stringify({
            candidateId: candidate!.candidateId,
            receivedAmount:
              candidate?.currency === "BRL"
                ? decimalInput(received)
                : String(
                    nativeAmount(
                      decimalInput(received),
                      candidateExchangeRate || 1,
                    ),
                  ),
            adjustmentReason: reason || null,
          }),
        },
      ),
    onSuccess: () => {
      refresh();
      setCandidate(null);
      notify("Provento confirmado.");
    },
  });
  const cancel = useMutation({
    mutationFn: (id: string) =>
      api<void>(`/api/portfolios/${portfolioId}/income-events/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      refresh();
      setCancelling(null);
      notify("Provento cancelado.");
    },
  });
  const filtered =
    events.data?.filter(
      (item) =>
        !filter ||
        `${item.ticker} ${item.assetName}`
          .toLowerCase()
          .includes(filter.toLowerCase()),
    ) ?? [];
  return (
    <>
      <div className="grid grid-3">
        {summary.isLoading ? (
          <>
            <Skeleton />
            <Skeleton />
            <Skeleton />
          </>
        ) : summary.isError ? (
          <ErrorState
            message={apiMessage(summary.error)}
            retry={() => summary.refetch()}
          />
        ) : (
          summary.data && (
            <Kpi
              label="Proventos consolidados"
              value={money(summary.data.consolidatedReceivedAmount, "BRL")}
              icon={<Banknote />}
              detail="Total efetivamente recebido em reais"
            />
          )
        )}
      </div>
      <div className="dashboard-grid">
        <Card>
          <div className="card-header">
            <div>
              <h2>Proventos identificados</h2>
              <p>Eventos encontrados pelas integrações</p>
            </div>
            <select
              className="control"
              value={market}
              onChange={(e) => setMarket(e.target.value as AssetMarket)}
              aria-label="Mercado dos candidatos"
            >
              <option value="BR">Brasil</option>
              <option value="US">Estados Unidos</option>
            </select>
          </div>
          {candidates.isLoading ? (
            <Skeleton height={220} />
          ) : candidates.isError ? (
            <ErrorState
              message={apiMessage(candidates.error)}
              retry={() => candidates.refetch()}
            />
          ) : !candidates.data?.length ? (
            <EmptyState title="Nenhum candidato encontrado">
              Não há eventos disponíveis para confirmação neste mercado.
            </EmptyState>
          ) : (
            <>
              <div className="search-results">
                {candidates.data.map((item) => (
                  <div className="asset-result" key={item.candidateId}>
                    <div className="asset-result__identity">
                      <AssetLogo ticker={item.ticker} market={item.market} />
                      <div>
                        <strong>
                          {item.ticker} · {label(item.type)}
                        </strong>
                        <span>
                          Pagamento {date(item.paymentDate)} · esperado{" "}
                          <BrlAmount
                            portfolioId={portfolioId}
                            value={item.expectedAmount}
                            currency={item.currency}
                            referenceDate={item.paymentDate}
                          />
                        </span>
                      </div>
                    </div>
                    {item.alreadyRecorded ? (
                      <Badge>Já registrado</Badge>
                    ) : item.confirmable ? (
                      <Button
                        variant="secondary"
                        onClick={() => {
                          setCandidate(item);
                          setReceived("");
                        }}
                      >
                        Confirmar
                      </Button>
                    ) : (
                      <Badge tone="warning">Inelegível</Badge>
                    )}
                  </div>
                ))}
              </div>
              {market === "US" && <AssetLogoAttribution />}
            </>
          )}
        </Card>
        <Card>
          <div className="card-header">
            <div>
              <h2>Resumo consolidado</h2>
              <p>Recebimentos por moeda</p>
            </div>
            <Button onClick={() => setManualOpen(true)}>
              <Plus size={16} />
              Manual
            </Button>
          </div>
          {summary.data && (
            <div className="currency-row">
              <div>
                <strong>Total em {summary.data.baseCurrency}</strong>
                <span>Consolidado pela API</span>
              </div>
              <strong>
                {money(
                  summary.data.consolidatedReceivedAmount,
                  summary.data.baseCurrency,
                )}
              </strong>
            </div>
          )}
        </Card>
      </div>
      <Card>
        <div className="card-header">
          <div>
            <h2>Histórico de proventos</h2>
            <p>Eventos confirmados e manuais</p>
          </div>
        </div>
        <div className="filter-bar">
          <input
            placeholder="Buscar ativo"
            aria-label="Buscar provento"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </div>
        {events.isLoading ? (
          <Skeleton height={230} />
        ) : events.isError ? (
          <ErrorState
            message={apiMessage(events.error)}
            retry={() => events.refetch()}
          />
        ) : !filtered.length ? (
          <EmptyState title="Nenhum provento registrado">
            Confirme um candidato ou adicione um evento manual.
          </EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="data-table mobile-cards">
              <thead>
                <tr>
                  <th>Ativo</th>
                  <th>Tipo</th>
                  <th>Pagamento</th>
                  <th>Recebido</th>
                  <th>Origem</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id}>
                    <td data-label="Ativo">
                      <Ticker item={item} />
                    </td>
                    <td data-label="Tipo">{label(item.type)}</td>
                    <td data-label="Pagamento">{date(item.paymentDate)}</td>
                    <td data-label="Recebido">
                      <BrlAmount
                        portfolioId={portfolioId}
                        value={item.receivedAmount}
                        currency={item.currency}
                        referenceDate={item.paymentDate}
                      />
                    </td>
                    <td data-label="Origem">
                      <Badge tone="info">{label(item.source)}</Badge>
                    </td>
                    <td data-label="Estado">
                      <StatusBadge value={item.status} />
                    </td>
                    <td data-label="Ação">
                      {item.status === "PENDING" && (
                        <Button
                          variant="danger"
                          loading={cancel.isPending}
                          onClick={() => setCancelling(item)}
                        >
                          Cancelar
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {filtered.some((item) => item.market === "US") && (
        <AssetLogoAttribution />
      )}
      <Dialog
        open={Boolean(candidate)}
        onClose={() => setCandidate(null)}
        title="Confirmar provento"
      >
        <p>
          <strong>{candidate?.ticker}</strong> ·{" "}
          {candidate && label(candidate.type)} com pagamento em{" "}
          {date(candidate?.paymentDate)}
        </p>
        <div className="form-grid">
          <Field
            label="Valor recebido (BRL)"
            value={received}
            onFocus={() => setReceived(decimalInput(received))}
            onChange={(e) => setReceived(e.target.value)}
            onBlur={(e) =>
              setReceived(
                formattedMoneyInput(e.currentTarget.value || "0", "BRL"),
              )
            }
          />
          <Field
            label="Motivo do ajuste"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={500}
            placeholder="Opcional"
          />
        </div>
        {confirm.isError && (
          <div className="error-state">{apiMessage(confirm.error)}</div>
        )}
        {candidate?.currency !== "BRL" && candidate && (
          <p className="muted">
            {candidateRate.isFetching
              ? "Consultando câmbio para reais…"
              : candidateRate.isError
                ? apiMessage(candidateRate.error)
                : candidateExchangeRate
                  ? `Valor exibido em reais · 1 ${candidate.currency} = ${money(candidateExchangeRate, "BRL")}.`
                  : "Câmbio indisponível."}
          </p>
        )}
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setCandidate(null)}>
            Cancelar
          </Button>
          <Button
            loading={confirm.isPending}
            disabled={
              asNumber(decimalInput(received)) <= 0 ||
              (candidate?.currency !== "BRL" && candidateExchangeRate <= 0)
            }
            onClick={() => confirm.mutate()}
          >
            Confirmar recebimento
          </Button>
        </div>
      </Dialog>
      <Dialog
        open={Boolean(cancelling)}
        onClose={() => setCancelling(null)}
        title="Cancelar provento?"
      >
        <p>
          O provento pendente de <strong>{cancelling?.ticker}</strong> será
          cancelado e deixará de compor os resumos da carteira.
        </p>
        {cancel.isError && (
          <div className="error-state">{apiMessage(cancel.error)}</div>
        )}
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setCancelling(null)}>
            Voltar
          </Button>
          <Button
            variant="danger"
            loading={cancel.isPending}
            onClick={() => cancelling && cancel.mutate(cancelling.id)}
          >
            Confirmar cancelamento
          </Button>
        </div>
      </Dialog>
      <ManualIncomeDialog
        open={manualOpen}
        onClose={() => setManualOpen(false)}
        portfolioId={portfolioId}
        onSuccess={refresh}
      />
    </>
  );
}

const newManualIncomeForm = () => ({
  ticker: "",
  type: "DIVIDEND" as IncomeEventType,
  paymentDate: todayPtBr(),
  receivedAmount: formattedMoneyInput("0", "BRL"),
  eligibilityDate: "",
  eligibleQuantity: "",
  unitAmount: formattedMoneyInput("0", "BRL"),
  notes: "",
});

function ManualIncomeDialog({
  open,
  onClose,
  portfolioId,
  onSuccess,
}: {
  open: boolean;
  onClose: () => void;
  portfolioId: string;
  onSuccess: () => void;
}) {
  const [form, setForm] = useState(newManualIncomeForm);
  const positions = useQuery({
    queryKey: ["positions", portfolioId],
    queryFn: () => api<Position[]>(`/api/portfolios/${portfolioId}/positions`),
    enabled: open,
  });
  const selectedPosition = positions.data?.find(
    (item) => item.ticker === form.ticker,
  );
  const paymentDate = ptBrToIso(form.paymentDate);
  const eligibilityDate = form.eligibilityDate
    ? ptBrToIso(form.eligibilityDate)
    : null;
  const sourceCurrency = selectedPosition?.currency ?? "BRL";
  const rateQuery = useQuery({
    queryKey: ["exchange-rate", portfolioId, sourceCurrency, paymentDate],
    queryFn: () =>
      api<ExchangeRate>(
        `/api/portfolios/${portfolioId}/exchange-rates?sourceCurrency=${sourceCurrency}&date=${paymentDate}`,
      ),
    enabled: open && sourceCurrency !== "BRL" && Boolean(paymentDate),
    retry: false,
  });
  const exchangeRate =
    sourceCurrency === "BRL" ? 1 : asNumber(rateQuery.data?.rate);
  useEffect(() => {
    if (open) setForm(newManualIncomeForm());
  }, [open]);
  const create = useMutation({
    mutationFn: () =>
      api<IncomeEvent>(`/api/portfolios/${portfolioId}/income-events/manual`, {
        method: "POST",
        body: JSON.stringify({
          ...form,
          ticker: form.ticker.toUpperCase(),
          paymentDate,
          receivedAmount:
            sourceCurrency === "BRL"
              ? decimalInput(form.receivedAmount)
              : String(
                  nativeAmount(
                    decimalInput(form.receivedAmount),
                    exchangeRate || 1,
                  ),
                ),
          eligibilityDate,
          eligibleQuantity: form.eligibleQuantity || null,
          unitAmount: asNumber(decimalInput(form.unitAmount))
            ? sourceCurrency === "BRL"
              ? decimalInput(form.unitAmount)
              : String(
                  nativeAmount(
                    decimalInput(form.unitAmount),
                    exchangeRate || 1,
                  ),
                )
            : null,
          notes: form.notes || null,
        }),
      }),
    onSuccess: () => {
      onSuccess();
      onClose();
      setForm(newManualIncomeForm());
      notify("Provento manual registrado.");
    },
  });
  return (
    <Dialog open={open} onClose={onClose} title="Provento manual">
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          if (
            form.ticker &&
            paymentDate &&
            asNumber(decimalInput(form.receivedAmount)) > 0 &&
            (sourceCurrency === "BRL" || exchangeRate > 0)
          )
            create.mutate();
        }}
      >
        <div className="form-grid">
          <SelectField
            label="Ticker"
            value={form.ticker}
            onChange={(e) => setForm({ ...form, ticker: e.target.value })}
            required
          >
            <option value="">Selecione um ativo em custódia</option>
            {positions.data?.map((item) => (
              <option key={`${item.market}-${item.ticker}`} value={item.ticker}>
                {item.ticker} — {item.assetName}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Tipo"
            value={form.type}
            onChange={(e) =>
              setForm({ ...form, type: e.target.value as IncomeEventType })
            }
          >
            <option value="DIVIDEND">Dividendo</option>
            <option value="INTEREST_ON_EQUITY">Juros sobre capital</option>
            <option value="DISTRIBUTION">Distribuição</option>
          </SelectField>
          <DateField
            label="Data de pagamento"
            value={form.paymentDate}
            onValueChange={(value) => setForm({ ...form, paymentDate: value })}
            error={
              form.paymentDate.length === 10 && !paymentDate
                ? "Informe uma data válida."
                : undefined
            }
            required
          />
          <Field
            label="Valor recebido (BRL)"
            inputMode="decimal"
            value={form.receivedAmount}
            onFocus={() =>
              setForm({
                ...form,
                receivedAmount: decimalInput(form.receivedAmount),
              })
            }
            onChange={(e) =>
              setForm({ ...form, receivedAmount: e.target.value })
            }
            onBlur={(e) =>
              setForm({
                ...form,
                receivedAmount: formattedMoneyInput(
                  e.currentTarget.value || "0",
                  "BRL",
                ),
              })
            }
            required
          />
          <DateField
            label="Data de elegibilidade"
            value={form.eligibilityDate}
            onValueChange={(value) =>
              setForm({ ...form, eligibilityDate: value })
            }
            error={
              form.eligibilityDate.length === 10 && !eligibilityDate
                ? "Informe uma data válida."
                : undefined
            }
          />
          <Field
            label="Quantidade elegível"
            inputMode="decimal"
            value={form.eligibleQuantity}
            onChange={(e) =>
              setForm({
                ...form,
                eligibleQuantity: e.target.value.replace(",", "."),
              })
            }
          />
          <Field
            label="Valor unitário (BRL)"
            inputMode="decimal"
            value={form.unitAmount}
            onFocus={() =>
              setForm({ ...form, unitAmount: decimalInput(form.unitAmount) })
            }
            onChange={(e) => setForm({ ...form, unitAmount: e.target.value })}
            onBlur={(e) =>
              setForm({
                ...form,
                unitAmount: formattedMoneyInput(
                  e.currentTarget.value || "0",
                  "BRL",
                ),
              })
            }
          />
          <Field
            label="Observações"
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
            maxLength={500}
          />
        </div>
        {positions.isError && (
          <div className="error-state">{apiMessage(positions.error)}</div>
        )}
        {sourceCurrency !== "BRL" && (
          <p className="muted">
            {rateQuery.isFetching
              ? "Consultando câmbio para reais…"
              : rateQuery.isError
                ? apiMessage(rateQuery.error)
                : exchangeRate
                  ? `Valores exibidos em reais · 1 ${sourceCurrency} = ${money(exchangeRate, "BRL")}.`
                  : "Informe uma data válida para consultar o câmbio."}
          </p>
        )}
        {create.isError && (
          <div className="error-state">{apiMessage(create.error)}</div>
        )}
        <div className="form-actions">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            type="submit"
            loading={create.isPending}
            disabled={
              !form.ticker ||
              !paymentDate ||
              asNumber(decimalInput(form.receivedAmount)) <= 0 ||
              (sourceCurrency !== "BRL" && exchangeRate <= 0)
            }
          >
            Registrar provento
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function StatusBadge({ value }: { value: TransactionStatus | string }) {
  return (
    <Badge
      tone={
        value === "EFFECTIVE" || value === "ACTIVE"
          ? "success"
          : value === "PENDING"
            ? "warning"
            : value === "CANCELLED" || value === "INACTIVE"
              ? "danger"
              : "neutral"
      }
    >
      {label(value)}
    </Badge>
  );
}
