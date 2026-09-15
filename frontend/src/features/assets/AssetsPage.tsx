import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChartCandlestick, Plus, RefreshCw, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { AssetLogo } from "../../components/AssetLogo";
import {
  Badge,
  Button,
  Card,
  Dialog,
  EmptyState,
  ErrorState,
  Field,
  PageHeading,
  SelectField,
  Skeleton,
  notify,
} from "../../components/ui";
import { api, apiMessage } from "../../lib/http";
import { asNumber, convertedAmount, label, money } from "../../lib/format";
import type {
  AssetMarket,
  AssetSearchResult,
  AssetType,
  ExchangeRate,
  RegisteredAsset,
} from "../../types/api";

type MarketFilter = "ALL" | AssetMarket;

function quoteDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function todayIso() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function AssetsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<MarketFilter>("ALL");
  const [open, setOpen] = useState(false);
  const [market, setMarket] = useState<AssetMarket>("BR");
  const [assetType, setAssetType] = useState<AssetType>("STOCK");
  const [term, setTerm] = useState("");
  const [debounced, setDebounced] = useState("");
  const [selected, setSelected] = useState<AssetSearchResult | null>(null);
  const [removing, setRemoving] = useState<RegisteredAsset | "ALL" | null>(
    null,
  );

  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(term.trim()), 350);
    return () => window.clearTimeout(id);
  }, [term]);

  const assets = useQuery({
    queryKey: ["registered-assets", filter],
    queryFn: () =>
      api<RegisteredAsset[]>(
        `/api/assets${filter === "ALL" ? "" : `?market=${filter}`}`,
      ),
  });
  const search = useQuery({
    queryKey: ["asset-registration-search", market, assetType, debounced],
    queryFn: () =>
      api<AssetSearchResult[]>(
        `/api/assets/search?market=${market}&assetType=${assetType}&query=${encodeURIComponent(debounced)}`,
      ),
    enabled: open && debounced.length >= 2,
  });
  const needsUsdRate =
    assets.data?.some((item) => item.market === "US") ||
    (open && market === "US");
  const exchangeRate = useQuery({
    queryKey: ["asset-catalog-exchange-rate", "USD", todayIso()],
    queryFn: () =>
      api<ExchangeRate>(
        `/api/assets/exchange-rate?sourceCurrency=USD&date=${todayIso()}`,
      ),
    enabled: Boolean(needsUsdRate),
    retry: false,
  });
  const usdToBrl = asNumber(exchangeRate.data?.rate);
  const create = useMutation({
    mutationFn: (selectionId: string) =>
      api<RegisteredAsset>("/api/assets", {
        method: "POST",
        body: JSON.stringify({ assetSelectionId: selectionId }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["registered-assets"] });
      closeDialog();
      notify("Ativo cadastrado com sucesso.");
    },
  });
  const refresh = useMutation({
    mutationFn: (assetId: string) =>
      api<RegisteredAsset>(`/api/assets/${assetId}/quote-refresh`, {
        method: "POST",
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["registered-assets"] });
      notify("Cotação atualizada com sucesso.");
    },
  });
  const remove = useMutation({
    mutationFn: (target: RegisteredAsset | "ALL") =>
      api<void>(target === "ALL" ? "/api/assets" : `/api/assets/${target.id}`, {
        method: "DELETE",
      }),
    onSuccess: (_, target) => {
      queryClient.invalidateQueries({ queryKey: ["registered-assets"] });
      setRemoving(null);
      notify(
        target === "ALL"
          ? "Ativos excluídos com sucesso."
          : "Ativo excluído com sucesso.",
      );
    },
  });

  const brlQuote = (
    value: RegisteredAsset["lastQuote"] | AssetSearchResult["price"],
    currency: string,
  ) =>
    currency === "BRL"
      ? money(value, "BRL")
      : usdToBrl > 0
        ? money(convertedAmount(value, usdToBrl), "BRL")
        : exchangeRate.isFetching
          ? "Consultando câmbio…"
          : "Câmbio indisponível";

  function closeDialog() {
    setOpen(false);
    setMarket("BR");
    setAssetType("STOCK");
    setTerm("");
    setDebounced("");
    setSelected(null);
    create.reset();
  }

  return (
    <>
      <PageHeading
        eyebrow="Investimentos"
        title="Seus ativos"
        description="Cadastre os ativos que poderão ser usados em qualquer uma das suas carteiras."
        actions={
          <>
            {filter === "ALL" && assets.data?.length ? (
              <Button variant="danger" onClick={() => setRemoving("ALL")}>
                <Trash2 size={17} /> Excluir todos
              </Button>
            ) : null}
            <Button onClick={() => setOpen(true)}>
              <Plus size={18} /> Adicionar ativo
            </Button>
          </>
        }
      />
      <div
        className="tabs asset-filters"
        role="group"
        aria-label="Filtrar ativos por mercado"
      >
        {(
          [
            ["ALL", "Todos"],
            ["BR", "Brasileiros"],
            ["US", "Americanos"],
          ] as const
        ).map(([value, text]) => (
          <button
            key={value}
            type="button"
            className={filter === value ? "active" : ""}
            aria-pressed={filter === value}
            onClick={() => setFilter(value)}
          >
            {text}
          </button>
        ))}
      </div>
      {assets.isLoading ? (
        <div className="grid grid-3">
          <Skeleton height={230} />
          <Skeleton height={230} />
          <Skeleton height={230} />
        </div>
      ) : assets.isError ? (
        <ErrorState
          message={apiMessage(assets.error)}
          retry={() => assets.refetch()}
        />
      ) : !assets.data?.length ? (
        <Card>
          <EmptyState
            icon={<ChartCandlestick size={42} />}
            title={
              filter === "ALL"
                ? "Cadastre seu primeiro ativo"
                : "Nenhum ativo neste mercado"
            }
            action={
              <Button onClick={() => setOpen(true)}>
                <Plus size={17} /> Cadastrar ativo
              </Button>
            }
          >
            Pesquise uma ação ou ETF para validar sua identidade e registrar a
            cotação atual.
          </EmptyState>
        </Card>
      ) : (
        <div className="grid grid-3">
          {assets.data.map((item) => (
            <Card className="asset-card" key={item.id}>
              <div className="asset-card__heading">
                <AssetLogo ticker={item.ticker} market={item.market} />
                <div>
                  <h3>{item.ticker}</h3>
                  <p className="muted">{item.name}</p>
                </div>
              </div>
              <div className="asset-card__badges">
                <Badge>
                  {item.market === "BR" ? "Brasil" : "Estados Unidos"}
                </Badge>
                <Badge>{label(item.assetType)}</Badge>
              </div>
              <div className="asset-card__quote">
                <span>Última cotação cadastrada</span>
                <strong>{brlQuote(item.lastQuote, item.currency)}</strong>
                <small>Consultada em {quoteDate(item.quotedAt)}</small>
              </div>
              <div className="asset-card__actions">
                <Button
                  variant="secondary"
                  loading={refresh.isPending && refresh.variables === item.id}
                  disabled={refresh.isPending && refresh.variables !== item.id}
                  onClick={() => refresh.mutate(item.id)}
                >
                  <RefreshCw size={16} /> Atualizar cotação
                </Button>
                <Button variant="danger" onClick={() => setRemoving(item)}>
                  <Trash2 size={16} /> Excluir
                </Button>
              </div>
              {refresh.isError && refresh.variables === item.id ? (
                <div className="error-state">{apiMessage(refresh.error)}</div>
              ) : null}
            </Card>
          ))}
        </div>
      )}

      <Dialog open={open} onClose={closeDialog} title="Cadastrar ativo">
        <form
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            if (selected) create.mutate(selected.selectionId);
          }}
        >
          <div className="form-grid">
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
            {search.isFetching ? (
              <div className="span-2 muted">Buscando ativos…</div>
            ) : null}
            {search.isError ? (
              <div className="span-2 error-state">
                {apiMessage(search.error)}
              </div>
            ) : null}
            {search.data ? (
              <div className="span-2 search-results">
                {search.data.map((item) => (
                  <button
                    type="button"
                    className={`asset-result ${selected?.selectionId === item.selectionId ? "selected" : ""}`}
                    key={item.selectionId}
                    onClick={() => setSelected(item)}
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
                    <strong>{brlQuote(item.price, item.currency)}</strong>
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          {create.isError ? (
            <div className="error-state">{apiMessage(create.error)}</div>
          ) : null}
          <div className="form-actions">
            <Button type="button" variant="ghost" onClick={closeDialog}>
              Cancelar
            </Button>
            <Button
              type="submit"
              loading={create.isPending}
              disabled={!selected}
            >
              Cadastrar ativo
            </Button>
          </div>
        </form>
      </Dialog>

      <Dialog
        open={Boolean(removing)}
        onClose={() => {
          setRemoving(null);
          remove.reset();
        }}
        title={
          removing === "ALL" ? "Excluir todos os ativos?" : "Excluir ativo?"
        }
      >
        <p>
          {removing === "ALL" ? (
            <>
              Todo o catálogo será excluído. Se algum dos ativos tiver saldo
              positivo, nenhuma exclusão será realizada.
            </>
          ) : (
            <>
              O ativo <strong>{removing?.ticker}</strong> será removido do seu
              catálogo. Lançamentos históricos serão preservados.
            </>
          )}
        </p>
        <p className="muted">
          Ativos com saldo positivo em qualquer carteira não podem ser
          excluídos.
        </p>
        {remove.isError ? (
          <div className="error-state">{apiMessage(remove.error)}</div>
        ) : null}
        <div className="form-actions">
          <Button
            type="button"
            variant="ghost"
            onClick={() => setRemoving(null)}
          >
            Voltar
          </Button>
          <Button
            type="button"
            variant="danger"
            loading={remove.isPending}
            onClick={() => removing && remove.mutate(removing)}
          >
            Confirmar exclusão
          </Button>
        </div>
      </Dialog>
    </>
  );
}
