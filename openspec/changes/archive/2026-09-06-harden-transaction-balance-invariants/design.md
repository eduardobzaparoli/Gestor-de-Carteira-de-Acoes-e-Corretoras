## Context

O serviço de lançamentos contém regras separadas para validação cronológica de vendas efetivas e cálculo de disponibilidade para vendas futuras. A reconciliação apenas altera o status de pendências vencidas. Como as leituras e escritas ocorrem sem uma trava comum da carteira, operações concorrentes podem observar o mesmo saldo.

## Goals / Non-Goals

**Goals:**

- Manter uma única projeção de saldo para as regras de venda.
- Garantir que reservas futuras não sejam consumidas por vendas efetivas.
- Preservar a invariância de saldo não negativo durante efetivação e concorrência.
- Ter o mesmo comportamento no H2 e no PostgreSQL.

**Non-Goals:**

- Alterar endpoints, DTOs, estados de lançamento ou o cálculo de preço médio.
- Criar uma tabela de posições materializadas ou uma migração de dados.
- Resolver manualmente lançamentos legados que já estejam inconsistentes; eles serão bloqueados antes de gerar nova posição negativa.

## Decisions

### Centralizar a projeção de saldo

Será introduzido um componente de domínio/aplicação para avaliar o histórico de um ticker e mercado. Ele calculará a posição efetiva em ordem cronológica e as reservas de vendas ainda pendentes. A criação e a reconciliação usarão esse mesmo componente.

Para uma venda efetiva, serão exigidas duas verificações: a sequência histórica não pode ficar negativa na data do lançamento e a posição atual não pode consumir reservas futuras. Para uma venda futura, somente a posição já efetiva, descontadas as reservas existentes, poderá ser usada; compras pendentes não antecipam saldo.

Alternativa considerada: manter os dois cálculos atuais e acrescentar condições pontuais. Foi rejeitada porque duplicaria regras e voltaria a permitir divergência entre criação e reconciliação.

### Reconciliar pendências em ordem e validar antes de mudar o estado

Pendências vencidas serão processadas em ordem de data da transação e criação. Cada venda será validada no estado que resultaria da efetivação; apenas então mudará para `EFFECTIVE`. Uma inconsistência interrompe a operação com o conflito já exposto pela API e a transação é revertida, preservando os estados anteriores.

Alternativa considerada: cancelar automaticamente uma venda inválida. Foi rejeitada porque mudaria o histórico do investidor sem uma ação explícita.

### Serializar por carteira com bloqueio pessimista

Operações que criam, cancelam ou efetivam lançamentos obterão um bloqueio pessimista na linha da carteira dentro da transação. Assim, a segunda venda concorrente aguarda a primeira concluir e recalcula o saldo já atualizado.

O bloqueio por carteira é deliberadamente mais amplo do que um bloqueio por ticker: evita uma nova tabela de saldos e garante a coerência da reconciliação multiativo. O custo é a serialização temporária de lançamentos da mesma carteira, que é aceitável para o volume esperado.

Alternativa considerada: bloqueio otimista ou uma linha de saldo por ativo. Ambos exigiriam versionamento ou modelo persistente adicional e migrações que não são necessários para este escopo.

## Risks / Trade-offs

- [Uma operação de cotação ou consulta pode acionar reconciliação e aguardar uma escrita da mesma carteira] → O bloqueio será aplicado somente durante a curta transação de reconciliação e persistência.
- [O H2 pode diferir do PostgreSQL em detalhes de bloqueio] → Cobrir a regra funcional nos testes padrão e o cenário concorrente no perfil de integração PostgreSQL quando configurado.
- [Dados históricos já inconsistentes impedem a efetivação automática] → Retornar conflito sem alterar dados, em vez de produzir saldo negativo ou cancelar registros silenciosamente.

## Migration Plan

Não há mudança de esquema nem dados a migrar. A implantação é compatível com o histórico existente; o rollback consiste em reverter o código da change. Se houver um lançamento legado inconsistente, ele permanecerá pendente e será identificado pelo conflito de saldo durante a reconciliação.
