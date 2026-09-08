import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, ShieldCheck, UserCheck, Users } from "lucide-react";
import { useMemo, useState } from "react";
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
import { dateTime, label } from "../../lib/format";
import type { AdminUser, UserRole } from "../../types/api";

type UserForm = {
  name: string;
  email: string;
  password: string;
  role: UserRole;
};
const emptyForm: UserForm = {
  name: "",
  email: "",
  password: "",
  role: "INVESTOR",
};

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [editing, setEditing] = useState<AdminUser | null | undefined>(
    undefined,
  );
  const [form, setForm] = useState<UserForm>(emptyForm);
  const [action, setAction] = useState<{
    user: AdminUser;
    kind: "deactivate" | "reactivate";
  } | null>(null);
  const users = useQuery({
    queryKey: ["admin-users"],
    queryFn: () => api<AdminUser[]>("/api/admin/users"),
  });
  const save = useMutation({
    mutationFn: () =>
      api<AdminUser>(
        editing ? `/api/admin/users/${editing.id}` : "/api/admin/users",
        { method: editing ? "PUT" : "POST", body: JSON.stringify(form) },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      setEditing(undefined);
      notify(editing ? "Usuário atualizado." : "Usuário criado.");
    },
  });
  const changeStatus = useMutation({
    mutationFn: ({
      user,
      kind,
    }: {
      user: AdminUser;
      kind: "deactivate" | "reactivate";
    }) =>
      api<void | AdminUser>(
        kind === "deactivate"
          ? `/api/admin/users/${user.id}`
          : `/api/admin/users/${user.id}/reactivate`,
        { method: kind === "deactivate" ? "DELETE" : "POST" },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      setAction(null);
      notify("Estado da conta atualizado.");
    },
  });
  const rows = useMemo(
    () =>
      users.data?.filter(
        (user) =>
          (!search ||
            `${user.name} ${user.email}`
              .toLowerCase()
              .includes(search.toLowerCase())) &&
          (!role || user.role === role) &&
          (!status || user.status === status),
      ) ?? [],
    [users.data, search, role, status],
  );
  const counts = {
    total: users.data?.length ?? 0,
    active: users.data?.filter((u) => u.status === "ACTIVE").length ?? 0,
    admins:
      users.data?.filter((u) => u.role === "ADMIN" && u.status === "ACTIVE")
        .length ?? 0,
  };
  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
  };
  const openEdit = (user: AdminUser) => {
    setEditing(user);
    setForm({
      name: user.name,
      email: user.email,
      password: "",
      role: user.role,
    });
  };
  return (
    <>
      <PageHeading
        eyebrow="Administração"
        title="Gestão de usuários"
        description="Gerencie acessos e papéis sem visualizar informações financeiras privadas."
        actions={
          <Button onClick={openCreate}>
            <Plus size={17} />
            Novo usuário
          </Button>
        }
      />
      <div className="grid grid-3">
        <AdminMetric
          label="Contas cadastradas"
          value={counts.total}
          icon={<Users />}
        />
        <AdminMetric
          label="Contas ativas"
          value={counts.active}
          icon={<UserCheck />}
        />
        <AdminMetric
          label="Administradores ativos"
          value={counts.admins}
          icon={<ShieldCheck />}
        />
      </div>
      <Card>
        <div className="card-header">
          <div>
            <h2>Usuários</h2>
            <p>Papéis e estados atuais das contas.</p>
          </div>
        </div>
        <div className="filter-bar">
          <input
            aria-label="Buscar usuário"
            placeholder="Buscar nome ou e-mail"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select
            aria-label="Filtrar papel"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="">Todos os papéis</option>
            <option value="INVESTOR">Investidor</option>
            <option value="ADMIN">Administrador</option>
          </select>
          <select
            aria-label="Filtrar estado"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">Todos os estados</option>
            <option value="ACTIVE">Ativos</option>
            <option value="INACTIVE">Inativos</option>
          </select>
        </div>
        {users.isLoading ? (
          <Skeleton height={320} />
        ) : users.isError ? (
          <ErrorState
            message={apiMessage(users.error)}
            retry={() => users.refetch()}
          />
        ) : !rows.length ? (
          <EmptyState icon={<Search />} title="Nenhum usuário encontrado">
            Revise os filtros ou cadastre uma nova conta.
          </EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="data-table mobile-cards">
              <thead>
                <tr>
                  <th>Usuário</th>
                  <th>Papel</th>
                  <th>Estado</th>
                  <th>Criado em</th>
                  <th>Atualizado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((user) => (
                  <tr key={user.id}>
                    <td data-label="Usuário">
                      <div>
                        <strong>{user.name}</strong>
                        <div className="muted">{user.email}</div>
                      </div>
                    </td>
                    <td data-label="Papel">
                      <Badge tone={user.role === "ADMIN" ? "info" : "neutral"}>
                        {label(user.role)}
                      </Badge>
                    </td>
                    <td data-label="Estado">
                      <Badge
                        tone={user.status === "ACTIVE" ? "success" : "danger"}
                      >
                        {label(user.status)}
                      </Badge>
                    </td>
                    <td data-label="Criado">{dateTime(user.createdAt)}</td>
                    <td data-label="Atualizado">{dateTime(user.updatedAt)}</td>
                    <td data-label="Ações">
                      <div style={{ display: "flex", gap: ".35rem" }}>
                        <Button
                          variant="secondary"
                          onClick={() => openEdit(user)}
                        >
                          Editar
                        </Button>
                        <Button
                          variant={
                            user.status === "ACTIVE" ? "danger" : "secondary"
                          }
                          onClick={() =>
                            setAction({
                              user,
                              kind:
                                user.status === "ACTIVE"
                                  ? "deactivate"
                                  : "reactivate",
                            })
                          }
                        >
                          {user.status === "ACTIVE" ? "Desativar" : "Reativar"}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      <Dialog
        open={editing !== undefined}
        onClose={() => setEditing(undefined)}
        title={editing ? "Editar usuário" : "Novo usuário"}
      >
        <form
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            save.mutate();
          }}
        >
          <div className="form-grid">
            <Field
              label="Nome"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              maxLength={100}
              required
            />
            <Field
              label="E-mail"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              maxLength={254}
              required
            />
            <Field
              label="Senha"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              minLength={8}
              maxLength={72}
              required
              hint={
                editing
                  ? "Informe a senha que ficará ativa após a edição."
                  : "Entre 8 e 72 caracteres."
              }
            />
            <SelectField
              label="Papel"
              value={form.role}
              onChange={(e) =>
                setForm({ ...form, role: e.target.value as UserRole })
              }
            >
              <option value="INVESTOR">Investidor</option>
              <option value="ADMIN">Administrador</option>
            </SelectField>
          </div>
          {save.isError && (
            <div className="error-state">{apiMessage(save.error)}</div>
          )}
          <div className="form-actions">
            <Button
              type="button"
              variant="ghost"
              onClick={() => setEditing(undefined)}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              loading={save.isPending}
              disabled={!form.name || !form.email || form.password.length < 8}
            >
              {editing ? "Salvar alterações" : "Criar usuário"}
            </Button>
          </div>
        </form>
      </Dialog>
      <Dialog
        open={Boolean(action)}
        onClose={() => setAction(null)}
        title={
          action?.kind === "deactivate"
            ? "Desativar usuário?"
            : "Reativar usuário?"
        }
      >
        <p>
          {action?.kind === "deactivate" ? (
            <>
              O acesso de <strong>{action.user.name}</strong> será bloqueado
              imediatamente, inclusive para tokens já emitidos.
            </>
          ) : (
            <>
              O acesso de <strong>{action?.user.name}</strong> será restaurado.
            </>
          )}
        </p>
        {changeStatus.isError && (
          <div className="error-state">{apiMessage(changeStatus.error)}</div>
        )}
        <div className="form-actions">
          <Button variant="ghost" onClick={() => setAction(null)}>
            Cancelar
          </Button>
          <Button
            variant={action?.kind === "deactivate" ? "danger" : "primary"}
            loading={changeStatus.isPending}
            onClick={() => action && changeStatus.mutate(action)}
          >
            Confirmar
          </Button>
        </div>
      </Dialog>
    </>
  );
}

function AdminMetric({
  label: caption,
  value,
  icon,
}: {
  label: string;
  value: number;
  icon: React.ReactNode;
}) {
  return (
    <Card className="kpi">
      <div className="kpi__top">
        <span>{caption}</span>
        <span className="kpi__icon">{icon}</span>
      </div>
      <div className="kpi__value">{value}</div>
      <div className="kpi__detail">Atualizado nesta consulta</div>
    </Card>
  );
}
