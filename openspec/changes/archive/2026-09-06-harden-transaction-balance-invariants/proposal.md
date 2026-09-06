## Why

A reserva de vendas futuras já é considerada ao criar outra venda futura, mas vendas efetivas e retroativas ainda podem consumir uma quantidade que está reservada. Além disso, requisições simultâneas podem validar o mesmo saldo antes da persistência, permitindo venda a descoberto apesar da validação individual.

## What Changes

- Centralizar o cálculo de saldo negociável por ticker e mercado, combinando posição efetiva, reservas de vendas pendentes e validação cronológica.
- Aplicar a reserva de vendas pendentes também à criação de vendas efetivas, inclusive retroativas.
- Revalidar quantitativos antes de efetivar lançamentos pendentes vencidos, preservando o lançamento pendente caso a efetivação causasse saldo negativo.
- Serializar operações que alteram ou efetivam lançamentos da mesma carteira para que vendas concorrentes não aprovem o mesmo saldo.
- Cobrir o comportamento com testes de vendas presentes, retroativas, futuras, efetivação de pendências e concorrência.

## Capabilities

### New Capabilities

<!-- Nenhuma. -->

### Modified Capabilities

- `portfolio-transactions`: ampliar a garantia de bloqueio de venda a descoberto para reservas, efetivação de pendências e operações concorrentes.

## Impact

- Serviços de lançamentos e reconciliação de lançamentos.
- Repositório e acesso bloqueante à carteira durante operações de saldo.
- Testes de integração de lançamentos, incluindo o perfil PostgreSQL quando aplicável.
- Sem novos endpoints, dependências de aplicação ou migrações de banco previstas.
