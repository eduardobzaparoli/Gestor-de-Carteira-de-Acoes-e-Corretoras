# Portfolio Market Valuation Specification

## Purpose

Permitir que o investidor acompanhe a valorização atual das posições abertas de uma carteira privada, com indicadores confiáveis e separados por moeda.

## Requirements

### Requirement: Consulta privada de valorização da carteira
O sistema SHALL disponibilizar `GET /api/portfolios/{portfolioId}/valuation` somente no contexto de uma carteira pertencente ao investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, e administradores MUST NOT consultar indicadores privados.

#### Scenario: Investidor consulta valorização de carteira própria
- **WHEN** um investidor autenticado consulta a valorização de uma de suas carteiras
- **THEN** o sistema responde com status `200` e os indicadores calculados somente para aquela carteira

#### Scenario: Carteira inexistente ou de outro investidor
- **WHEN** o investidor consulta a valorização de uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem consultar provedores de cotação

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador consulta a valorização
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não consulta provedores nem revela dados privados

### Requirement: Valorização das posições abertas pela cotação atual
O sistema SHALL derivar as posições a partir dos lançamentos efetivados e consultar a cotação mais recente disponível de cada posição aberta. Para cada posição, a resposta MUST conter ticker, nome, mercado, tipo, moeda, quantidade, preço médio, custo em custódia, preço atual, valor de mercado, ganho ou perda não realizado, rentabilidade percentual e percentual de alocação na moeda da posição. O preço atual e o valor de mercado MUST usar precisão decimal, sem arredondamento monetário prematuro.

#### Scenario: Posição aberta com cotação disponível
- **WHEN** a carteira possui uma posição aberta e a cotação mais recente do mesmo ticker e moeda está disponível
- **THEN** o sistema inclui a posição valorizada, com `valor de mercado = quantidade × preço atual`, `ganho ou perda não realizado = valor de mercado − custo em custódia` e `rentabilidade = ganho ou perda não realizado ÷ custo em custódia × 100`

#### Scenario: Carteira sem posições abertas
- **WHEN** a carteira não possui lançamentos efetivados ou todas as posições estão encerradas
- **THEN** o sistema responde com status `200`, sem posições, sem subtotais por moeda e sem consultar provedores de cotação

#### Scenario: Cotação ausente ou incompatível
- **WHEN** qualquer posição aberta não possui cotação válida, positiva e correspondente ao seu ticker e moeda
- **THEN** o sistema responde com status `503` e código `ASSET_QUOTE_UNAVAILABLE`, sem devolver posições ou subtotais parciais

### Requirement: Totais e alocação segregados por moeda
O sistema SHALL agrupar a valorização por moeda. Para cada moeda presente em posições abertas, o sistema MUST devolver valor investido, valor de mercado, ganho ou perda não realizado e rentabilidade percentual. O percentual de alocação de uma posição MUST ser calculado em relação ao valor de mercado total das posições com a mesma moeda. O sistema MUST preservar esses subtotais nativos sem converter seus valores. Além deles, a resposta MUST incluir um resumo consolidado em `BRL` quando houver posições abertas.

#### Scenario: Carteira possui ativos em BRL e USD
- **WHEN** a carteira possui posições abertas em reais e dólares
- **THEN** o sistema retorna subtotais independentes para `BRL` e `USD` e também retorna o resumo consolidado em `BRL`

#### Scenario: Alocação de posições na mesma moeda
- **WHEN** duas ou mais posições abertas possuem a mesma moeda
- **THEN** o percentual de alocação de cada uma é seu valor de mercado dividido pelo valor de mercado total daquela moeda, multiplicado por cem

#### Scenario: Posição é a única na moeda
- **WHEN** uma posição é a única posição aberta em sua moeda
- **THEN** seu percentual de alocação é `100`

### Requirement: Consulta resiliente e sem persistência própria
O sistema SHALL reutilizar temporariamente cotações equivalentes já obtidas para o mesmo mercado e ticker, respeitando a janela de cache configurada. O sistema SHALL reutilizar temporariamente uma taxa cambial equivalente para a mesma moeda de origem, moeda-base e data de referência, respeitando sua janela de cache configurada. A consulta MUST ser atômica para a resposta: se qualquer cotação de ativo ou conversão cambial necessária falhar por indisponibilidade ou limite do provedor, o sistema MUST responder `503` com o código público específico e não retornar totais parciais. O sistema MUST NOT persistir cotações, taxas cambiais, posições ou indicadores de valorização.

#### Scenario: Cotação reutilizada dentro da janela temporária
- **WHEN** a mesma carteira ou outra carteira solicita valorização de um ticker no mesmo mercado dentro da janela de cache configurada
- **THEN** o sistema reutiliza a cotação temporária disponível antes de chamar o provedor novamente

#### Scenario: Taxa cambial reutilizada dentro da janela temporária
- **WHEN** uma segunda consulta de valorização precisa converter `USD` para `BRL` com a mesma data de referência dentro da janela de cache configurada
- **THEN** o sistema reutiliza a taxa cambial temporária disponível antes de chamar o provedor novamente

#### Scenario: Provedor indisponível ou limitado durante a valorização
- **WHEN** a Brapi ou a AlphaVantage excede o tempo limite, falha tecnicamente ou informa limite de uso ao cotar qualquer posição aberta
- **THEN** o sistema responde com status `503` e o código público específico do provedor, sem expor credenciais, URLs internas, conteúdo bruto ou resultados parciais

#### Scenario: Provedor cambial indisponível durante a valorização
- **WHEN** a fonte cambial excede o tempo limite, falha tecnicamente ou não disponibiliza uma taxa PTAX de compra de fechamento válida
- **THEN** o sistema responde com status `503`, código `EXCHANGE_RATE_UNAVAILABLE`, sem expor detalhes internos e sem devolver resultados parciais

#### Scenario: Novo lançamento altera a base de cálculo
- **WHEN** uma compra ou venda efetivada altera o histórico da carteira
- **THEN** a próxima consulta recalcula a valorização a partir das posições abertas atualizadas, sem exigir persistência própria

#### Scenario: Banco suportado
- **WHEN** a aplicação usa H2 ou PostgreSQL
- **THEN** a consulta produz os mesmos indicadores para o mesmo histórico, as mesmas cotações e as mesmas taxas cambiais disponíveis

### Requirement: Patrimônio de mercado consolidado na moeda-base BRL
O sistema SHALL incluir, na consulta de valorização de uma carteira, um resumo consolidado de patrimônio de mercado na moeda-base `BRL`. O resumo MUST somar diretamente posições em `BRL` e converter o valor de mercado de posições em `USD` para `BRL` usando a cotação de compra do boletim `FECHAMENTO PTAX` mais recente disponível. O resumo MUST conter a moeda-base, o valor de mercado consolidado e, para cada moeda convertida, a moeda de origem, a taxa aplicada e a data de referência.

#### Scenario: Carteira com posições em BRL e USD
- **WHEN** uma carteira possui posições abertas em `BRL` e em `USD`
- **THEN** a resposta contém o patrimônio de mercado consolidado em `BRL`, composto pelo valor em reais mais o valor em dólares convertido pela PTAX de compra de fechamento mais recente disponível

#### Scenario: Carteira somente com posições em BRL
- **WHEN** uma carteira possui apenas posições abertas em `BRL`
- **THEN** o patrimônio consolidado em `BRL` é igual à soma dos valores de mercado em `BRL` e não há taxa cambial aplicada

#### Scenario: Carteira sem posições abertas
- **WHEN** uma carteira não possui posições abertas
- **THEN** a resposta permanece sem posições, sem subtotais por moeda e sem resumo consolidado, e não consulta o provedor cambial

### Requirement: Falha explícita da conversão cambial
O sistema SHALL responder com status `503` e código `EXCHANGE_RATE_UNAVAILABLE` quando uma posição aberta em moeda diferente de `BRL` não puder ser convertida por ausência de taxa de fechamento válida, moeda não suportada ou indisponibilidade técnica do provedor cambial. A resposta MUST NOT devolver patrimônio consolidado parcial.

#### Scenario: Cotação PTAX indisponível
- **WHEN** uma carteira contém posição em `USD` e a cotação PTAX de compra de fechamento mais recente disponível não pode ser obtida
- **THEN** o sistema responde com `503`, código `EXCHANGE_RATE_UNAVAILABLE` e não retorna indicadores parciais de valorização

#### Scenario: Moeda sem suporte de conversão
- **WHEN** uma carteira contém posição aberta em moeda diferente de `BRL` e `USD`
- **THEN** o sistema responde com `503`, código `EXCHANGE_RATE_UNAVAILABLE` e não retorna patrimônio consolidado parcial

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
