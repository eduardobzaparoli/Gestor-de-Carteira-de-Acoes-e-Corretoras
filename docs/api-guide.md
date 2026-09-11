# Guia da API

Com a aplicação em `http://localhost:8080`, o contrato fica em `/v3/api-docs` e a interface navegável em `/swagger-ui.html`.

1. Cadastre uma conta em `POST /api/auth/register` ou autentique em `POST /api/auth/login`.
2. Envie o token nas rotas protegidas como `Authorization: Bearer <token>`.
3. Cadastre uma corretora, crie uma carteira, pesquise o ativo e use o `selectionId` retornado para registrar o lançamento.
4. Consulte posições, valorização, evolução e proventos pelos endpoints da carteira.

Erros usam JSON com `timestamp`, `status`, `code`, `message`, `path` e `fieldErrors`. O código público é a referência estável para tratamento pelo cliente; mensagens não devem ser usadas como identificadores.

O health check público está em `GET /actuator/health` e expõe somente o estado agregado.

## Refinamentos usados pelo frontend

- `GET /api/brokerages/cnpj?cnpj=<cnpj>` consulta os dados oficiais da empresa antes do cadastro e retorna `cnpj`, `legalName` e `tradeName`.
- `DELETE /api/brokerages/{id}` exclui uma corretora do investidor quando ela não está vinculada a nenhuma carteira. Uma corretora vinculada responde com `409` e código `BROKERAGE_HAS_PORTFOLIOS`.
- `PUT /api/portfolios/{portfolioId}/transactions/{transactionId}` atualiza tipo, data, quantidade, preço unitário e custos de um lançamento ainda pendente. Lançamentos efetivados ou cancelados respondem com `409` e código `TRANSACTION_CANNOT_BE_EDITED`.
- `GET /api/portfolios/{portfolioId}/exchange-rates?sourceCurrency=USD&date=aaaa-mm-dd` retorna a taxa da moeda informada para BRL na data solicitada, após validar que a carteira pertence ao investidor autenticado.

Datas enviadas à API permanecem no formato ISO `aaaa-mm-dd`; a conversão para `dd/mm/aaaa` é responsabilidade da interface. Valores decimais são enviados sem símbolo monetário e com ponto como separador decimal. A interface apresenta valores monetários em BRL; quando o ativo usa outra moeda, converte o valor somente para exibição e restaura a moeda nativa antes de enviar comandos financeiros à API.

## Provedores de dados de mercado

- Ativos brasileiros usam a Brapi para pesquisa, cotação e histórico.
- Ativos dos Estados Unidos usam a Twelve Data para pesquisa, cotação e histórico diário.
- Dividendos de ativos dos Estados Unidos continuam sendo consultados na Alpha Vantage.

As respostas públicas da API mantêm os mesmos contratos independentemente do provedor. Falhas da Twelve Data são normalizadas como `TWELVE_DATA_RATE_LIMITED` ou `TWELVE_DATA_PROVIDER_UNAVAILABLE`; detalhes técnicos e credenciais não são retornados ao cliente. Não existe fallback para a Alpha Vantage quando pesquisa, cotação ou histórico da Twelve Data falham.
