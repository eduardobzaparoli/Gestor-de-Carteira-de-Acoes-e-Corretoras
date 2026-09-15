## Why

Carteiras com posições em BRL e USD exibem percentuais calculados dentro de cada moeda, mas o frontend reúne todas as posições em um único gráfico; por isso a composição pode somar aproximadamente 200% em vez de 100%. A consulta de candidatos a proventos também repete chamadas externas por ticker e transforma a falha de um único provedor ou ativo em erro da seção inteira, tornando o fluxo especialmente vulnerável aos limites da Alpha Vantage e a indisponibilidades da Brapi.

## What Changes

- Alterar o percentual público de alocação de cada posição para representar sua participação no patrimônio consolidado da carteira em BRL, usando as mesmas taxas cambiais e a mesma precisão do resumo consolidado.
- Garantir que os percentuais não arredondados tenham soma matemática igual a 100% quando houver patrimônio positivo e que a apresentação arredondada no gráfico e na tabela também feche em 100%.
- Manter os subtotais financeiros nativos por moeda, separando-os do percentual global usado na composição consolidada.
- Consultar candidatos a proventos apenas para tickers relevantes do mercado solicitado e evitar chamadas duplicadas simultâneas ou repetidas durante uma janela configurável, inclusive para respostas vazias.
- Preservar temporariamente a última resposta válida por mercado e ticker e reutilizá-la quando uma nova tentativa falhar por indisponibilidade ou limitação do provedor, identificando que os dados exibidos não são uma atualização nova.
- Isolar falhas por ticker: candidatos obtidos ou recuperados com segurança continuam disponíveis, enquanto a interface informa quais ativos não puderam ser atualizados; a consulta só falha integralmente quando nenhum resultado confiável pode ser produzido.
- Manter Brapi para proventos brasileiros e Alpha Vantage para dividendos americanos, sem alterar o cadastro manual de contingência.
- Exibir no frontend uma composição coerente de 100%, estado de atualização dos proventos, avisos parciais acionáveis e mensagens públicas específicas em português.
- Atualizar testes e documentação dos contratos afetados, incluindo carteiras multimoeda, arredondamento, cache, concorrência, limite de provedor, resposta parcial e falha total.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-market-valuation`: substituir a alocação segregada por moeda pela participação consolidada em BRL e definir o fechamento determinístico dos percentuais em 100%.
- `portfolio-income-events`: tornar a descoberta de candidatos resiliente a repetição, concorrência, respostas vazias, falhas parciais e limites dos provedores, preservando a última resposta válida temporária.
- `react-web-interface`: apresentar percentuais consolidados consistentes e distinguir sucesso, dados reutilizados, falha parcial e indisponibilidade total na área de proventos.

## Impact

- Cálculo e DTOs de valorização, serviço de câmbio e testes de carteiras com uma ou mais moedas.
- Contrato JSON da consulta de candidatos a proventos, que precisará incluir candidatos, informações de atualização e avisos por ticker; o frontend será atualizado na mesma change.
- Estratégias Brapi e Alpha Vantage, orquestração do serviço de proventos e um cache temporário compartilhado com deduplicação de requisições concorrentes.
- Dashboard React, gráfico de composição, tabela de posições, painel de proventos, mensagens públicas e testes de interface.
- Documentação da API e requisitos consolidados; não são previstas novas dependências nem migração de banco de dados.

**BREAKING**: a resposta de `GET /api/portfolios/{portfolioId}/income-events/candidates` deixará de ser uma coleção JSON simples e passará a ser um objeto que contém a coleção de candidatos e metadados de atualização/avisos. A aplicação web será migrada junto com o backend.
