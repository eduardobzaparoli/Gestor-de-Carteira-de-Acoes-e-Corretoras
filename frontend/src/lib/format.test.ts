import {
  decimalInput,
  decimalForApi,
  completePtBrDate,
  convertedAmount,
  formattedMoneyInput,
  isoToPtBr,
  label,
  maskCep,
  maskCnpj,
  money,
  percent,
  ptBrToIso,
  nativeAmount,
} from "./format";
test("formata valores financeiros e identificadores em pt-BR", () => {
  expect(money(1234.5, "BRL")).toContain("1.234,50");
  expect(percent(12.34)).toBe("12,34%");
  expect(maskCep("12345678")).toBe("12345-678");
  expect(maskCnpj("61384004000105")).toBe("61.384.004/0001-05");
  expect(label("BUY")).toBe("Compra");
});

test("converte datas brasileiras sem aceitar datas inexistentes", () => {
  expect(isoToPtBr("2026-09-10")).toBe("10/09/2026");
  expect(ptBrToIso("10/09/2026")).toBe("2026-09-10");
  expect(ptBrToIso("31/02/2026")).toBeNull();
  expect(completePtBrDate("10/09", 2026)).toBe("10/09/2026");
});

test("converte valores apenas na borda de apresentação", () => {
  expect(convertedAmount("10", "5.25")).toBe(52.5);
  expect(nativeAmount("52.5", "5.25")).toBe(10);
  expect(decimalForApi(nativeAmount("1620.53", "5.12"))).toBe("316.50976563");
});

test("normaliza e formata valores monetários digitados", () => {
  expect(decimalInput("R$ 1.234,56")).toBe("1234.56");
  expect(formattedMoneyInput("25,5", "BRL")).toContain("25,50");
});
