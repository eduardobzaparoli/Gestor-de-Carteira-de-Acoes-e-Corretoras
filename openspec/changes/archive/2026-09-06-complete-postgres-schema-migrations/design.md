## Context

Veja a motivação em `proposal.md`. O perfil `prod` já valida o esquema com Hibernate e habilita Flyway, mas o perfil `postgres` ainda usa `ddl-auto=update`. O diretório de migrações contém apenas `V1__add_user_status.sql`; assim, as tabelas que antecedem a coluna `status` não são criadas em um PostgreSQL vazio.

Existem três estados de banco que precisam continuar funcionando:

1. Banco vazio, sem histórico Flyway.
2. Banco legado não vazio, criado anteriormente pelo Hibernate e sem histórico Flyway.
3. Banco já registrado no histórico Flyway, inclusive com `V1` aplicada.

## Goals / Non-Goals

**Goals:**

- Tornar a criação inicial do esquema PostgreSQL reproduzível a partir do repositório.
- Manter a atualização de bancos legados segura e não destrutiva.
- Fazer com que os perfis PostgreSQL validem, em vez de alterarem, o esquema pelo Hibernate.
- Verificar o fluxo com testes determinísticos e com a suíte PostgreSQL opt-in existente.

**Non-Goals:**

- Alterar o modelo de domínio, os endpoints ou os contratos JSON.
- Migrar H2 para Flyway ou substituir seu ciclo de esquema gerenciado pelo Hibernate.
- Reparar automaticamente bancos legados que já estejam estruturalmente incompatíveis com a aplicação.
- Introduzir limpeza automática, exclusão de tabelas ou de dados em bancos PostgreSQL.

## Decisions

### Usar uma baseline migration cumulativa `B1`

Será adicionada uma migração `B1__initial_schema.sql` que representa o estado atual completo do PostgreSQL, incluindo `users.status`. Em banco vazio, Flyway executa a baseline e ignora a migração versionada `V1` de mesma versão; em ambiente que já possui histórico Flyway, a baseline é ignorada. Isso permite manter `V1` sem alterar seu checksum.

Em banco legado sem histórico, `baseline-on-migrate=true` registra a versão `0`; o Flyway aplica a evolução `V1` e preserva as tabelas já existentes. Esse é o motivo para não criar uma `V0.x` com `CREATE TABLE IF NOT EXISTS`: uma migração versionada adicionada retroativamente pode ter comportamento ambíguo em históricos que já alcançaram `V1`.

### Derivar o DDL das entidades persistentes atuais

A baseline conterá as cinco tabelas físicas existentes (`users`, `brokerages`, `portfolios`, `portfolio_transactions` e `portfolio_income_events`), seus tipos PostgreSQL, `NOT NULL`, chaves estrangeiras e nomes explícitos das unicidades anotadas nas entidades. Não haverá tabelas para posições, cotações, valuation ou evolução, pois esses dados são calculados dinamicamente e os testes atuais confirmam que não há persistência para eles.

### Hibernate somente como validador em PostgreSQL

`application-postgres.properties` passará de `update` para `validate`, alinhando-o ao perfil `prod`. H2 de desenvolvimento e teste continuará com Flyway desabilitado e Hibernate criando seu esquema automaticamente.

### Testar separadamente banco novo, legado e integração

Os testes Flyway em H2 no modo de compatibilidade PostgreSQL cobrirão a criação de banco vazio, as tabelas/restrições essenciais e a atualização mínima de um banco legado. A suíte PostgreSQL existente continuará opt-in, mediante configuração explícita do banco de testes, para verificar o fluxo real da aplicação com o driver PostgreSQL.

## Risks / Trade-offs

- [Uma baseline não retrata bancos legados incompletos] → a inicialização falhará pela validação, em vez de o Hibernate completar silenciosamente a estrutura; isso expõe uma inconsistência que exige correção planejada.
- [`baseline-on-migrate` pode registrar o banco errado se variáveis apontarem para uma base não pretendida] → manter a configuração limitada aos perfis PostgreSQL e documentar o uso exclusivo de URL, usuário e senha do banco alvo.
- [H2 não é uma réplica perfeita do PostgreSQL] → manter a verificação PostgreSQL opt-in para o ciclo completo, além dos testes rápidos de migração.
- [DDL cumulativo pode divergir das entidades no futuro] → cada mudança persistente no PostgreSQL deverá incluir uma nova migração versionada e testes; a validação Hibernate falhará se houver divergência.

## Migration Plan

1. Publicar a aplicação com `B1` e a `V1` existente.
2. Para banco vazio, iniciar com as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` apontando para o banco alvo; Flyway cria o histórico e a baseline.
3. Para banco legado sem histórico, executar uma cópia de segurança operacional antes do primeiro início; Flyway registra o baseline `0` e aplica `V1` uma única vez.
4. Confirmar no histórico Flyway a migração correspondente e iniciar a aplicação com Hibernate em modo de validação.

Não há rollback automático seguro para migrações de esquema. Se uma implantação falhar antes de concluir, a recuperação será feita a partir do backup do banco e da versão anterior da aplicação; após uma migração concluída, mudanças futuras devem ocorrer por nova migração versionada, nunca pela edição de arquivos já aplicados.
