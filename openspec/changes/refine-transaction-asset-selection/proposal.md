## Why

A lista de ativos do diálogo de novo lançamento exibe uma cotação antes da seleção, criando ruído visual e podendo sugerir que aquele valor armazenado será necessariamente o preço da operação. Na mesma lista, a identificação visual precisa carregar a logo do ativo de forma consistente com as demais telas e manter um marcador legível quando a fonte externa falhar.

## What Changes

- Remover a cotação armazenada ou convertida das linhas de ativos apresentadas no diálogo de novo lançamento.
- Manter em cada opção somente a identidade necessária à escolha: logo, ticker, nome e tipo do ativo.
- Garantir que a logo use o componente compartilhado e as fontes adequadas ao mercado, com tentativas alternativas e fallback local.
- Preservar a consulta de cotação corrente depois que o investidor seleciona o ativo, pois ela continua necessária para preencher e validar o preço unitário da transação.
- Cobrir o novo comportamento com testes de interface, incluindo ausência da cotação na lista e renderização da logo nos mercados brasileiro e americano.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `react-web-interface`: refinar a pesquisa de ativos no diálogo de novo lançamento para ocultar cotações nas opções e assegurar a identificação por logo com fallback.

## Impact

- Frontend React, principalmente `PortfolioPage.tsx`, `AssetLogo.tsx` e seus testes.
- Especificação consolidada da interface web.
- Nenhuma mudança em endpoints, DTOs, persistência, integrações de cotação ou dependências.
