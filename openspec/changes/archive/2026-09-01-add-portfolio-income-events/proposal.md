## Why

O dashboard precisa apresentar proventos recebidos, mas hoje o sistema não possui um evento próprio para representar dividendos, JCP e distribuições nem consegue relacionar os eventos publicados pelas APIs à posição histórica do investidor. A mudança permite localizar proventos, calcular a elegibilidade com base nos lançamentos da carteira e confirmar o valor efetivamente creditado sem confundir renda com compra, venda ou valorização de mercado.

## What Changes

- Consultar, sob solicitação do investidor, eventos monetários de ativos brasileiros na Brapi e de ativos americanos na Alpha Vantage.
- Normalizar dividendos, JCP e distribuições em candidatos comuns, mantendo fora do escopo eventos que alterem quantidade, como bonificações, desdobramentos, grupamentos e subscrições.
- Reconstituir a quantidade elegível na data-com brasileira ou imediatamente antes da data ex-dividendo americana e calcular o valor bruto previsto por evento.
- Exigir confirmação do investidor antes da persistência, permitindo ajustar o valor efetivamente recebido e registrando justificativa quando ele divergir do previsto.
- Oferecer registro manual como contingência para evento ausente, provedor indisponível ou plano sem acesso ao recurso.
- Manter histórico privado, imutável e sem duplicidades, com eventos futuros pendentes, cancelamento apenas enquanto pendentes e efetivação na data de pagamento.
- Exibir, em um resumo próprio para o dashboard, proventos efetivados por moeda e consolidados em BRL, convertendo valores em USD pela PTAX histórica da data de pagamento, sem alterar posição, preço médio, custo em custódia, valor investido ou ganho/perda de mercado.

## Capabilities

### New Capabilities

- `portfolio-income-events`: Consulta de candidatos nas APIs, cálculo de elegibilidade, confirmação ou registro manual, ciclo de vida, prevenção de duplicidades, histórico privado e resumo de proventos da carteira.

### Modified Capabilities

- Nenhuma. O resumo de proventos será independente da valorização de mercado e não alterará seus indicadores existentes.

## Impact

- Novos contratos REST sob o contexto privado da carteira para consultar candidatos, confirmar ou cadastrar, listar/cancelar e resumir proventos.
- Novas camadas Domain, Mapper, Controller, Service, Repository, Entity e DTO para eventos de proventos.
- Novas estratégias de integração de proventos para Brapi e Alpha Vantage, reutilizando configuração, resiliência e tratamento público de falhas existentes.
- Reutilização dos lançamentos efetivados para posição histórica e do serviço cambial para conversão PTAX histórica.
- Persistência compatível com H2 e PostgreSQL e ampliação dos testes unitários e integrados, sem nova dependência externa.
