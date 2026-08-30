## Why

As posições atuais já mostram quantidade, custo em custódia e preço médio, mas o investidor ainda não consegue comparar esses valores com a cotação mais recente nem enxergar indicadores de valorização da carteira. Esta mudança inicia o dashboard de BI previsto no produto, sem misturar moedas enquanto a conversão cambial não estiver disponível.

## What Changes

- Adicionar uma consulta privada de valorização de carteira que reúna as posições abertas e suas cotações mais recentes disponíveis.
- Exibir, por posição, preço atual, valor de mercado, ganho ou perda não realizado, rentabilidade percentual e participação na carteira dentro da mesma moeda.
- Exibir subtotais separados por moeda, contendo valor investido, valor de mercado, ganho ou perda não realizado e rentabilidade.
- Reutilizar as integrações e o cache temporário de cotações da Brapi e Alpha Vantage; não persistir posições, cotações ou indicadores.
- Falhar a consulta inteira caso qualquer posição aberta não possa ser cotada, mantendo respostas consistentes e sem totais parciais.
- Manter BRL e USD separados. Conversão cambial, total consolidado, dividendos e ganho realizado permanecem fora do escopo desta change.

## Capabilities

### New Capabilities

- `portfolio-market-valuation`: consulta privada de indicadores de mercado e valorização das posições abertas de uma carteira, segmentados por moeda.

### Modified Capabilities

- Nenhuma.

## Impact

- Nova API de valorização no contexto de carteira privada e novos DTOs, domínio, serviço, mapper e controlador correspondentes.
- Reaproveitamento de `PortfolioPositionService`, da reconciliação de lançamentos e de `AssetSearchService` com suas estratégias Brapi/Alpha Vantage e cache de cotações.
- Novos testes unitários e de integração para cálculos, autorização, cache e falhas de cotação em H2 e PostgreSQL.
- Nenhuma dependência nova, alteração de esquema ou conversão de moeda.
