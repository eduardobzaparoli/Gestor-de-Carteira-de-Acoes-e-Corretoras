## ADDED Requirements

### Requirement: Patrimônio de mercado consolidado na moeda-base BRL
O sistema SHALL incluir, na consulta de valorização de uma carteira, um resumo consolidado de patrimônio de mercado na moeda-base `BRL`. O resumo MUST somar diretamente posições em `BRL` e converter o valor de mercado de posições em `USD` para `BRL` usando a cotação de compra do boletim `FECHAMENTO PTAX` mais recente disponível. O resumo MUST conter a moeda-base, o valor de mercado consolidado e, para cada moeda convertida, a moeda de origem, a taxa aplicada e a data de referência. O sistema MUST NOT expor valor investido, ganho ou perda não realizado, rentabilidade ou alocação consolidados nesta etapa.

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

## MODIFIED Requirements

### Requirement: Totais e alocação segregados por moeda
O sistema SHALL agrupar a valorização por moeda. Para cada moeda presente em posições abertas, o sistema MUST devolver valor investido, valor de mercado, ganho ou perda não realizado e rentabilidade percentual. O percentual de alocação de uma posição MUST ser calculado em relação ao valor de mercado total das posições com a mesma moeda. O sistema MUST preservar esses subtotais nativos sem converter seus valores. Além deles, a resposta MUST incluir o resumo de patrimônio de mercado consolidado em `BRL` quando houver posições abertas.

#### Scenario: Carteira possui ativos em BRL e USD
- **WHEN** a carteira possui posições abertas em `BRL` e `USD`
- **THEN** o sistema retorna subtotais independentes para `BRL` e `USD` e também retorna o patrimônio de mercado consolidado em `BRL`, sem retornar valor investido, lucro ou rentabilidade consolidados

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
- **THEN** a consulta produz os mesmos indicadores para o mesmo histórico, as mesmas cotações e a mesma taxa cambial disponíveis
