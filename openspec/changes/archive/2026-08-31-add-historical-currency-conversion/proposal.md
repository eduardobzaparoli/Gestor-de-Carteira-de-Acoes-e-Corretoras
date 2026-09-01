## Why

A valorização consolidada já converte o patrimônio atual em USD para BRL, mas ainda não calcula em reais quanto foi efetivamente investido nas compras históricas. Sem converter cada custo pela taxa aplicável à sua data, o ganho total e a rentabilidade consolidados misturam moedas e podem induzir o investidor a uma conclusão incorreta.

## What Changes

- Ampliar a valorização privada da carteira com valor investido, ganho ou perda total e rentabilidade consolidados em BRL.
- Converter o custo de cada compra efetivada em USD, incluindo custos adicionais, pela PTAX de compra do fechamento aplicável à data do lançamento.
- Reduzir o valor investido de uma venda efetivada pelo custo médio histórico em BRL das unidades vendidas, e não pelo valor recebido na venda.
- Reutilizar a cotação PTAX corrente exclusivamente para converter o patrimônio de mercado atual, preservando a separação entre custo histórico e valor presente.
- Manter lançamentos pendentes fora dos cálculos e deixar dividendos fora do escopo, pois ainda não possuem lançamento próprio.
- Expor as taxas históricas efetivamente utilizadas de forma auditável, sem persistir cotações cambiais.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-market-valuation`: ampliar a valorização consolidada em BRL com custo histórico convertido, ganho ou perda total e rentabilidade, preservando os subtotais nativos por moeda.

## Impact

- Altera de forma aditiva a resposta de `GET /api/portfolios/{portfolioId}/valuation`.
- Afeta os domínios, serviços, DTOs, mapeadores, estratégia e cache da integração cambial, além dos testes de posições, lançamentos e valorização.
- Usa a BrasilAPI já integrada; não introduz dependências, credenciais, migrações ou persistência de taxas.
