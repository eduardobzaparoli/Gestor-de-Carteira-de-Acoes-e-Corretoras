## ADDED Requirements

### Requirement: Indicadores consolidados com custo histórico em BRL
O sistema SHALL incluir no resumo consolidado em `BRL` da valorização de uma carteira o valor investido, o patrimônio de mercado, o ganho ou perda total e a rentabilidade percentual. O valor investido MUST ser calculado somente a partir de lançamentos efetivados, respeitando a ordem cronológica do histórico. Para compras em `BRL`, o sistema MUST usar o custo da compra acrescido dos custos adicionais. Para compras em `USD`, o sistema MUST converter esse custo pela cotação de compra do boletim `FECHAMENTO PTAX` aplicável à data do lançamento, usando o último fechamento anterior quando não houver fechamento naquela data. Uma venda MUST reduzir o valor investido pelo custo médio histórico em `BRL` das unidades vendidas, e não pelo valor recebido na venda. O ganho ou perda total MUST ser o patrimônio de mercado consolidado menos o valor investido consolidado, e a rentabilidade MUST ser esse ganho ou perda dividido pelo valor investido, multiplicado por cem. O sistema MUST manter precisão decimal e não arredondar monetariamente antes da resposta.

#### Scenario: Compra em USD compõe o valor investido histórico
- **WHEN** uma carteira possui uma compra efetivada em `USD` na data de uma PTAX de compra de fechamento disponível
- **THEN** o valor investido consolidado inclui o custo da compra e seus custos adicionais convertidos para `BRL` pela taxa aplicável àquela data

#### Scenario: Data sem fechamento cambial
- **WHEN** uma compra efetivada em `USD` ocorre em data sem fechamento PTAX
- **THEN** o sistema usa a PTAX de compra de fechamento válida mais recente anterior à data do lançamento e informa a taxa e a data de referência efetivamente utilizadas

#### Scenario: Venda reduz o custo histórico sem usar sua receita
- **WHEN** uma venda efetivada encerra parcialmente uma posição adquirida em `USD`
- **THEN** o valor investido consolidado é reduzido pelo custo médio histórico em `BRL` das unidades vendidas, independentemente do preço de venda

#### Scenario: Patrimônio e retorno consolidados
- **WHEN** uma carteira possui posições abertas em `BRL` ou `USD`
- **THEN** a resposta contém em seu resumo consolidado em `BRL` o valor investido, o patrimônio de mercado, o ganho ou perda total e a rentabilidade percentual calculados a partir dos custos históricos e da cotação atual aplicável

#### Scenario: Lançamentos pendentes e dividendos não alteram os indicadores
- **WHEN** uma carteira possui lançamentos pendentes ou eventos de dividendos ainda não representados por lançamento próprio
- **THEN** esses itens não alteram o valor investido, o patrimônio, o ganho ou perda nem a rentabilidade consolidados

### Requirement: Rastreabilidade e falha atômica da conversão histórica
O sistema SHALL expor, junto ao resumo consolidado, as taxas históricas de conversão efetivamente utilizadas para custos em `USD`, identificando ao menos a moeda de origem, a moeda-base, a taxa e sua data de referência. Caso uma taxa histórica necessária não possa ser obtida, seja inválida ou corresponda a moeda sem suporte, o sistema MUST responder com status `503` e código `EXCHANGE_RATE_UNAVAILABLE`, sem devolver valorização ou indicadores consolidados parciais. O sistema MUST reutilizar temporariamente taxas históricas equivalentes para a mesma moeda de origem, moeda-base e data de referência dentro da janela de cache configurada, sem persistir taxas.

#### Scenario: Taxa histórica necessária indisponível
- **WHEN** uma compra efetivada em `USD` exige uma PTAX histórica que não pode ser obtida
- **THEN** o sistema responde com `503`, código `EXCHANGE_RATE_UNAVAILABLE`, sem devolver resultados parciais

#### Scenario: Taxa histórica é reutilizada
- **WHEN** duas compras em `USD` exigem a mesma taxa histórica dentro da janela de cache
- **THEN** o sistema reutiliza a taxa temporária já obtida antes de consultar novamente o provedor
