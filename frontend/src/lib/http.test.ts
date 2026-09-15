import { ApiError, apiMessage } from "./http";

test.each([
  ["CNPJ_NOT_ACTIVE_AT_CVM", "instituição ativa na CVM"],
  ["BROKERAGE_HAS_PORTFOLIOS", "vinculada a uma carteira"],
  ["TRANSACTION_CANNOT_BE_EDITED", "pendentes podem ser editados"],
  ["INSUFFICIENT_ASSET_QUANTITY", "supera o saldo disponível"],
])("traduz o erro %s para português", (code, expected) => {
  const error = new ApiError({
    status: 409,
    code,
    message: "Mensagem técnica em inglês",
    fieldErrors: [],
  });

  expect(apiMessage(error)).toContain(expected);
});

test("apresenta o campo inválido em português", () => {
  const error = new ApiError({
    status: 400,
    code: "VALIDATION_ERROR",
    message: "Request validation failed",
    fieldErrors: [
      {
        field: "unitPrice",
        message:
          "numeric value out of bounds (<11 digits>.<8 digits> expected)",
      },
    ],
  });

  expect(apiMessage(error)).toBe(
    "Revise o valor informado para preço unitário.",
  );
});
