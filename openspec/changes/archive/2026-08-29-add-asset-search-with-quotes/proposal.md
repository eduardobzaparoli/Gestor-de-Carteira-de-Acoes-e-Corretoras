## Why

Ao abrir uma carteira, o investidor precisa localizar de forma segura ações ou ETFs do mercado brasileiro ou americano e visualizar a cotação retornada antes de iniciar um lançamento futuro. A busca deve respeitar a propriedade da carteira, limitar chamadas às fontes externas e não antecipar a persistência de ativos ou operações.

## What Changes

- Adicionar busca protegida de ativos no contexto de uma carteira pertencente ao investidor autenticado.
- Exigir a seleção explícita de mercado (`BR` ou `US`) e tipo de ativo (`STOCK` ou `ETF`), sem categoria agregada.
- Consultar Brapi para mercado brasileiro e AlphaVantage para mercado americano por estratégias isoladas.
- Retornar ticker, nome, mercado, tipo, moeda e cotação mais recente disponível para cada resultado, sem expor detalhes dos provedores.
- Aplicar cache local de cinco minutos para resultados de identificação e de um minuto para cotações por ticker.
- Padronizar validações e falhas técnicas ou limites dos provedores em respostas JSON, sem persistir resultados de busca.

## Capabilities

### New Capabilities

- `asset-search`: Busca privada de ações e ETFs com cotações por mercado e tipo selecionados.

### Modified Capabilities

- Nenhuma.

## Impact

- Nova API protegida associada a uma carteira existente e seus DTOs públicos.
- Novas abstrações Strategy, clientes HTTP, configuração por variáveis de ambiente e cache em memória para Brapi e AlphaVantage.
- Ampliação do tratamento centralizado de erros e da cobertura de testes unitários, de integração H2 e de compatibilidade PostgreSQL sem persistir ativos pesquisados.
