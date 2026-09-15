## Context

O serviço administrativo protege o último administrador ativo por uma contagem dentro de cada transação, mas não coordena operações em transações diferentes. A mesma decisão precisa abranger rebaixamento e desativação, nos bancos H2 e PostgreSQL. Veja a motivação em [proposal.md](proposal.md) e o contrato em `specs/admin-user-management/spec.md`.

## Goals / Non-Goals

**Goals:**

- Serializar a decisão que remove uma conta da condição `ADMIN` e `ACTIVE`.
- Manter ao menos uma conta administrativa ativa após operações concorrentes.
- Preservar o contrato atual de conflitos e os fluxos administrativos não concorrentes.

**Non-Goals:**

- Alterar os endpoints, o modelo de usuário ou o comportamento de promoção e reativação.
- Criar bloqueio global de toda a administração ou introduzir fila, cache distribuído ou nova tabela de coordenação.

## Decisions

### Bloquear pessimisticamente a coleção de administradores ativos antes de decidir

Operações que podem remover um administrador ativo obterão, na mesma transação, bloqueio de escrita sobre todas as contas `ADMIN` e `ACTIVE`, em uma ordem estável. Depois do bloqueio, o serviço localizará a conta-alvo e verificará quantos administradores ativos permanecem antes de persistir a alteração.

O conjunto pequeno de administradores torna esse bloqueio apropriado e ele funciona com os dois bancos suportados, sem mudança de esquema. Ao serializar remoções, a segunda operação observa o resultado da primeira e recebe `LAST_ACTIVE_ADMIN` se deixar de haver margem para remoção.

Alternativas consideradas:

- **Contagem sem bloqueio:** descartada porque permite leitura concorrente da mesma contagem e viola o invariante.
- **Bloquear somente a conta-alvo:** descartada porque duas operações em contas administrativas distintas continuariam independentes.
- **Adicionar uma entidade de trava global:** descartada porque exige nova tabela e migração para resolver uma coordenação limitada ao conjunto de administradores ativos.
- **Usar lock consultivo do PostgreSQL:** descartada porque quebraria a compatibilidade do comportamento com H2.

### Revalidar a conta-alvo após adquirir os bloqueios

O serviço carregará a conta que será alterada somente após sincronizar o conjunto de administradores ativos. Isso evita decidir com papel ou status observados antes de outra transação terminar sua alteração.

## Risks / Trade-offs

- **Operações administrativas concorrentes podem aguardar uma à outra** → o bloqueio é restrito às remoções de administradores ativos e o conjunto costuma ser pequeno.
- **Uma solicitação iniciada por administrador recém-desativado pode chegar ao serviço** → a verificação sob bloqueio ainda preserva o invariante; a próxima solicitação será recusada pela validação de conta ativa.
- **Possível diferença de dialeto de bloqueio** → a cobertura de integração será executada no H2 e o teste PostgreSQL existente continuará validando a inicialização no banco de produção.

## Migration Plan

1. Publicar a alteração sem migração de banco.
2. Operações administrativas novas passam a ter garantia de continuidade imediatamente.
3. Em rollback, reverter apenas o código; não há dados ou esquema a recuperar.
