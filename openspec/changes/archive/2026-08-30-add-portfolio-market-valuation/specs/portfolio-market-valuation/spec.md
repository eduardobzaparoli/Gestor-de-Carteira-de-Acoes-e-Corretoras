## Purpose

Permitir que o investidor acompanhe a valorização atual das posições abertas de uma carteira privada, com indicadores confiáveis e separados por moeda.

## ADDED Requirements

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
O sistema SHALL agrupar a valorização por moeda. Para cada moeda presente em posições abertas, o sistema MUST devolver valor investido, valor de mercado, ganho ou perda não realizado e rentabilidade percentual. O percentual de alocação de uma posição MUST ser calculado em relação ao valor de mercado total das posições com a mesma moeda. O sistema MUST NOT somar ou converter valores de moedas diferentes.

#### Scenario: Carteira possui ativos em BRL e USD
- **WHEN** a carteira possui posições abertas em reais e dólares
- **THEN** o sistema retorna subtotais independentes para `BRL` e `USD`, e não retorna patrimônio total consolidado

#### Scenario: Alocação de posições na mesma moeda
- **WHEN** duas ou mais posições abertas possuem a mesma moeda
- **THEN** o percentual de alocação de cada uma é seu valor de mercado dividido pelo valor de mercado total daquela moeda, multiplicado por cem

#### Scenario: Posição é a única na moeda
- **WHEN** uma posição é a única posição aberta em sua moeda
- **THEN** seu percentual de alocação é `100`

### Requirement: Consulta resiliente e sem persistência própria
O sistema SHALL reutilizar temporariamente cotações equivalentes já obtidas para o mesmo mercado e ticker, respeitando a janela de cache configurada. A consulta MUST ser atômica para a resposta: se qualquer cotação necessária falhar por indisponibilidade ou limite do provedor, o sistema MUST responder `503` com o código público específico já definido para o provedor e não retornar totais parciais. O sistema MUST NOT persistir cotações, posições ou indicadores de valorização.

#### Scenario: Cotação reutilizada dentro da janela temporária
- **WHEN** a mesma carteira ou outra carteira solicita valorização de um ticker no mesmo mercado dentro da janela de cache configurada
- **THEN** o sistema reutiliza a cotação temporária disponível antes de chamar o provedor novamente

#### Scenario: Provedor indisponível ou limitado durante a valorização
- **WHEN** a Brapi ou a AlphaVantage excede o tempo limite, falha tecnicamente ou informa limite de uso ao cotar qualquer posição aberta
- **THEN** o sistema responde com status `503` e o código público específico do provedor, sem expor credenciais, URLs internas, conteúdo bruto ou resultados parciais

#### Scenario: Novo lançamento altera a base de cálculo
- **WHEN** uma compra ou venda efetivada altera o histórico da carteira
- **THEN** a próxima consulta recalcula a valorização a partir das posições abertas atualizadas, sem exigir persistência própria

#### Scenario: Banco suportado
- **WHEN** a aplicação usa H2 ou PostgreSQL
- **THEN** a consulta produz os mesmos indicadores para o mesmo histórico e as mesmas cotações disponíveis
