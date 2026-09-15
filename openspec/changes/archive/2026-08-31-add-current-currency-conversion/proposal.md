## Why

A carteira pode conter posições em reais e dólares, mas a valorização atual só apresenta subtotais separados por moeda. O investidor precisa visualizar o patrimônio de mercado total em uma única moeda-base, sem confundir essa conversão pontual com uma rentabilidade histórica que ainda não considera a variação cambial de cada lançamento.

## What Changes

- Adicionar à valorização privada da carteira um patrimônio de mercado consolidado em BRL.
- Converter somente valores de mercado de posições em USD para BRL com a cotação PTAX de compra do fechamento mais recente disponível.
- Preservar as posições e subtotais nativos em BRL e USD já retornados pela API.
- Expor, junto ao total consolidado, a moeda-base, a taxa aplicada e a data de referência para tornar a conversão auditável.
- Não calcular nesta etapa valor investido, lucro ou rentabilidade consolidados em BRL; esses indicadores dependem das taxas históricas das compras e serão tratados em uma mudança futura.
- Reutilizar a BrasilAPI como fonte da cotação cambial USD/BRL, com tratamento de indisponibilidade e cache temporário.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-market-valuation`: ampliar a valorização de carteira com patrimônio de mercado consolidado em BRL, sem alterar os subtotais nativos por moeda.

## Impact

- Altera a resposta de `GET /api/portfolios/{portfolioId}/valuation` de forma compatível, adicionando dados consolidados opcionais quando houver posições abertas.
- Afeta os serviços, domínios, DTOs, mapeadores e testes de valorização; adiciona uma estratégia de integração cambial e cache próprio.
- Usa a BrasilAPI já configurada no projeto, sem nova dependência nem credencial.
