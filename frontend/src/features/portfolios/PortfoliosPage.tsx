import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowUpRight,
  BarChart3,
  BriefcaseBusiness,
  Plus,
  Trash2,
} from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../app/AuthContext";
import {
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
import { dateTime, maskCnpj } from "../../lib/format";
import type { Brokerage, Portfolio } from "../../types/api";

export function PortfoliosPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [removing, setRemoving] = useState<Portfolio | null>(null);
  const [name, setName] = useState("");
  const [brokerageId, setBrokerageId] = useState("");
  const portfolios = useQuery({
    queryKey: ["portfolios"],
    queryFn: () => api<Portfolio[]>("/api/portfolios"),
  });
  const brokerages = useQuery({
    queryKey: ["brokerages"],
    queryFn: () => api<Brokerage[]>("/api/brokerages"),
  });
  const create = useMutation({
    mutationFn: () =>
      api<Portfolio>("/api/portfolios", {
        method: "POST",
        body: JSON.stringify({ name, brokerageId }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      setCreateOpen(false);
      setName("");
      notify("Carteira criada. Seu novo dashboard está pronto.");
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) =>
      api<void>(`/api/portfolios/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      setRemoving(null);
      notify("Carteira excluída.");
    },
  });
  const openCreate = () => {
    if (brokerages.data?.[0] && !brokerageId)
      setBrokerageId(brokerages.data[0].id);
    setCreateOpen(true);
  };
  return (
    <>
      <PageHeading
        eyebrow="Visão geral"
        title={`Olá, ${user?.name.split(" ")[0]}`}
        description="Acompanhe suas estratégias e abra uma carteira para explorar todos os indicadores."
        actions={
          <Button onClick={openCreate}>
            <Plus size={18} />
            Nova carteira
          </Button>
        }
      />
      <div className="summary-strip">
        <div>
          <span className="eyebrow" style={{ color: "#79ddbc" }}>
            Central de investimentos
          </span>
          <h2 style={{ margin: ".35rem 0 0" }}>
            {portfolios.data?.length ?? 0}{" "}
            {portfolios.data?.length === 1
              ? "carteira monitorada"
              : "carteiras monitoradas"}
          </h2>
          <p>Patrimônio organizado por estratégia e corretora.</p>
        </div>
        <div className="summary-strip__visual" aria-hidden>
          {[22, 35, 29, 46, 40, 55].map((height, index) => (
            <i key={index} style={{ height }} />
          ))}
        </div>
      </div>
      {portfolios.isLoading ? (
        <div className="grid grid-3">
          <Skeleton height={210} />
          <Skeleton height={210} />
          <Skeleton height={210} />
        </div>
      ) : portfolios.isError ? (
        <ErrorState
          message={apiMessage(portfolios.error)}
          retry={() => portfolios.refetch()}
        />
      ) : !portfolios.data?.length ? (
        <Card>
          <EmptyState
            icon={<BriefcaseBusiness size={44} />}
            title="Sua jornada começa com uma carteira"
            action={
              <Button onClick={openCreate}>
                <Plus size={17} />
                Criar primeira carteira
              </Button>
            }
          >
            Agrupe seus ativos por objetivo e acompanhe evolução, composição,
            lançamentos e proventos.
          </EmptyState>
        </Card>
      ) : (
        <div className="grid grid-3">
          {portfolios.data.map((item) => (
            <Card className="portfolio-card" key={item.id}>
              <span className="eyebrow">Carteira</span>
              <h2>{item.name}</h2>
              <span className="portfolio-card__broker">
                {item.brokerage.nickname}
              </span>
              <p className="muted" style={{ fontSize: ".78rem" }}>
                {maskCnpj(item.brokerage.cnpj)} · Atualizada{" "}
                {dateTime(item.updatedAt)}
              </p>
              <div className="portfolio-card__footer">
                <Link to={`/app/carteiras/${item.id}`}>
                  Ver dashboard <ArrowUpRight size={15} />
                </Link>
                <button
                  onClick={() => setRemoving(item)}
                  aria-label={`Excluir ${item.name}`}
                >
                  <Trash2 size={17} />
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Nova carteira"
      >
        {!brokerages.data?.length ? (
          <EmptyState
            icon={<BarChart3 />}
            title="Cadastre uma corretora primeiro"
            action={
              <Link className="button button--primary" to="/app/corretoras">
                Ir para corretoras
              </Link>
            }
          >
            Toda carteira precisa estar vinculada a uma instituição validada.
          </EmptyState>
        ) : (
          <form
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              create.mutate();
            }}
          >
            <div className="form-grid">
              <Field
                className="span-2"
                label="Nome da carteira"
                value={name}
                onChange={(e) => setName(e.target.value)}
                maxLength={100}
                required
                placeholder="Ex.: Longo prazo"
              />
              <SelectField
                className="span-2"
                label="Corretora"
                value={brokerageId}
                onChange={(e) => setBrokerageId(e.target.value)}
                required
              >
                {brokerages.data.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nickname}
                  </option>
                ))}
              </SelectField>
            </div>
            {create.isError && (
              <div className="error-state">{apiMessage(create.error)}</div>
            )}
            <div className="form-actions">
              <Button
                type="button"
                variant="ghost"
                onClick={() => setCreateOpen(false)}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                loading={create.isPending}
                disabled={!name.trim() || !brokerageId}
              >
                Criar carteira
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Dialog
        open={Boolean(removing)}
        onClose={() => setRemoving(null)}
        title="Excluir carteira?"
      >
        <p>
          Você está prestes a excluir <strong>{removing?.name}</strong> e todo o
          histórico associado. Essa ação não pode ser desfeita.
        </p>
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setRemoving(null)}>
            Manter carteira
          </Button>
          <Button
            variant="danger"
            loading={remove.isPending}
            onClick={() => removing && remove.mutate(removing.id)}
          >
            Excluir definitivamente
          </Button>
        </div>
      </Dialog>
    </>
  );
}
