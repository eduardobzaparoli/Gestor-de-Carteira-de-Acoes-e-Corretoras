## Why

O limite gratuito da Alpha Vantage restringe pesquisas, atualizações de carteira e gráficos com ativos americanos. A Twelve Data oferece uma cota maior e atende pesquisa, cotação e séries históricas, enquanto a Alpha Vantage deve ser preservada para dividendos porque fornece a data de pagamento exigida pelo domínio atual.

## What Changes

- Substituir a Alpha Vantage pela Twelve Data na pesquisa e cotação de ações e ETFs do mercado `US`.
- Usar a Twelve Data para fechamentos históricos diários não ajustados de ativos `US` na evolução patrimonial.
- Manter Brapi para ativos `BR`, Brasil API para PTAX e Alpha Vantage exclusivamente para candidatos de dividendos `US`.
- Externalizar URL e chave da Twelve Data, documentando separadamente as credenciais exigidas por cada fluxo.
- Normalizar respostas, classificação de instrumentos, limites de crédito e falhas da Twelve Data sem alterar os contratos REST públicos do sistema.
- Atualizar os requisitos globais e a documentação técnica que ainda identificam a Alpha Vantage como fonte geral de ações americanas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `asset-search`: alterar para Twelve Data a fonte de identificação e cotação de ativos `US` e definir o tratamento público de falhas desse provedor.
- `portfolio-market-valuation`: refletir que cotações atuais de posições `US` passam a vir da Twelve Data.
- `portfolio-value-evolution`: alterar para Twelve Data a fonte das séries históricas `US`, preservando fechamentos não ajustados e atomicidade.
- `environment-configuration`: distinguir e documentar as credenciais da Twelve Data para dados de mercado e da Alpha Vantage para dividendos.

## Impact

- Estratégias de pesquisa, cotação e preços históricos do mercado americano, seus resolvedores, clientes HTTP, propriedades e testes.
- Configuração por ambiente, `.env.example`, documentação de execução e mensagens públicas de indisponibilidade ou limite.
- Especificação global em `docs/product-spec.md`, mantendo inalterado o contrato de proventos que já determina Alpha Vantage para dividendos `US`.
- Nenhuma nova tabela, migração de banco ou alteração nos endpoints e DTOs públicos é prevista.
- Não é necessária nova dependência: a integração continuará usando o cliente HTTP e o padrão Strategy já existentes.
