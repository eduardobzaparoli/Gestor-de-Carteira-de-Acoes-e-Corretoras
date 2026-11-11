import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  KeyRound,
  Plus,
  Search,
  ShieldCheck,
  UserCheck,
  Users,
} from "lucide-react";
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
  const [passwordVisibilityResetKey, setPasswordVisibilityResetKey] =
    useState(0);
  const [passwordTarget, setPasswordTarget] = useState<AdminUser | null>(null);
  const [newPassword, setNewPassword] = useState("");
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
        {
          method: editing ? "PUT" : "POST",
          body: JSON.stringify(
            editing
              ? { name: form.name, email: form.email, role: form.role }
              : form,
          ),
        },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      setEditing(undefined);
      setForm(emptyForm);
      notify(editing ? "Usuário atualizado." : "Usuário criado.");
    },
  });
  const resetPassword = useMutation({
    mutationFn: () =>
      api<void>(`/api/admin/users/${passwordTarget?.id}/password`, {
        method: "PUT",
        body: JSON.stringify({ password: newPassword }),
      }),
    onSuccess: () => {
      setPasswordTarget(null);
      setNewPassword("");
      notify("Senha redefinida.");
    },
    onError: () => {
      setNewPassword("");
      setPasswordVisibilityResetKey((current) => current + 1);
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
    setPasswordVisibilityResetKey((current) => current + 1);
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
  const closeEditor = () => {
    setEditing(undefined);
    setForm(emptyForm);
    save.reset();
  };
  const openPasswordReset = (user: AdminUser) => {
    setPasswordVisibilityResetKey((current) => current + 1);
    setPasswordTarget(user);
    setNewPassword("");
    resetPassword.reset();
  };
  const closePasswordReset = () => {
    setPasswordTarget(null);
    setNewPassword("");
    resetPassword.reset();
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
      <div className="grid grid-3 admin-users-metrics">
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
      <Card className="admin-users-list">
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
                          variant="secondary"
                          onClick={() => openPasswordReset(user)}
                        >
                          <KeyRound size={15} />
                          Senha
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
        onClose={closeEditor}
        title={editing ? "Editar usuário" : "Novo usuário"}
      >
        <form
          className="admin-user-form"
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
            {!editing && (
              <Field
                label="Senha"
                type="password"
                visibilityResetKey={passwordVisibilityResetKey}
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                minLength={8}
                maxLength={72}
                required
                hint="Entre 8 e 72 caracteres."
              />
            )}
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
            <Button type="button" variant="ghost" onClick={closeEditor}>
              Cancelar
            </Button>
            <Button
              type="submit"
              loading={save.isPending}
              disabled={
                !form.name ||
                !form.email ||
                (!editing && form.password.length < 8)
              }
            >
              {editing ? "Salvar alterações" : "Criar usuário"}
            </Button>
          </div>
        </form>
      </Dialog>
      <Dialog
        open={Boolean(passwordTarget)}
        onClose={closePasswordReset}
        title="Redefinir senha"
      >
        <form
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            resetPassword.mutate();
          }}
        >
          <p>
            Defina uma nova senha para <strong>{passwordTarget?.name}</strong>.
          </p>
          <Field
            label="Nova senha"
            type="password"
            visibilityResetKey={passwordVisibilityResetKey}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            minLength={8}
            maxLength={72}
            required
            hint="Entre 8 e 72 caracteres."
          />
          {resetPassword.isError && (
            <div className="error-state">{apiMessage(resetPassword.error)}</div>
          )}
          <div className="form-actions">
            <Button type="button" variant="ghost" onClick={closePasswordReset}>
              Cancelar
            </Button>
            <Button
              type="submit"
              loading={resetPassword.isPending}
              disabled={newPassword.length < 8}
            >
              Redefinir senha
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
