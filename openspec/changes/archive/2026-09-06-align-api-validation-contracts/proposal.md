## Why

Os contratos de criação de lançamentos e proventos ainda aceitam parte relevante do retrato do ativo diretamente do cliente e não limitam os decimais à capacidade das colunas persistentes. Isso permite dados inconsistentes e faz entradas inválidas chegarem ao banco como falhas internas, além de divergir do `409` já especificado para proventos de ativos nunca adquiridos.

## What Changes

- A pesquisa de ativos passa a emitir uma referência temporária, vinculada ao investidor e à carteira, para que lançamentos usem um retrato de ativo confirmado pelo servidor. **BREAKING**: a criação de lançamento deixa de aceitar ticker, nome, mercado, tipo e moeda livres.
- Lançamentos validam a referência expirada, de outra carteira ou de outro investidor com conflito público e usam somente os dados recuperados dela.
- Entradas monetárias e quantitativas passam a respeitar a precisão `DECIMAL(19,8)` antes da persistência; combinações de mercado e moeda incompatíveis são rejeitadas como dados inválidos.
- O registro manual de proventos recupera o retrato do ativo do histórico efetivo da própria carteira e responde `409` quando não houver compra efetiva correspondente.

## Capabilities

### New Capabilities

_Nenhuma._

### Modified Capabilities

- `asset-search`: expor referência temporária e vinculada para a seleção de ativos pesquisados.
- `portfolio-transactions`: exigir referência de seleção de ativo, aplicar limites decimais e impedir retratos inconsistentes enviados pelo cliente.
- `portfolio-income-events`: alinhar o conflito para ativo sem compra efetiva e obter o retrato manual do histórico da carteira.

## Impact

- Contratos de `GET /api/portfolios/{portfolioId}/assets` e `POST /api/portfolios/{portfolioId}/transactions`.
- DTOs, cache temporário de pesquisa, validação de serviço, tratamento de erros e testes de integração.
- DTO e validação do registro manual de proventos, sem dependências ou tabelas novas.
