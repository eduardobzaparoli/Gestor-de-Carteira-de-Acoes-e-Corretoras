import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { Field } from "./ui";

function PasswordFields({ resetKey = 0 }: { resetKey?: number }) {
  const [values, setValues] = useState({ first: "", second: "" });
  return (
    <>
      <Field
        name="firstPassword"
        label="Primeira senha"
        type="password"
        value={values.first}
        visibilityResetKey={resetKey}
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            first: event.target.value,
          }))
        }
        autoComplete="current-password"
        hint="Ajuda do campo."
      />
      <Field
        name="secondPassword"
        label="Segunda senha"
        type="password"
        value={values.second}
        visibilityResetKey={resetKey}
        onChange={(event) =>
          setValues((current) => ({
            ...current,
            second: event.target.value,
          }))
        }
        autoComplete="new-password"
      />
    </>
  );
}

test("alterna cada senha sem alterar valor, foco ou atributos do campo", async () => {
  const user = userEvent.setup();
  render(<PasswordFields />);
  const first = screen.getByLabelText("Primeira senha");
  const second = screen.getByLabelText("Segunda senha");

  expect(first).toHaveAttribute("type", "password");
  expect(second).toHaveAttribute("type", "password");
  expect(first).toHaveAttribute("autocomplete", "current-password");
  expect(first).toHaveAccessibleDescription("Ajuda do campo.");

  await user.type(first, "segredo123");
  await user.click(screen.getAllByRole("button", { name: "Mostrar senha" })[0]);

  expect(first).toHaveAttribute("type", "text");
  expect(first).toHaveValue("segredo123");
  expect(first).toHaveFocus();
  expect(second).toHaveAttribute("type", "password");
  expect(screen.getByRole("button", { name: "Ocultar senha" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
});

test("permite ocultar por teclado e comunica o estado do controle", async () => {
  const user = userEvent.setup();
  render(<PasswordFields />);
  const first = screen.getByLabelText("Primeira senha");
  const show = screen.getAllByRole("button", { name: "Mostrar senha" })[0];

  show.focus();
  await user.keyboard("{Enter}");
  const hide = screen.getByRole("button", { name: "Ocultar senha" });
  expect(hide).toHaveFocus();
  expect(first).toHaveAttribute("type", "text");

  await user.keyboard(" ");
  expect(first).toHaveAttribute("type", "password");
  expect(
    screen.getAllByRole("button", { name: "Mostrar senha" })[0],
  ).toHaveAttribute("aria-pressed", "false");
});

test("volta ao estado oculto quando o formulário solicita reinicialização", async () => {
  const user = userEvent.setup();
  const { rerender } = render(<PasswordFields resetKey={0} />);
  const first = screen.getByLabelText("Primeira senha");

  await user.type(first, "segredo123");
  await user.click(screen.getAllByRole("button", { name: "Mostrar senha" })[0]);
  expect(first).toHaveAttribute("type", "text");

  rerender(<PasswordFields resetKey={1} />);

  expect(first).toHaveAttribute("type", "password");
  expect(first).toHaveValue("segredo123");
  expect(screen.getAllByRole("button", { name: "Mostrar senha" })).toHaveLength(
    2,
  );
});

test("não adiciona controle a campos comuns", () => {
  render(<Field label="Nome" value="Ana" readOnly />);

  expect(screen.getByLabelText("Nome")).toHaveProperty("type", "text");
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
});
