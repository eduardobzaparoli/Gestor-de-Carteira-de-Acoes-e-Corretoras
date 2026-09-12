import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowUpRight,
  BarChart3,
  BriefcaseBusiness,
  Pencil,
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
  const [editing, setEditing] = useState<Portfolio | null>(null);
  const [removing, setRemoving] = useState<Portfolio | null>(null);
  const [name, setName] = useState("");
  const [brokerageId, setBrokerageId] = useState("");
  const [editName, setEditName] = useState("");
  const [editBrokerageId, setEditBrokerageId] = useState("");
  const [editAttempted, setEditAttempted] = useState(false);
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
  const update = useMutation({
    mutationFn: (input: { id: string; name: string; brokerageId: string }) =>
      api<Portfolio>(`/api/portfolios/${input.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: input.name,
          brokerageId: input.brokerageId,
        }),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<Portfolio[]>(["portfolios"], (current) =>
        current?.map((item) => (item.id === updated.id ? updated : item)),
      );
      queryClient.setQueryData(["portfolio", updated.id], updated);
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio", updated.id] });
      setEditing(null);
      setEditAttempted(false);
      notify("Carteira atualizada.");
    },
  });
  const openCreate = () => {
    if (brokerages.data?.[0] && !brokerageId)
      setBrokerageId(brokerages.data[0].id);
    setCreateOpen(true);
  };
  const openEdit = (portfolio: Portfolio) => {
    update.reset();
    setEditName(portfolio.name);
    setEditBrokerageId(portfolio.brokerage.id);
    setEditAttempted(false);
    setEditing(portfolio);
  };
  const closeEdit = () => {
    if (update.isPending) return;
    setEditing(null);
    setEditAttempted(false);
    update.reset();
  };
  const editNameError =
    editAttempted && !editName.trim()
      ? "Informe o nome da carteira."
      : undefined;
  const editBrokerageError =
    editAttempted && !editBrokerageId ? "Selecione uma corretora." : undefined;
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
          <span className="eyebrow eyebrow--on-dark">
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
                <div className="portfolio-card__actions">
                  <button
                    onClick={() => openEdit(item)}
                    aria-label={`Editar ${item.name}`}
                  >
                    <Pencil size={17} />
                  </button>
                  <button
                    className="portfolio-card__delete"
                    onClick={() => setRemoving(item)}
                    aria-label={`Excluir ${item.name}`}
                  >
                    <Trash2 size={17} />
                  </button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
      {createOpen && (
        <Dialog open onClose={() => setCreateOpen(false)} title="Nova carteira">
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
      )}
      {editing && (
        <Dialog open onClose={closeEdit} title="Editar carteira">
          <form
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              setEditAttempted(true);
              if (!editing || !editName.trim() || !editBrokerageId) return;
              update.mutate({
                id: editing.id,
                name: editName,
                brokerageId: editBrokerageId,
              });
            }}
          >
            <div className="form-grid">
              <Field
                className="span-2"
                label="Nome da carteira"
                value={editName}
                onChange={(event) => setEditName(event.target.value)}
                error={editNameError}
                maxLength={100}
                required
                autoFocus
              />
              <SelectField
                className="span-2"
                label="Corretora"
                value={editBrokerageId}
                onChange={(event) => setEditBrokerageId(event.target.value)}
                error={editBrokerageError}
                required
            >
              <option value="">Selecione uma corretora</option>
              {editing &&
                !brokerages.data?.some(
                  (item) => item.id === editing.brokerage.id,
                ) && (
                  <option value={editing.brokerage.id}>
                    {editing.brokerage.nickname}
                  </option>
                )}
              {brokerages.data?.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nickname}
                  </option>
                ))}
              </SelectField>
            </div>
            {update.isError && (
              <div className="error-state" role="alert">
                {apiMessage(update.error)}
              </div>
            )}
            <div className="form-actions">
              <Button type="button" variant="ghost" onClick={closeEdit}>
                Cancelar
              </Button>
              <Button type="submit" loading={update.isPending}>
                Salvar alterações
              </Button>
            </div>
          </form>
        </Dialog>
      )}
      {removing && (
        <Dialog
          open
          onClose={() => setRemoving(null)}
          title="Excluir carteira?"
        >
          <p>
            Você está prestes a excluir <strong>{removing?.name}</strong> e todo
            o histórico associado. Essa ação não pode ser desfeita.
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
      )}
    </>
  );
}
