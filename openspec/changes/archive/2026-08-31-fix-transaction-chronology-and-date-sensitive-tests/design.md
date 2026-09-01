## Context

A regra existente já exige que vendas efetivas sejam validadas em sequência cronológica. A implementação aplica, antes dessa sequência, uma validação de disponibilidade agregada que não é necessária para vendas efetivas retroativas; a sequência histórica é a fonte de verdade para esses casos. Os testes de lançamento futuro também usam datas absolutas, em vez do relógio injetado.

## Goals / Non-Goals

**Goals:**

- Validar vendas `EFFECTIVE` exclusivamente contra o saldo acumulado na sequência cronológica de transações efetivas, incluindo o lançamento candidato.
- Manter a reserva agregada somente para vendas `PENDING` futuras.
- Substituir datas absolutas em cenários temporais por datas calculadas a partir do `Clock` de teste.

**Non-Goals:**

- Alterar o contrato da API, os estados de lançamentos ou regras de preço médio.
- Criar migrações, dependências ou campos persistidos.
- Definir uma nova política de desempate para lançamentos criados no mesmo instante.

## Decisions

- Separar a validação por estado: vendas efetivas passam pela reconstrução cronológica; vendas pendentes usam a disponibilidade agregada para reservar saldo. A reconstrução já detecta todo saldo histórico negativo e também cobre vendas na data atual.
- Preservar a ordenação atual por data de transação e data de criação. A correção não introduz UUID como desempate, pois isso alteraria semântica de domínio sem requisito aprovado.
- Usar `LocalDate.now(clock)` e deslocamentos relativos nos testes de ciclo de vida. O relógio de teste será movido para efetivar o lançamento pendente sem depender da data do calendário.

## Risks / Trade-offs

- [Mudança na ramificação da validação de vendas efetivas] → Cobrir compra e venda retroativas, venda acima do saldo e reservas futuras em testes de integração.
- [Datas relativas tornam os testes menos autoexplicativos] → Nomear as variáveis `today`, `pastDate` e `futureDate` para deixar a intenção clara.
