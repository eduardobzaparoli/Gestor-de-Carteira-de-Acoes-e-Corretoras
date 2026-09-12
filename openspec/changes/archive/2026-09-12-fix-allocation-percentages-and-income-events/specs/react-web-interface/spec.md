## ADDED Requirements

### Requirement: Composição consolidada com fechamento visual
O sistema SHALL usar na tabela de posições e no gráfico de composição os percentuais consolidados em `BRL` devolvidos pela API. A interface MUST apresentar uma única distribuição para todos os mercados e moedas, manter correspondência entre fatias, legendas e linhas e exibir percentuais cuja soma seja `100%` quando houver patrimônio positivo.

#### Scenario: Carteira com ativos brasileiros e americanos
- **WHEN** o dashboard recebe posições em `BRL` e `USD`
- **THEN** tabela, gráfico e legenda exibem as mesmas participações consolidadas e a soma apresentada é `100%`

#### Scenario: Diferença residual de arredondamento
- **WHEN** os percentuais recebidos possuem casas além da precisão visual
- **THEN** a interface usa os valores públicos já reconciliados e não recalcula cada moeda como uma distribuição independente

#### Scenario: Carteira sem patrimônio positivo
- **WHEN** não há posições com valor de mercado positivo
- **THEN** a interface não fabrica percentuais nem uma composição enganosa e apresenta o estado vazio correspondente

### Requirement: Estados resilientes da descoberta de proventos
O sistema SHALL consumir o objeto enriquecido da consulta de candidatos e distinguir resultado atualizado, resultado reutilizado, falha parcial e indisponibilidade total. Candidatos confiáveis MUST permanecer operáveis quando existirem avisos parciais, e o cadastro manual MUST continuar acessível independentemente do estado dos provedores externos.

#### Scenario: Consulta totalmente atualizada
- **WHEN** todos os tickers necessários são consultados ou reutilizados dentro da janela normal sem avisos
- **THEN** a interface apresenta os candidatos ou o estado vazio sem mensagem de erro

#### Scenario: Dados anteriores reutilizados
- **WHEN** a API informa reutilização de último resultado por falha na atualização
- **THEN** a interface mantém os candidatos disponíveis e informa em português que os dados não puderam ser atualizados, exibindo a referência temporal disponível

#### Scenario: Falha parcial
- **WHEN** a API retorna candidatos e avisos para um ou mais tickers
- **THEN** a interface exibe os candidatos confiáveis, identifica os ativos não atualizados e oferece nova tentativa sem ocultar os resultados válidos

#### Scenario: Falha total
- **WHEN** a API responde com indisponibilidade total e nenhum resultado confiável
- **THEN** a interface apresenta o código público traduzido, oferece nova tentativa e mantém disponível a ação de registro manual

#### Scenario: Limite da Alpha Vantage
- **WHEN** a descoberta americana não pode ser atualizada porque o limite da Alpha Vantage foi atingido
- **THEN** a interface evita tentativas automáticas repetidas, usa dados reutilizáveis quando fornecidos e orienta o investidor a tentar novamente mais tarde
