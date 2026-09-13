import { useState, type FormEvent } from "react";
import { KeyRound, Save, UserRound } from "lucide-react";
import { useAuth } from "../../app/AuthContext";
import { Button, Card, Field, PageHeading, notify } from "../../components/ui";
import { ApiError, apiMessage } from "../../lib/http";

type ProfileForm = {
  name: string;
  email: string;
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};
type ProfileErrors = Partial<Record<keyof ProfileForm, string>>;

const emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function ProfilePage() {
  const { user, updateProfile } = useAuth();
  const [form, setForm] = useState<ProfileForm>({
    name: user?.name ?? "",
    email: user?.email ?? "",
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [submitError, setSubmitError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);
  const [passwordVisibilityResetKey, setPasswordVisibilityResetKey] =
    useState(0);

  const update = (field: keyof ProfileForm, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setSubmitError(undefined);
  };

  const clearPasswords = () => {
    setForm((current) => ({
      ...current,
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    }));
    setPasswordVisibilityResetKey((current) => current + 1);
  };

  const validate = () => {
    const next: ProfileErrors = {};
    const name = form.name.trim();
    const email = form.email.trim();
    const changingPassword = Boolean(
      form.currentPassword || form.newPassword || form.confirmPassword,
    );
    if (!name) next.name = "Informe seu nome.";
    else if (name.length > 100)
      next.name = "O nome deve possuir no máximo 100 caracteres.";
    if (!email) next.email = "Informe seu e-mail.";
    else if (email.length > 254 || !emailPattern.test(email))
      next.email = "Informe um e-mail válido.";
    if (changingPassword) {
      if (!form.currentPassword)
        next.currentPassword = "Informe sua senha atual.";
      if (form.newPassword.length < 8 || form.newPassword.length > 72)
        next.newPassword = "A nova senha deve possuir entre 8 e 72 caracteres.";
      if (form.confirmPassword !== form.newPassword)
        next.confirmPassword = "A confirmação não corresponde à nova senha.";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitError(undefined);
    if (!validate()) {
      clearPasswords();
      return;
    }
    const changingPassword = Boolean(form.newPassword);
    setSubmitting(true);
    try {
      const updated = await updateProfile({
        name: form.name,
        email: form.email,
        ...(changingPassword
          ? {
              currentPassword: form.currentPassword,
              newPassword: form.newPassword,
            }
          : {}),
      });
      setForm((current) => ({
        ...current,
        name: updated.name,
        email: updated.email,
      }));
      notify("Perfil atualizado com sucesso.");
    } catch (error) {
      if (error instanceof ApiError) {
        const fieldErrors: ProfileErrors = {};
        for (const field of [
          "name",
          "email",
          "currentPassword",
          "newPassword",
        ] as const) {
          const message = error.field(field);
          if (message) fieldErrors[field] = message;
        }
        if (error.code === "EMAIL_ALREADY_REGISTERED")
          fieldErrors.email = "Este e-mail já está sendo utilizado.";
        setErrors(fieldErrors);
        setSubmitError(
          Object.keys(fieldErrors).length === 0 ? apiMessage(error) : undefined,
        );
      } else {
        setSubmitError(apiMessage(error));
      }
    } finally {
      clearPasswords();
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeading
        eyebrow="Minha conta"
        title="Perfil do investidor"
        description="Consulte e mantenha seus dados de acesso atualizados."
      />
      <Card className="profile-card">
        <div className="profile-card__heading">
          <span className="profile-card__icon">
            <UserRound aria-hidden="true" />
          </span>
          <div>
            <h2>Informações pessoais</h2>
            <p>
              Seu papel e suas informações financeiras não são alterados aqui.
            </p>
          </div>
        </div>
        <form className="profile-form" noValidate onSubmit={submit}>
          <div className="form-grid">
            <Field
              name="name"
              label="Nome"
              value={form.name}
              error={errors.name}
              onChange={(event) => update("name", event.target.value)}
              maxLength={100}
              autoComplete="name"
              required
            />
            <Field
              name="email"
              label="E-mail"
              type="email"
              value={form.email}
              error={errors.email}
              onChange={(event) => update("email", event.target.value)}
              maxLength={254}
              autoComplete="email"
              required
            />
          </div>
          <div className="profile-form__section">
            <div className="profile-form__section-title">
              <KeyRound size={19} aria-hidden="true" />
              <div>
                <h3>Alterar senha</h3>
                <p>Deixe os campos vazios para manter sua senha atual.</p>
              </div>
            </div>
            <div className="form-grid profile-password-grid">
              <Field
                name="currentPassword"
                label="Senha atual"
                type="password"
                visibilityResetKey={passwordVisibilityResetKey}
                value={form.currentPassword}
                error={errors.currentPassword}
                onChange={(event) =>
                  update("currentPassword", event.target.value)
                }
                maxLength={72}
                autoComplete="current-password"
              />
              <Field
                name="newPassword"
                label="Nova senha"
                type="password"
                visibilityResetKey={passwordVisibilityResetKey}
                value={form.newPassword}
                error={errors.newPassword}
                onChange={(event) => update("newPassword", event.target.value)}
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                hint="Entre 8 e 72 caracteres."
              />
              <Field
                name="confirmPassword"
                label="Confirmar nova senha"
                type="password"
                visibilityResetKey={passwordVisibilityResetKey}
                value={form.confirmPassword}
                error={errors.confirmPassword}
                onChange={(event) =>
                  update("confirmPassword", event.target.value)
                }
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
              />
            </div>
          </div>
          {submitError && (
            <div className="error-state profile-form__error" role="alert">
              {submitError}
            </div>
          )}
          <div className="form-actions">
            <Button type="submit" loading={submitting}>
              <Save size={17} />
              Salvar alterações
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
