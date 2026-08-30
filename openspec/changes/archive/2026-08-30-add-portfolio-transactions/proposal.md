## Why

As carteiras já podem ser criadas e pesquisadas, mas ainda não registram as compras e vendas que compõem a posição do investidor. Esta mudança introduz o histórico confiável de lançamentos e evita vendas acima da quantidade disponível, preparando os cálculos e o dashboard para etapas futuras.

## What Changes

- Adiciona lançamentos de compra e venda para ações e ETFs dos mercados brasileiro e americano, no contexto de uma carteira privada.
- Registra data da operação, quantidade, preço unitário, custos opcionais e uma cópia dos dados do ativo pesquisado.
- Define o ciclo de vida `PENDENTE`, `EFETIVADO` e `CANCELADO`: datas presentes ou passadas são efetivadas; datas futuras ficam pendentes e são efetivadas na próxima consulta ou operação após a data prevista, salvo cancelamento prévio.
- Impede vendas que excedam a posição efetivada do ativo e reserva a quantidade de vendas futuras pendentes.
- Disponibiliza um histórico ordenado do lançamento mais recente para o mais antigo. Lançamentos efetivados são imutáveis; somente pendentes podem ser cancelados.
- Impede a exclusão definitiva de carteiras que já possuam lançamentos, preservando o log histórico.
- Não inclui, nesta mudança, cálculo de preço médio, indicadores de dashboard, dividendos, impostos ou atualização de cotações de posições.

## Capabilities

### New Capabilities

- `portfolio-transactions`: criação, consulta, ciclo de vida, cancelamento e regras de saldo dos lançamentos privados de uma carteira.

### Modified Capabilities

- `portfolio-management`: a exclusão de carteira passa a ser bloqueada quando existir lançamento associado, para preservar o histórico.

## Impact

- Novas rotas REST privadas de lançamentos no contexto de carteiras e novos DTOs JSON.
- Novas camadas Domain, Entity, Repository, Mapper e Service para persistir e consultar lançamentos em H2 e PostgreSQL.
- A camada de carteira passará a consultar lançamentos antes da exclusão.
- Reutilização dos tipos e resultados de ativos existentes, sem nova integração externa ou nova dependência.
