import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, MapPin, Plus, Search, Trash2 } from "lucide-react";
import { useState } from "react";
import {
  Button,
  Card,
  Dialog,
  EmptyState,
  ErrorState,
  Field,
  PageHeading,
  Skeleton,
  notify,
} from "../../components/ui";
import { api, apiMessage } from "../../lib/http";
import { digits, maskCep, maskCnpj } from "../../lib/format";
import type {
  Brokerage,
  BrokerageInput,
  CepLookup,
  CnpjLookup,
} from "../../types/api";

const emptyForm: BrokerageInput = {
  nickname: "",
  cnpj: "",
  cep: "",
  street: "",
  neighborhood: "",
  number: "",
  complement: "",
};

export function BrokeragesPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [place, setPlace] = useState<CepLookup | null>(null);
  const [company, setCompany] = useState<CnpjLookup | null>(null);
  const [formError, setFormError] = useState("");
  const [removing, setRemoving] = useState<Brokerage | null>(null);
  const brokerages = useQuery({
    queryKey: ["brokerages"],
    queryFn: () => api<Brokerage[]>("/api/brokerages"),
  });
  const cep = useMutation({
    mutationFn: (value: string) =>
      api<CepLookup>(`/api/brokerages/cep/${digits(value)}`),
    onSuccess: (data) => {
      setPlace(data);
      setForm((current) => ({
        ...current,
        cep: data.cep,
        street: data.street,
        neighborhood: data.neighborhood,
      }));
    },
  });
  const cnpj = useMutation({
    mutationFn: (value: string) =>
      api<CnpjLookup>(`/api/brokerages/cnpj?cnpj=${encodeURIComponent(value)}`),
    onSuccess: setCompany,
  });
  const create = useMutation({
    mutationFn: (input: BrokerageInput) =>
      api<Brokerage>("/api/brokerages", {
        method: "POST",
        body: JSON.stringify({
          ...input,
          cnpj: digits(input.cnpj),
          cep: digits(input.cep),
        }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brokerages"] });
      setOpen(false);
      setForm(emptyForm);
      setPlace(null);
      setCompany(null);
      notify("Corretora cadastrada com sucesso.");
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) =>
      api<void>(`/api/brokerages/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["brokerages"] });
      setRemoving(null);
      notify("Corretora excluída.");
    },
  });
  const update = (field: keyof BrokerageInput, value: string) =>
    setForm((current) => ({ ...current, [field]: value }));
  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const missing = [
      [form.nickname, "Informe o apelido."],
      [digits(form.cnpj).length === 14, "Informe um CNPJ válido."],
      [digits(form.cep).length === 8, "Informe um CEP válido."],
      [form.street, "Informe a rua."],
      [form.neighborhood, "Informe o bairro."],
      [form.number, "Informe o número."],
    ].find(([value]) => !value);
    if (missing) {
      setFormError(String(missing[1]));
      return;
    }
    setFormError("");
    create.mutate(form);
  };
  const closeForm = () => {
    setOpen(false);
    setForm(emptyForm);
    setPlace(null);
    setCompany(null);
    cnpj.reset();
    cep.reset();
    create.reset();
    setFormError("");
  };
  return (
    <>
      <PageHeading
        eyebrow="Instituições"
        title="Suas corretoras"
        description="Conecte as instituições onde seus investimentos estão custodiados."
        actions={
          <Button onClick={() => setOpen(true)}>
            <Plus size={18} />
            Nova corretora
          </Button>
        }
      />
      {brokerages.isLoading ? (
        <div className="grid grid-3">
          <Skeleton height={220} />
          <Skeleton height={220} />
          <Skeleton height={220} />
        </div>
      ) : brokerages.isError ? (
        <ErrorState
          message={apiMessage(brokerages.error)}
          retry={() => brokerages.refetch()}
        />
      ) : !brokerages.data?.length ? (
        <Card>
          <EmptyState
            icon={<Building2 size={42} />}
            title="Cadastre sua primeira corretora"
            action={
              <Button onClick={() => setOpen(true)}>
                <Plus size={17} />
                Cadastrar corretora
              </Button>
            }
          >
            Ela será validada pelo CNPJ, endereço e cadastro na CVM antes de ser
            vinculada a uma carteira.
          </EmptyState>
        </Card>
      ) : (
        <div className="grid grid-3">
          {brokerages.data.map((item) => (
            <Card className="broker-card" key={item.id}>
              <BadgeCvm status={item.registrationStatus} />
              <h3>{item.nickname}</h3>
              <p className="muted">{item.legalName}</p>
              <div className="broker-meta">
                <div>
                  <span>CNPJ</span>
                  <strong>{maskCnpj(item.cnpj)}</strong>
                </div>
                <div>
                  <span>Categoria CVM</span>
                  <strong>
                    {item.cvmParticipantCategory || "Participante"}
                  </strong>
                </div>
                <div className="span-2">
                  <span>
                    <MapPin size={12} /> Endereço
                  </span>
                  <strong>
                    {item.address.street}, {item.address.number} ·{" "}
                    {item.address.city}/{item.address.state}
                  </strong>
                </div>
              </div>
              <div className="broker-card__actions">
                <Button variant="danger" onClick={() => setRemoving(item)}>
                  <Trash2 size={16} />
                  Excluir
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <Dialog open={open} onClose={closeForm} title="Cadastrar corretora">
        <form onSubmit={submit} noValidate>
          <div className="form-grid">
            <Field
              className="span-2"
              label="Apelido"
              value={form.nickname}
              onChange={(e) => update("nickname", e.target.value)}
              required
              placeholder="Ex.: Corretora principal"
            />
            <div
              className="span-2"
              style={{ display: "flex", gap: ".6rem", alignItems: "end" }}
            >
              <Field
                label="CNPJ"
                value={form.cnpj}
                onChange={(e) => {
                  update("cnpj", maskCnpj(e.target.value));
                  setCompany(null);
                  cnpj.reset();
                }}
                required
                placeholder="00.000.000/0000-00"
                style={{ flex: 1 }}
              />
              <Button
                type="button"
                variant="secondary"
                loading={cnpj.isPending}
                disabled={digits(form.cnpj).length !== 14}
                onClick={() => cnpj.mutate(form.cnpj)}
              >
                <Search size={17} />
                Consultar CNPJ
              </Button>
            </div>
            {cnpj.isError && (
              <div className="span-2 error-state">{apiMessage(cnpj.error)}</div>
            )}
            <Field
              className="span-2"
              label="Razão social"
              value={company?.legalName ?? ""}
              readOnly
              placeholder="Consulte o CNPJ para preencher"
            />
            <div
              className="span-2"
              style={{ display: "flex", gap: ".6rem", alignItems: "end" }}
            >
              <Field
                label="CEP"
                value={form.cep}
                onChange={(e) => update("cep", maskCep(e.target.value))}
                required
                placeholder="00000-000"
                style={{ flex: 1 }}
              />
              <Button
                type="button"
                variant="secondary"
                loading={cep.isPending}
                disabled={digits(form.cep).length !== 8}
                onClick={() => cep.mutate(form.cep)}
              >
                <Search size={17} />
                Consultar
              </Button>
            </div>
            {cep.isError && (
              <div className="span-2 error-state">{apiMessage(cep.error)}</div>
            )}
            <Field
              label="Rua"
              value={form.street}
              readOnly={Boolean(place)}
              onChange={(e) => update("street", e.target.value)}
              required
            />
            <Field
              label="Bairro"
              value={form.neighborhood}
              readOnly={Boolean(place)}
              onChange={(e) => update("neighborhood", e.target.value)}
              required
            />
            <Field
              label="Número"
              value={form.number}
              onChange={(e) => update("number", e.target.value)}
              required
            />
            <Field
              label="Complemento"
              value={form.complement}
              onChange={(e) => update("complement", e.target.value)}
            />
            {place && (
              <p className="span-2 muted">
                <MapPin size={14} /> {place.city}/{place.state} · endereço
                consultado via CEP
              </p>
            )}
          </div>
          {create.isError && (
            <div className="error-state">{apiMessage(create.error)}</div>
          )}
          {formError && <div className="error-state">{formError}</div>}
          <div className="form-actions">
            <Button type="button" variant="ghost" onClick={closeForm}>
              Cancelar
            </Button>
            <Button
              type="submit"
              loading={create.isPending}
              disabled={!place || !company}
            >
              Validar e cadastrar
            </Button>
          </div>
        </form>
      </Dialog>
      <Dialog
        open={Boolean(removing)}
        onClose={() => setRemoving(null)}
        title="Excluir corretora?"
      >
        <p>
          A corretora <strong>{removing?.nickname}</strong> será excluída. A
          ação só será concluída se ela não estiver vinculada a nenhuma
          carteira.
        </p>
        {remove.isError && (
          <div className="error-state">{apiMessage(remove.error)}</div>
        )}
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setRemoving(null)}>
            Manter corretora
          </Button>
          <Button
            variant="danger"
            loading={remove.isPending}
            onClick={() => removing && remove.mutate(removing.id)}
          >
            Excluir corretora
          </Button>
        </div>
      </Dialog>
    </>
  );
}

function BadgeCvm({ status }: { status: string }) {
  return (
    <span className="badge badge--success">
      {status || "Validada pela CVM"}
    </span>
  );
}
