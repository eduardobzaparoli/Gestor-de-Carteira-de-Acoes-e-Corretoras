## MODIFIED Requirements

### Requirement: Valorização das posições abertas pela cotação atual
O sistema SHALL derivar as posições a partir dos lançamentos efetivados e consultar a cotação mais recente disponível de cada posição aberta. Para cada posição, a resposta MUST conter ticker, nome, mercado, tipo, moeda, quantidade, preço médio, custo em custódia, preço atual, valor de mercado, ganho ou perda não realizado, rentabilidade percentual e percentual de alocação consolidado na moeda-base `BRL`. O preço atual e o valor de mercado MUST usar precisão decimal, sem arredondamento monetário prematuro.

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
O sistema SHALL manter valor investido, valor de mercado, ganho ou perda não realizado e rentabilidade em subtotais nativos separados por moeda. Para a composição da carteira, o sistema MUST converter o valor de mercado de cada posição para `BRL` com as mesmas taxas do resumo consolidado e calcular sua alocação contra o patrimônio consolidado positivo. Os percentuais públicos usados pela tabela e pelo gráfico MUST ser determinísticos, não negativos e fechar exatamente em `100%` após a precisão de apresentação; os subtotais nativos MUST permanecer inalterados.

#### Scenario: Carteira possui ativos em BRL e USD
- **WHEN** a carteira possui posições abertas em reais e dólares
- **THEN** o sistema retorna subtotais independentes para `BRL` e `USD`, resumo consolidado em `BRL` e uma única distribuição percentual calculada sobre os valores de mercado convertidos

#### Scenario: Alocação de posições na mesma moeda
- **WHEN** duas ou mais posições abertas possuem a mesma moeda
- **THEN** o percentual de cada posição corresponde ao seu valor de mercado em `BRL` dividido pelo patrimônio consolidado em `BRL`, multiplicado por cem

#### Scenario: Posição é a única na moeda
- **WHEN** uma posição é a única posição aberta em sua moeda, mas há posições abertas em outra moeda
- **THEN** sua alocação representa somente sua participação no patrimônio consolidado e não é artificialmente elevada para `100%`

#### Scenario: Única posição da carteira
- **WHEN** a carteira possui somente uma posição aberta com valor de mercado positivo
- **THEN** a alocação dessa posição é `100%`

#### Scenario: Fechamento dos percentuais apresentados
- **WHEN** divisões periódicas ou arredondamento na precisão pública produziriam diferença residual
- **THEN** o sistema distribui deterministicamente o resíduo sem alterar valores monetários e a soma dos percentuais retornados é exatamente `100%`

#### Scenario: Ordem não altera o resultado
- **WHEN** as mesmas posições são processadas em ordens diferentes
- **THEN** cada ticker recebe o mesmo percentual e o fechamento em `100%` permanece estável
