## Why

A validação de lançamentos não aplica de modo consistente a sequência cronológica já especificada quando compras e vendas efetivas compartilham a mesma data. Além disso, testes com datas futuras fixas se tornam inválidos com a passagem do calendário e bloqueiam a suíte Maven.

## What Changes

- Corrigir a validação cronológica para avaliar transações efetivas na ordem de data da transação e criação, incluindo o lançamento candidato.
- Preservar o bloqueio de venda que produz saldo histórico negativo e aceitar a venda quando existir compra histórica suficiente.
- Tornar cenários de lançamentos futuros relativos ao relógio controlado dos testes.
- Manter a API, os códigos de erro, o modelo de dados e as regras funcionais já especificadas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A change corrige a implementação e os testes para cumprir o comportamento já definido em `portfolio-transactions`.

## Impact

- Serviço de lançamentos e seus testes de integração.
- Suíte Maven completa.
- Nenhuma migração, dependência ou alteração de contrato HTTP.
