## Purpose

Permitir que o investidor acompanhe, em uma série diária consolidada em BRL, como o custo das posições em custódia e seu valor de mercado evoluíram nos últimos 90 dias.

## ADDED Requirements

### Requirement: Acesso privado à evolução da carteira
O sistema SHALL disponibilizar a evolução patrimonial somente para uma carteira pertencente ao investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, administradores MUST NOT acessar a série privada e todas as respostas MUST ser JSON.

#### Scenario: Investidor consulta carteira própria
- **WHEN** um investidor autenticado solicita a evolução de uma de suas carteiras
- **THEN** o sistema responde com a série calculada exclusivamente para essa carteira

#### Scenario: Carteira inexistente ou de outro investidor
- **WHEN** o investidor solicita uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com `404` e código `PORTFOLIO_NOT_FOUND` sem consultar provedores nem revelar a existência da carteira

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador solicita a evolução privada
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON

### Requirement: Janela e representação diária determinísticas
O sistema SHALL produzir pontos em ordem cronológica crescente para a janela inclusiva formada pela data atual e pelos 89 dias de calendário anteriores. Cada ponto MUST conter a data de referência, `investedValue` e `marketValue`, ambos consolidados em `BRL`, com precisão decimal e sem arredondamento monetário prematuro. A série histórica MUST usar as datas de negociação retornadas pelos mercados e MUST incluir um ponto da data atual quando houver posição em custódia nessa data.

#### Scenario: Carteira com histórico dentro da janela
- **WHEN** a carteira possui posição em custódia em dias da janela de 90 dias
- **THEN** o sistema retorna pontos crescentes somente a partir das datas em que existe base de preço suficiente para valorar a custódia

#### Scenario: Mercados possuem calendários diferentes
- **WHEN** um mercado possui fechamento em uma data e outro está fechado
- **THEN** o sistema reutiliza para o mercado fechado seu último fechamento anterior disponível e produz um único ponto consolidado para a data

#### Scenario: Carteira sem custódia na janela
- **WHEN** nenhum ativo esteve em custódia durante a janela
- **THEN** o sistema responde com `200` e coleção de pontos vazia sem consultar preços nem câmbio

### Requirement: Valor investido histórico da custódia
O sistema SHALL calcular `investedValue` em cada data reproduzindo em ordem cronológica somente compras e vendas `EFFECTIVE` ocorridas até aquela data. Compras MUST aumentar o custo da posição pela quantidade multiplicada pelo preço unitário acrescida dos custos; compras em `USD` MUST ser convertidas para `BRL` pela PTAX histórica da data do lançamento. Vendas MUST reduzir o custo pelo custo médio histórico em `BRL` das unidades vendidas, sem usar a receita da venda. Uma posição totalmente liquidada MUST ter custo zero, e uma compra posterior MUST iniciar uma nova base de custo.

#### Scenario: Compra aumenta o valor investido
- **WHEN** uma compra efetivada passa a fazer parte do histórico em uma data da série
- **THEN** o `investedValue` daquele ponto e dos seguintes inclui o custo histórico dessa compra enquanto suas unidades permanecerem em custódia

#### Scenario: Venda parcial reduz pelo custo médio
- **WHEN** uma venda parcial efetivada ocorre em uma data da série
- **THEN** o `investedValue` é reduzido pelo custo médio histórico das unidades vendidas, independentemente do preço recebido

#### Scenario: Liquidação e reabertura
- **WHEN** uma posição é totalmente vendida e posteriormente recomprada
- **THEN** o custo chega a zero após a liquidação e a recompra inicia uma nova base de custo

### Requirement: Patrimônio histórico pelos fechamentos de mercado
O sistema SHALL calcular `marketValue` em cada data multiplicando a quantidade então em custódia pelo fechamento histórico não ajustado de cada ativo. Valores em `BRL` MUST ser somados diretamente e valores em `USD` MUST ser convertidos pela PTAX histórica aplicável à data do ponto, usando o último fechamento anterior quando necessário. O ponto da data atual MUST usar as cotações atuais e o câmbio atual já adotados pela valorização da carteira.

#### Scenario: Posição brasileira em data histórica
- **WHEN** uma posição `BR` está em custódia em uma data com fechamento disponível
- **THEN** seu patrimônio é a quantidade daquela data multiplicada pelo fechamento não ajustado em `BRL`

#### Scenario: Posição americana em data histórica
- **WHEN** uma posição `US` está em custódia em uma data da série
- **THEN** seu valor em `USD` é convertido para `BRL` pela PTAX histórica aplicável àquela data

#### Scenario: Ponto atual
- **WHEN** a janela alcança a data atual e a carteira possui posição aberta
- **THEN** o último ponto usa as cotações e o câmbio atuais em vez de tratar um fechamento histórico como preço corrente

### Requirement: Evolução restrita aos ativos em custódia
O sistema MUST representar somente o custo e o valor de mercado dos ativos em custódia em cada data. Receita de vendas MUST NOT permanecer como caixa, proventos MUST NOT aumentar o patrimônio, e lançamentos `PENDING` ou `CANCELLED` MUST NOT alterar nenhum ponto. Eventos que modifiquem quantidade fora do log de compras e vendas, como desdobramentos, grupamentos e bonificações, MUST permanecer fora deste cálculo.

#### Scenario: Venda total não mantém caixa
- **WHEN** todas as unidades de uma posição são vendidas
- **THEN** os pontos seguintes não mantêm a receita da venda em `investedValue` nem em `marketValue`

#### Scenario: Proventos e lançamentos não efetivos
- **WHEN** a carteira possui proventos ou lançamentos pendentes ou cancelados
- **THEN** esses registros não alteram a evolução do valor investido nem do patrimônio

### Requirement: Consulta histórica resiliente e atômica
O sistema SHALL consultar séries históricas na Brapi para ativos `BR` e na Alpha Vantage para ativos `US`, respeitando credenciais, limites e cobertura disponíveis. O sistema MUST reutilizar temporariamente séries equivalentes em cache e MUST NOT persistir preços, taxas cambiais, posições ou pontos da evolução. Se faltar preço ou câmbio necessário para qualquer posição em custódia, a operação MUST responder com `503` e código público correspondente, sem retornar série parcial nem detalhes internos do provedor.

#### Scenario: Série histórica reutilizada
- **WHEN** uma nova consulta exige a mesma série de mercado ainda válida no cache
- **THEN** o sistema reutiliza os dados sem repetir a chamada externa equivalente

#### Scenario: Cobertura histórica insuficiente
- **WHEN** o provedor não entrega preço suficiente para valorar uma posição existente na janela
- **THEN** o sistema responde com `503` e erro público de dados históricos indisponíveis sem fabricar ou truncar silenciosamente a série

#### Scenario: Câmbio histórico indisponível
- **WHEN** falta PTAX necessária para consolidar uma posição `US`
- **THEN** o sistema responde com `503` e código `EXCHANGE_RATE_UNAVAILABLE` sem pontos parciais

### Requirement: Compatibilidade dos bancos suportados
O sistema MUST manter comportamento equivalente com H2 e PostgreSQL e MUST calcular a evolução sob demanda sem criar persistência própria para o gráfico.

#### Scenario: Aplicação usa banco suportado
- **WHEN** a evolução é consultada com H2 ou PostgreSQL
- **THEN** o contrato e os cálculos observáveis permanecem equivalentes sem nova tabela de evolução
